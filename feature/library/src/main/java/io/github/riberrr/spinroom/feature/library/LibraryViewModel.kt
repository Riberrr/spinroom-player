package io.github.riberrr.spinroom.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.coverImage
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val THUMB_PX = 200
private const val PREVIEW_TRACKS = 8

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(private val backend: LibraryBackend) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.Albums)
    private val counts = MutableStateFlow<Map<LibraryTab, Int>>(emptyMap())
    private val scrollEvents = Channel<Int>(Channel.CONFLATED)
    private val filter = MutableStateFlow(LibraryFilter())
    private val genres = MutableStateFlow<List<FilterOption>>(emptyList())
    private val sources = MutableStateFlow<List<SourceOption>>(emptyList())
    private val selected = MutableStateFlow<LibraryRow?>(null)
    private val favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /** True when the server is connected and logged in, i.e. the library can be read. */
    private val ready: Flow<Boolean> = backend.connection.map { it.isReady() }.distinctUntilChanged()

    private val focus = MutableStateFlow<LibraryFocus>(LibraryFocus.Tab)

    private val listMode = backend.listMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val preview: Flow<PreviewState> = selected
        .debounce(PREVIEW_SETTLE_MS)
        .distinctUntilChanged()
        .flatMapLatest { row ->
            if (row == null) flow { emit(PreviewState()) } else flow {
                emit(PreviewState(row, loading = true))
                val tracks = runCatching { backend.tracks(row) }.getOrDefault(emptyList())
                emit(previewOf(row, tracks))
            }
        }

    private val base = combine(tab, counts, backend.connection, focus) { t, c, conn, f -> LibraryUiState(t, c, conn, f) }
    private val extras = combine(filter, listMode, genres, sources) { f, l, g, s -> Extras(f, l, g, s) }

    val state: StateFlow<LibraryUiState> = combine(
        base, extras, preview.stateIn(viewModelScope, SharingStarted.Eagerly, PreviewState()), favoriteOverrides,
    ) { b, e, p, fav ->
        b.copy(
            filter = e.filter, listMode = e.listMode, genres = e.genres, sources = e.sources,
            preview = p.copy(row = p.row?.let { it.copy(favorite = fav[it.uri] ?: it.favorite) }),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    /** Grid/list items of the selected tab and filter, mapped to [LibraryRow]. Empty while the server is not ready. */
    val items: Flow<PagingData<LibraryRow>> = combine(tab, ready, backend.serverAddress, filter) { t, r, a, f -> Query(t, r, a, f) }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (!q.ready) emptyFlow() else backend.pager(q.tab, q.filter).map { page -> page.map { it.toRow(q.address) } }
        }
        .cachedIn(viewModelScope)

    /** Absolute index the screen should scroll to (one-shot). */
    val scrollTo: Flow<Int> = scrollEvents.receiveAsFlow()

    init {
        // Counts fail while offline, so (re)load all of them every time the server becomes ready.
        viewModelScope.launch {
            ready.filter { it }.collect {
                LibraryTab.entries.forEach { refreshCount(it) }
                loadFilterOptions()
            }
        }
    }

    /** Remembers the last focused element (chip or grid index) so Back from another screen can restore it. */
    fun onFocused(target: LibraryFocus) { focus.value = target }

    fun onSelectTab(newTab: LibraryTab) {
        focus.value = LibraryFocus.Tab
        if (newTab == tab.value) return
        tab.value = newTab
        selected.value = null
        refreshCount(newTab)
    }

    /** The row the list/grid focus is on; drives the preview and the backdrop. */
    fun onPreview(row: LibraryRow?) { selected.value = row }

    fun onSetListMode(list: Boolean) { viewModelScope.launch { backend.setListMode(list) } }

    fun onPlay(row: LibraryRow, shuffle: Boolean) { viewModelScope.launch { backend.play(row.uri, shuffle) } }

    fun onToggleFavorite(row: LibraryRow) {
        val wanted = !(favoriteOverrides.value[row.uri] ?: row.favorite)
        favoriteOverrides.update { it + (row.uri to wanted) }
        viewModelScope.launch {
            if (runCatching { backend.setFavorite(row.uri, wanted) }.isFailure) favoriteOverrides.update { it + (row.uri to !wanted) }
        }
    }

    fun onToggleGenre(option: FilterOption) = filter.update {
        if (it.genreId == option.id) it.copy(genreId = null, genreName = null) else it.copy(genreId = option.id, genreName = option.name)
    }

    fun onToggleDecade(decade: Int) = filter.update { it.copy(decade = if (it.decade == decade) null else decade) }

    fun onToggleSource(domain: String) = filter.update { it.copy(provider = if (it.provider == domain) null else domain) }

    fun onToggleFavoritesOnly() = filter.update { it.copy(favoriteOnly = !it.favoriteOnly) }

    fun onClearFilters() { filter.value = LibraryFilter() }

    /** Scrolls to the first item whose name starts at or after [letter] (`#` = start of the list). Only without a filter (the total is unknown otherwise). */
    fun onJumpToLetter(letter: String) {
        if (!filter.value.isEmpty) return
        val t = tab.value
        viewModelScope.launch {
            val total = counts.value[t] ?: runCatching { backend.count(t) }.getOrNull() ?: return@launch
            val index = if (letter == "#") 0 else runCatching {
                firstIndexOf(letter.first(), total) { backend.itemAt(t, it) }
            }.getOrNull() ?: return@launch
            scrollEvents.trySend(index)
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            runCatching { backend.genres() }.onSuccess { list ->
                genres.value = list.map { FilterOption(it.itemId, it.name) }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
            }
            runCatching { backend.sources() }.onSuccess { sources.value = it }
        }
    }

    private fun refreshCount(t: LibraryTab) {
        viewModelScope.launch {
            runCatching { backend.count(t) }.onSuccess { n -> counts.update { it + (t to n) } }
        }
    }

    private data class Extras(val filter: LibraryFilter, val listMode: Boolean, val genres: List<FilterOption>, val sources: List<SourceOption>)
    private data class Query(val tab: LibraryTab, val ready: Boolean, val address: String, val filter: LibraryFilter)

    private companion object {
        const val PREVIEW_SETTLE_MS = 250L
    }
}

internal fun previewOf(row: LibraryRow, tracks: List<MediaItem>): PreviewState = PreviewState(
    row = row,
    tracks = tracks.take(PREVIEW_TRACKS).mapIndexed { i, t -> PreviewTrack(i + 1, t.name, t.duration) },
    trackCount = tracks.size,
    totalMinutes = tracks.sumOf { it.duration ?: 0 }.takeIf { it > 0 }?.let { (it + 30) / 60 },
)

/** Binary search over a name-sorted list of [total] items for the first one whose first letter is >= [letter]. */
internal suspend fun firstIndexOf(letter: Char, total: Int, itemAt: suspend (Int) -> MediaItem?): Int {
    var lo = 0
    var hi = total
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        val first = itemAt(mid)?.name?.trimStart()?.firstOrNull()?.uppercaseChar()
        if (first == null || first >= letter) hi = mid else lo = mid + 1
    }
    return lo.coerceAtMost((total - 1).coerceAtLeast(0))
}

internal fun MaConnectionState.isReady(): Boolean = this is MaConnectionState.Connected && authenticated

internal fun MediaItem.toRow(serverAddress: String): LibraryRow {
    val type = mediaType.ifBlank { "item" }
    return LibraryRow(
        uri = uri ?: "library://$type/$itemId",
        title = name,
        subtitle = if (mediaType == "album" || mediaType == "track") artistLine else "",
        coverUrl = if (serverAddress.isBlank()) null else ImageUrls.url(serverAddress, coverImage, THUMB_PX),
        year = year,
        favorite = favorite,
        mediaType = mediaType,
    )
}
