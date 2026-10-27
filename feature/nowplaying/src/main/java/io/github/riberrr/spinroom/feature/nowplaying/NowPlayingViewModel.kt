package io.github.riberrr.spinroom.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.QueueItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COVER_PX = 600
private const val ROW_COVER_PX = 96
private const val UPCOMING_COUNT = 3

/**
 * State and actions of the now-playing screen and the mini player (one instance, shared through the activity).
 * Progress is computed locally between MA events: `elapsed_time` is a snapshot, a 1 s ticker advances it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel internal constructor(
    private val backend: PlayerBackend,
    private val nowMs: () -> Long,
    private val tickMs: Long,
) : ViewModel() {

    @Inject constructor(backend: PlayerBackend) : this(backend, { System.nanoTime() / 1_000_000 }, 1_000)

    private val _state = MutableStateFlow(NowPlayingUiState())
    val state: StateFlow<NowPlayingUiState> = _state

    // Progress snapshot taken at the last queue event.
    private var baseMs = 0L
    private var baseAt = 0L

    private var queueId = ""
    private var serverAddress = ""
    private var queue: PlayerQueue? = null
    private var items: List<QueueItem> = emptyList()
    private var favoriteOverride: Pair<String, Boolean>? = null

    private data class Source(val id: String, val address: String)

    init {
        viewModelScope.launch { backend.connection.collect { c -> _state.update { it.copy(connection = c) } } }
        viewModelScope.launch {
            combine(backend.selectedPlayerId, backend.players().catch { emit(emptyList()) }.onStart { emit(emptyList()) }) { id, players -> id to players }
                .collect { (id, players) -> onPlayers(id, players) }
        }
        viewModelScope.launch {
            combine(backend.selectedPlayerId, backend.serverAddress) { id, address -> Source(id, address) }
                .distinctUntilChanged()
                .flatMapLatest { source ->
                    serverAddress = source.address
                    if (source.id.isBlank()) emptyFlow<Pair<PlayerQueue, List<QueueItem>>>() else combine(
                        backend.queue(source.id),
                        backend.queueItems(source.id).catch { emit(emptyList()) }.onStart { emit(emptyList()) },
                    ) { q, i -> q to i }
                }
                .catch { }
                .collect { (q, i) -> onQueue(q, i) }
        }
        viewModelScope.launch {
            _state.map { it.isPlaying }.distinctUntilChanged().collectLatest { playing ->
                if (playing) while (true) {
                    delay(tickMs)
                    _state.update { it.copy(positionMs = position(it.durationMs)) }
                }
            }
        }
    }

    private fun position(durationMs: Long): Long {
        val playing = _state.value.isPlaying
        val raw = if (playing) baseMs + (nowMs() - baseAt) else baseMs
        return if (durationMs > 0) raw.coerceIn(0, durationMs) else raw.coerceAtLeast(0)
    }

    private fun onPlayers(selectedId: String, players: List<Player>) {
        if (selectedId != queueId) resetPlayback(selectedId)
        val selected = players.firstOrNull { it.playerId == selectedId }
        _state.update {
            it.copy(
                playerId = selectedId,
                playerName = selected?.name.orEmpty(),
                volume = selected?.volumeLevel,
                players = players.map { p -> PlayerChoice(p.playerId, p.name, p.playerId == selectedId, p.available) },
            )
        }
    }

    private fun resetPlayback(newId: String) {
        queueId = newId
        queue = null
        items = emptyList()
        favoriteOverride = null
        baseMs = 0
        _state.update {
            it.copy(
                hasTrack = false, title = "", artist = "", album = "", coverUrl = null, isPlaying = false,
                positionMs = 0, durationMs = 0, upcoming = emptyList(), queue = emptyList(), queueExpanded = false,
            )
        }
    }

    private fun onQueue(q: PlayerQueue, queueItems: List<QueueItem>) {
        queue = q
        items = queueItems
        val current = q.currentItem
        val media = current?.mediaItem
        val durationMs = ((media?.duration ?: current?.duration ?: 0) * 1000L)
        val playing = q.state == "playing"
        baseMs = (q.elapsedTime * 1000).toLong()
        baseAt = nowMs()
        val rows = queueItems.mapIndexed { i, item -> item.toRow(q.currentIndex, i) }
        val currentIndex = q.currentIndex ?: -1
        val favorite = favoriteOverride?.takeIf { it.first == current?.queueItemId }?.second ?: (media?.favorite ?: false)
        _state.update {
            it.copy(
                hasTrack = current != null,
                title = media?.name?.ifBlank { null } ?: current?.name.orEmpty(),
                artist = media?.artistLine.orEmpty(),
                album = media?.album?.name.orEmpty(),
                coverUrl = coverUrl(media, current?.image, COVER_PX),
                isPlaying = playing,
                positionMs = baseMs.coerceIn(0, durationMs.takeIf { d -> d > 0 } ?: Long.MAX_VALUE),
                durationMs = durationMs,
                shuffle = q.shuffleEnabled,
                repeat = RepeatMode.fromApi(q.repeatMode),
                favorite = favorite,
                queue = rows,
                upcoming = rows.filter { r -> r.index > currentIndex }.take(UPCOMING_COUNT),
            )
        }
    }

    private fun coverUrl(media: MediaItem?, fallback: io.github.riberrr.spinroom.core.maapi.MediaItemImage?, size: Int) =
        ImageUrls.url(serverAddress, media?.bestImage ?: fallback, size)

    private fun QueueItem.toRow(currentIndex: Int?, position: Int): QueueRow {
        val i = index ?: position
        return QueueRow(
            index = i,
            queueItemId = queueItemId,
            title = mediaItem?.name?.ifBlank { null } ?: name,
            artist = mediaItem?.artistLine.orEmpty(),
            durationMs = (mediaItem?.duration ?: duration ?: 0) * 1000L,
            coverUrl = coverUrl(mediaItem, image, ROW_COVER_PX),
            isCurrent = i == currentIndex,
        )
    }

    // --- actions ---------------------------------------------------------------------------------------------

    private fun command(block: suspend (String) -> Unit) {
        val id = queueId.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { runCatching { block(id) } }
    }

    fun onPlayPause() {
        // Optimistic: freeze/resume the local clock now, the MA event confirms it.
        _state.update {
            baseMs = position(it.durationMs)
            baseAt = nowMs()
            it.copy(isPlaying = !it.isPlaying, positionMs = baseMs)
        }
        command { backend.playPause(it) }
    }

    fun onNext() = command { backend.next(it) }
    fun onPrevious() = command { backend.previous(it) }

    fun onShuffle() {
        val enabled = !_state.value.shuffle
        _state.update { it.copy(shuffle = enabled) }
        command { backend.setShuffle(it, enabled) }
    }

    fun onRepeat() {
        val mode = _state.value.repeat.next()
        _state.update { it.copy(repeat = mode) }
        command { backend.setRepeat(it, mode.api) }
    }

    fun onFavorite() {
        val current = queue?.currentItem ?: return
        val media = current.mediaItem ?: return
        val target = !_state.value.favorite
        favoriteOverride = current.queueItemId to target
        _state.update { it.copy(favorite = target) }
        viewModelScope.launch {
            runCatching { backend.setFavorite(media, target) }.onFailure {
                favoriteOverride = null
                _state.update { s -> s.copy(favorite = !target) }
            }
        }
    }

    fun onPlayIndex(index: Int) = command { backend.playIndex(it, index) }
    fun onToggleQueue() = _state.update { it.copy(queueExpanded = !it.queueExpanded) }
    fun onCollapseQueue() = _state.update { it.copy(queueExpanded = false) }
    fun onOpenPicker() = _state.update { it.copy(pickerOpen = true) }
    fun onClosePicker() = _state.update { it.copy(pickerOpen = false) }

    fun onSelectPlayer(playerId: String) {
        _state.update { it.copy(pickerOpen = false) }
        viewModelScope.launch { backend.selectPlayer(playerId) }
    }
}
