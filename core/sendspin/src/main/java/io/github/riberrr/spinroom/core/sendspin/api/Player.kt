package io.github.riberrr.spinroom.core.sendspin.api

import kotlinx.coroutines.flow.Flow

/** What the player advertises in `client/hello` (`player@v1_support`). In priority order. */
data class PlayerCapabilities(
    val supportedFormats: List<StreamFormat>,
    /** Max bytes of outstanding chunks (13-byte header + payload each). */
    val bufferCapacityBytes: Int,
)

/** Player state reported in `client/state`. S04 owns it, S02 serialises it. */
data class PlayerState(
    val volume: Int,
    val muted: Boolean,
    val outputDelayMs: Int = 0,
    val requiredLeadTimeMs: Int,
    val minBufferMs: Int,
    val supportedCommands: Set<PlayerCommandType> = PlayerCommandType.entries.toSet(),
    /** Preferred format, must be one of the advertised ones; null = no preference. */
    val preferredFormat: StreamFormat? = null,
)

enum class PlayerCommandType { VOLUME, MUTE, SET_OUTPUT_DELAY }

/** `server/command` player object. */
sealed interface PlayerCommand {
    data class SetVolume(val volume: Int) : PlayerCommand
    data class SetMuted(val muted: Boolean) : PlayerCommand
    data class SetOutputDelay(val delayMs: Int) : PlayerCommand
}

/** Events S02 delivers to the audio engine (S04), in wire order. */
sealed interface PlayerEvent {
    /** `stream/start`; also sent mid-stream to change format without a gap. */
    data class StreamStarted(val format: StreamFormat) : PlayerEvent
    data class Chunk(val chunk: AudioChunk) : PlayerEvent
    /** `stream/clear`: drop buffered audio, keep the stream (seek / track jump). */
    data object StreamCleared : PlayerEvent
    /** `stream/end`: stop output and clear buffers. */
    data object StreamEnded : PlayerEvent
    data class Command(val command: PlayerCommand) : PlayerEvent
}

/** Implemented by S02 (protocol side), collected by S04 (audio side). */
interface PlayerStream {
    val events: Flow<PlayerEvent>
}

/** Implemented by S02, called by S04 whenever local state changes (incl. volume set by remote keys). */
interface PlayerStateReporter {
    /** Reports the full state; S02 sends `client/state`. Safe to call before the session is up. */
    fun report(state: PlayerState)

    /** Audio engine is ready to take over; false while another app owns audio focus. */
    fun setAvailable(available: Boolean)
}
