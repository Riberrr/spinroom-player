package io.github.riberrr.spinroom.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TILE_PX = 320
private const val CARD_PX = 400
private const val ROW_MAX = 20
private const val YOUTUBE_PROVIDER = "ytmusic"

/**
 * Rows of the start screen. Loads once the server is connected and on [onRetry]; the "continue listening" card follows
 * the chosen player's queue live.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(private val backend: HomeBackend) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var serverAddress = ""
    private var playerId = ""
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch { backend.serverAddress.collect { serverAddress = it } }
        viewModelScope.launch {
            backend.playerId.collect { id ->
                playerId = id
                _state.update { it.copy(hasPlayer = id.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            backend.playerId.distinctUntilChanged()
                .flatMapLatest { id -> if (id.isBlank()) emptyFlow<PlayerQueue>() else backend.queue(id).catch { } }
                .catch { }
                .collect { q -> _state.update { it.copy(continueCard = q.toCard()) } }
        }
        viewModelScope.launch {
            combine(backend.connection, reload) { c, _ -> c }
                .collectLatest { c ->
                    _state.update { it.copy(connection = c) }
                    when (c) {
                        is MaConnectionState.Connected -> load()
                        is MaConnectionState.Failed -> _state.update { it.copy(loading = false, failed = true) }
                        else -> Unit
                    }
                }
        }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true, failed = false) }
        coroutineScope {
            val recent = async { runCatching { backend.recentlyPlayed() } }
            val playlists = async { runCatching { backend.playlists() } }
            val radios = async { runCatching { backend.favoriteRadios() } }
            val favs = async { runCatching { backend.favorites() } }
            val recs = async { runCatching { backend.recommendations() } }
            val r = recent.await()
            val p = playlists.await()
            val s = radios.await()
            val y = recs.await()
            val f = favs.await()
            val youtube = y.getOrNull().orEmpty()
                .filter { it.provider.contains(YOUTUBE_PROVIDER) }
                .flatMap { it.items }
                .distinctBy { it.uri ?: it.itemId }
            _state.update {
                it.copy(
                    loading = false,
                    failed = listOf(r, p, s, y).all { res -> res.isFailure },
                    recent = r.getOrNull().orEmpty().mapNotNull { m -> m.toTile() }.take(ROW_MAX),
                    favorites = f.getOrNull().orEmpty().mapNotNull { m -> m.toTile() }.take(ROW_MAX),
                    playlists = p.getOrNull().orEmpty().mapNotNull { m -> m.toTile() }.take(ROW_MAX),
                    stations = s.getOrNull().orEmpty().map { m -> m.toStation() },
                    recommended = youtube.mapNotNull { m -> m.toTile() }.take(ROW_MAX),
                )
            }
        }
    }

    fun onRetry() { reload.update { it + 1 } }

    /** Radio stations play right away on the chosen player. */
    fun onPlayStation(station: StationTileState) {
        val uri = station.uri ?: return
        val id = playerId.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { runCatching { backend.play(id, uri) } }
    }

    // --- mapping ---------------------------------------------------------------------------------------------

    private fun cover(item: MediaItem?, fallback: MediaItemImage?, px: Int) =
        ImageUrls.url(serverAddress, item?.bestImage ?: fallback, px)

    private fun MediaItem.toTile(): HomeTile? {
        if (name.isBlank()) return null
        val playlist = mediaType == "playlist"
        val sub = if (artists.isNotEmpty()) artistLine else if (playlist) "" else provider
        return HomeTile("$mediaType:$provider:$itemId", name, sub, cover(this, null, TILE_PX), uri, playlist)
    }

    private fun MediaItem.toStation() = StationTileState(
        key = "$provider:$itemId",
        name = name,
        provider = provider,
        initials = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" },
        uri = uri,
    )

    private fun PlayerQueue.toCard(): ContinueCard? {
        val item = currentItem ?: return null
        val media = item.mediaItem
        val title = media?.name?.ifBlank { null } ?: item.name
        if (title.isBlank()) return null
        val durationS = media?.duration ?: item.duration ?: 0
        return ContinueCard(
            title = title,
            artist = media?.artistLine.orEmpty(),
            coverUrl = cover(media, item.image, CARD_PX),
            progress = if (durationS > 0) (elapsedTime / durationS).toFloat().coerceIn(0f, 1f) else 0f,
            position = (currentIndex ?: 0) + 1,
            total = items,
        )
    }
}
