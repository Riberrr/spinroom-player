package io.github.riberrr.spinroom.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val COVER_PX = 600
internal const val TILE_PX = 200

@HiltViewModel
class DetailsViewModel @Inject constructor(private val backend: DetailsBackend) : ViewModel() {

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var loadedUri: String? = null
    private var item: MediaItem? = null
    private var tracks: List<MediaItem> = emptyList()

    /** Loads [uri] once; calling again with the same uri is a no-op (recomposition, rotation). */
    fun load(uri: String) {
        if (uri == loadedUri && _state.value.status != DetailsStatus.Error) return
        loadedUri = uri
        _state.value = DetailsUiState()
        viewModelScope.launch {
            try {
                val address = backend.serverAddress.first()
                val loaded = backend.item(uri)
                item = loaded
                val kind = when (loaded.mediaType) {
                    "artist" -> DetailsKind.Artist
                    "playlist" -> DetailsKind.Playlist
                    else -> DetailsKind.Album
                }
                val trackItems = backend.tracks(loaded)
                tracks = trackItems
                val albums = if (kind == DetailsKind.Artist) backend.artistAlbums(loaded) else emptyList()
                _state.value = DetailsUiState(
                    status = DetailsStatus.Content,
                    kind = kind,
                    title = loaded.name,
                    byline = if (kind == DetailsKind.Artist) "" else loaded.artistLine,
                    year = loaded.year,
                    trackCount = trackItems.size,
                    totalMinutes = ((trackItems.sumOf { it.duration ?: 0 }) + 30) / 60,
                    coverUrl = cover(address, loaded, COVER_PX),
                    favorite = loaded.favorite,
                    tracks = trackItems.mapIndexed { i, t -> t.toRow(i + 1, kind) },
                    albums = albums.map {
                        AlbumRow(it.uri ?: "library://album/${it.itemId}", it.name, it.year?.toString().orEmpty(), cover(address, it, TILE_PX))
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DetailsUiState(status = DetailsStatus.Error)
            }
        }
    }

    fun retry() {
        val uri = loadedUri ?: return
        loadedUri = null
        load(uri)
    }

    /** Play everything, or start at [track]; with [shuffle] the server shuffles the queue. */
    fun onPlay(shuffle: Boolean = false, track: TrackRow? = null) = enqueue("play", shuffle, track)

    fun onAddToQueue() = enqueue("add", null, null)

    fun onToggleFavorite() {
        val current = item ?: return
        val target = !_state.value.favorite
        _state.update { it.copy(favorite = target) }
        viewModelScope.launch {
            try {
                backend.setFavorite(current, target)
                item = current.copy(favorite = target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(favorite = !target, message = DetailsMessage.Failed) }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    private fun enqueue(option: String, shuffle: Boolean?, track: TrackRow?) {
        val current = item ?: return
        viewModelScope.launch {
            val player = backend.selectedPlayerId.first()
            if (player.isBlank()) {
                _state.update { it.copy(message = DetailsMessage.NoPlayer) }
                return@launch
            }
            // Albums/playlists go as one uri so MA keeps the context; artists and "start at track" use the track list.
            val uris = if (current.mediaType == "artist" || track != null) {
                tracks.mapNotNull { it.uri }
            } else {
                listOfNotNull(current.uri)
            }
            if (uris.isEmpty()) return@launch
            try {
                backend.play(player, uris, option, shuffle, startItem = track?.uri)
                _state.update { it.copy(message = if (option == "add") DetailsMessage.Queued else DetailsMessage.Playing) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = DetailsMessage.Failed) }
            }
        }
    }
}

private fun cover(address: String, item: MediaItem, px: Int): String? =
    if (address.isBlank()) null else ImageUrls.url(address, item.bestImage, px)

internal fun MediaItem.toRow(number: Int, kind: DetailsKind) = TrackRow(
    uri = uri ?: "library://track/$itemId",
    number = number,
    title = name,
    subtitle = if (kind == DetailsKind.Album) "" else artistLine,
    durationMs = (duration ?: 0) * 1000L,
)
