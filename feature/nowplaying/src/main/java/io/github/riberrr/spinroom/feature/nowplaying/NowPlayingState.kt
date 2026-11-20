package io.github.riberrr.spinroom.feature.nowplaying

import io.github.riberrr.spinroom.core.data.Appearance
import io.github.riberrr.spinroom.core.design.MiniPlayerState
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

enum class RepeatMode(val api: String) {
    OFF("off"), ONE("one"), ALL("all");

    /** Remote cycle: off -> all -> one -> off. */
    fun next() = when (this) { OFF -> ALL; ALL -> ONE; ONE -> OFF }

    companion object {
        fun fromApi(value: String) = entries.firstOrNull { it.api == value } ?: OFF
    }
}

/** One row of the queue list. [index] is the position in the MA queue (used to jump with `play_index`). */
data class QueueRow(
    val index: Int,
    val queueItemId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val coverUrl: String?,
    val isCurrent: Boolean,
)

/** One favorite station in the radio panel. [number] is its place in the favorites order (also the CH+/CH- order). */
data class StationRow(
    val number: Int,
    val uri: String,
    val name: String,
    val imageUrl: String?,
    /** What the station plays now; known only for the current station. */
    val nowPlaying: String,
    val isCurrent: Boolean,
)

data class PlayerChoice(val id: String, val name: String, val selected: Boolean, val available: Boolean)

/** Everything the screen and the mini player render. Times in milliseconds. */
data class NowPlayingUiState(
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val playerId: String = "",
    val playerName: String = "",
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val favorite: Boolean = false,
    /** Current item is a radio station (SUI2: dock shows favorite stations, CH+/CH- switch stations). */
    val isRadio: Boolean = false,
    /** Uri and name of the current media item (to find the playing station among the favorites). */
    val mediaUri: String = "",
    val mediaName: String = "",
    val volume: Int? = null,
    /** Radio: favorite stations in the user's order (replaces the queue panel). */
    val stations: List<StationRow> = emptyList(),
    /** Radio: "source · description" line shown instead of the progress bar. */
    val radioMeta: String = "",
    /** [Appearance.BG_BLUR], [Appearance.BG_COLOR] or [Appearance.BG_SOLID]. */
    val playerBackground: String = Appearance.BG_BLUR,
    /** Next few tracks after the current one (preview card). */
    val upcoming: List<QueueRow> = emptyList(),
    /** Whole queue (expanded view). */
    val queue: List<QueueRow> = emptyList(),
    val queueExpanded: Boolean = false,
    val players: List<PlayerChoice> = emptyList(),
    val pickerOpen: Boolean = false,
    /** A track/station was requested but audio has not started yet (drives the loading animation). */
    val loading: Boolean = false,
) {
    val hasPlayer: Boolean get() = playerId.isNotBlank()
    val isConnected: Boolean get() = connection is MaConnectionState.Connected

    fun toMiniPlayerState() = MiniPlayerState(title, artist, isPlaying, positionMs, durationMs, loading)
}
