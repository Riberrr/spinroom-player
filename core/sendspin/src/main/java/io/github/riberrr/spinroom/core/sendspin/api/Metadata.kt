package io.github.riberrr.spinroom.core.sendspin.api

import kotlinx.coroutines.flow.Flow

/** `server/state.metadata`. Text is plain text, never markup. */
data class TrackMetadata(
    /** Server clock µs at which this state took effect; progress extrapolates from it. */
    val timestampUs: Long,
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val artworkUrl: String? = null,
    val year: Int? = null,
    val track: Int? = null,
    val progress: TrackProgress? = null,
)

data class TrackProgress(
    val positionMs: Long,
    /** 0 = unknown / unlimited (live radio). */
    val durationMs: Long,
    /** Speed ×1000; 1000 = normal, 0 = paused. */
    val playbackSpeedPermille: Int,
)

enum class RepeatMode { OFF, ONE, ALL }

/** `group/update` + `server/state.controller`. */
data class GroupState(
    val groupId: String,
    val groupName: String,
    val playing: Boolean,
    val volume: Int,
    val muted: Boolean,
    val repeat: RepeatMode,
    val shuffle: Boolean,
    val supportedCommands: Set<ControllerCommandType>,
    val seekMaxMs: Long? = null,
)

enum class ControllerCommandType {
    PLAY, PAUSE, STOP, NEXT, PREVIOUS, VOLUME, MUTE,
    REPEAT_OFF, REPEAT_ONE, REPEAT_ALL, SHUFFLE, UNSHUFFLE, SWITCH, SEEK, SEEK_RELATIVE,
}

/** `client/command` controller object; send only types listed in [GroupState.supportedCommands]. */
sealed interface ControllerCommand {
    data object Play : ControllerCommand
    data object Pause : ControllerCommand
    data object Stop : ControllerCommand
    data object Next : ControllerCommand
    data object Previous : ControllerCommand
    data class SetGroupVolume(val volume: Int) : ControllerCommand
    data class SetGroupMuted(val muted: Boolean) : ControllerCommand
    data class SetRepeat(val mode: RepeatMode) : ControllerCommand
    data class SetShuffle(val shuffle: Boolean) : ControllerCommand
    data class Seek(val positionMs: Long) : ControllerCommand
    data class SeekRelative(val offsetMs: Long) : ControllerCommand
}

/** Decoded album artwork for channel 0 (encoded jpeg/png bytes; null bytes = cleared). */
class ArtworkImage(val timestampUs: Long, val bytes: ByteArray?)

/** What the UI / MediaSession side observes. Implemented by S02. */
interface SendspinSessionState {
    val metadata: Flow<TrackMetadata?>
    val group: Flow<GroupState?>
    val artwork: Flow<ArtworkImage?>
    suspend fun send(command: ControllerCommand)
}
