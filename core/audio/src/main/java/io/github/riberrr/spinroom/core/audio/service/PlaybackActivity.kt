package io.github.riberrr.spinroom.core.audio.service

import io.github.riberrr.spinroom.core.audio.AudioEngineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide view of what Spinroom is playing, published by [SpinroomPlaybackService] (SUI9).
 * The background key service reads [active] synchronously: it may only grab keys while something plays or is paused.
 */
object PlaybackActivity {
    /** What the corner card shows as "now playing". */
    data class Now(val title: String?, val artist: String?, val artworkUrl: String?)

    private val _active = MutableStateFlow(false)

    /** true = something plays or is paused in Spinroom (a track is known or audio is flowing). */
    val active: StateFlow<Boolean> = _active

    private val _now = MutableStateFlow<Now?>(null)
    val now: StateFlow<Now?> = _now

    internal fun publish(s: PlaybackSnapshot) {
        _active.value = isActive(s)
        _now.value = s.metadata?.let { Now(it.title, it.artist ?: it.albumArtist, it.artworkUrl) }
    }

    internal fun reset() {
        _active.value = false
        _now.value = null
    }

    internal fun isActive(s: PlaybackSnapshot) =
        s.connected && (s.metadata != null || s.audio is AudioEngineState.Playing || s.audio is AudioEngineState.Buffering)
}
