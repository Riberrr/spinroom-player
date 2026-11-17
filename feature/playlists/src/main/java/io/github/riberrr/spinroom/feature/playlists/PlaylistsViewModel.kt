package io.github.riberrr.spinroom.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.PlaylistSort
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val COVER_PX = 300

/** One tile. [source] is the display name of the provider (empty when unknown). */
data class PlaylistRow(val uri: String, val title: String, val source: String, val coverUrl: String?)

data class PlaylistsUiState(
    val sort: PlaylistSort = PlaylistSort.Recent,
    /** Provider domain of the source filter, null = all sources. */
    val provider: String? = null,
    val sources: List<SourceOption> = emptyList(),
    val connection: MaConnectionState = MaConnectionState.Disconnected,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistsViewModel @Inject constructor(private val backend: PlaylistsBackend) : ViewModel() {

    private val sort = MutableStateFlow(PlaylistSort.Recent)
    private val provider = MutableStateFlow<String?>(null)
    private val sources = MutableStateFlow<List<SourceOption>>(emptyList())
    private val ready: Flow<Boolean> =
        backend.connection.map { it is MaConnectionState.Connected && it.authenticated }.distinctUntilChanged()

    val state: StateFlow<PlaylistsUiState> = combine(sort, provider, sources, backend.connection) { s, p, src, c ->
        PlaylistsUiState(s, p, src, c)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlaylistsUiState())

    val items: Flow<PagingData<PlaylistRow>> =
        combine(sort, provider, ready, backend.serverAddress, sources) { s, p, r, a, src -> Query(s, p, r, a, src) }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (!q.ready) emptyFlow() else backend.pager(q.sort, q.provider).map { page -> page.map { it.toRow(q.address, q.sources) } }
            }
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            ready.filter { it }.collect { runCatching { backend.sources() }.onSuccess { sources.value = it } }
        }
    }

    /** Alternates between "Ostatnio grane" and A-Z. */
    fun onToggleSort() { sort.value = nextSort(sort.value) }

    /** Cycles "Wszystkie zrodla" -> each source -> all again. */
    fun onCycleSource() { provider.value = nextProvider(provider.value, sources.value) }

    fun onPlay(row: PlaylistRow, shuffle: Boolean) { viewModelScope.launch { backend.play(row.uri, shuffle) } }

    private data class Query(
        val sort: PlaylistSort,
        val provider: String?,
        val ready: Boolean,
        val address: String,
        val sources: List<SourceOption>,
    )
}

internal fun nextSort(current: PlaylistSort): PlaylistSort =
    if (current == PlaylistSort.Recent) PlaylistSort.Alphabetical else PlaylistSort.Recent

internal fun nextProvider(current: String?, sources: List<SourceOption>): String? {
    if (sources.isEmpty()) return null
    val i = sources.indexOfFirst { it.domain == current }
    return when {
        current == null -> sources.first().domain
        i < 0 || i == sources.lastIndex -> null
        else -> sources[i + 1].domain
    }
}

internal fun MediaItem.toRow(serverAddress: String, sources: List<SourceOption>): PlaylistRow {
    val domain = providerMappings.firstOrNull()?.providerDomain.orEmpty()
    return PlaylistRow(
        uri = uri ?: "library://playlist/$itemId",
        title = name,
        source = sources.firstOrNull { it.domain == domain }?.name.orEmpty(),
        coverUrl = if (serverAddress.isBlank()) null else ImageUrls.url(serverAddress, bestImage, COVER_PX),
    )
}
