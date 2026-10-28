package io.github.riberrr.spinroom.core.audio.service

import androidx.media3.common.C
import androidx.media3.common.Player
import io.github.riberrr.spinroom.core.audio.AudioEngineState
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommandType
import io.github.riberrr.spinroom.core.sendspin.api.GroupState
import io.github.riberrr.spinroom.core.sendspin.api.TrackMetadata

/** Everything the MediaSession shows, as last seen from the Sendspin session and the audio engine. */
data class PlaybackSnapshot(
    val connected: Boolean = false,
    /** Name of the connected MA server, for the notification. */
    val serverName: String? = null,
    /** False while MA has not allowed this (unpaired) player to play yet. */
    val playbackAllowed: Boolean = false,
    val group: GroupState? = null,
    val metadata: TrackMetadata? = null,
    val audio: AudioEngineState = AudioEngineState.Idle,
    /** Encoded jpeg/png of the current cover, or null. */
    val artwork: ByteArray? = null,
) {
    // ByteArray breaks data-class equality; identity is enough (a new cover is a new array).
    override fun equals(other: Any?) = other is PlaybackSnapshot && connected == other.connected &&
        serverName == other.serverName && playbackAllowed == other.playbackAllowed && group == other.group &&
        metadata == other.metadata && audio == other.audio && artwork === other.artwork

    override fun hashCode() =
        listOf(connected, serverName, playbackAllowed, group, metadata, audio, System.identityHashCode(artwork)).hashCode()
}

enum class MappedState { IDLE, BUFFERING, READY }

/** Remote-facing controls, derived from the server's `supported_commands`. */
enum class Control { PLAY_PAUSE, NEXT, PREVIOUS, STOP, SEEK }

data class TrackInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val artworkUrl: String?,
) {
    /** Stable per track, so a new track is a media item transition for the session. */
    val uid: String get() = "sendspin:" + listOf(title, artist, album).hashCode().toUInt().toString(16)
}

data class MappedPlayback(
    val state: MappedState,
    val playWhenReady: Boolean,
    val track: TrackInfo?,
    val positionMs: Long,
    /** 1.0 while playing at normal speed; 0 when paused. */
    val speed: Float,
    /** Null when unknown or live. */
    val durationMs: Long?,
    val controls: Set<Control>,
)

/** Pure mapping between Sendspin state and Media3 player state/commands (unit-tested on the JVM). */
object PlaybackMapper {
    fun map(s: PlaybackSnapshot, serverNowUs: Long?): MappedPlayback {
        val audioActive = s.audio is AudioEngineState.Playing || s.audio is AudioEngineState.Buffering
        val playing = s.group?.playing ?: audioActive
        val hasItem = s.connected && (s.metadata != null || playing || audioActive)
        val meta = s.metadata
        val progress = meta?.progress
        val durationMs = progress?.durationMs?.takeIf { it > 0 }
        val speed = if (playing) (progress?.playbackSpeedPermille ?: 1000) / 1000f else 0f

        var positionMs = progress?.positionMs ?: 0L
        if (meta != null && progress != null && serverNowUs != null && playing) {
            val elapsedUs = (serverNowUs - meta.timestampUs).coerceAtLeast(0)
            positionMs += (elapsedUs / 1000 * speed).toLong()
        }
        if (durationMs != null) positionMs = positionMs.coerceAtMost(durationMs)

        val state = when {
            !hasItem -> MappedState.IDLE
            playing && s.audio !is AudioEngineState.Playing -> MappedState.BUFFERING
            else -> MappedState.READY
        }
        return MappedPlayback(
            state = state,
            playWhenReady = playing,
            track = if (hasItem) s.metadata.toTrack() else null,
            positionMs = positionMs.coerceAtLeast(0),
            speed = speed,
            durationMs = durationMs,
            controls = if (s.connected) controls(s.group, durationMs != null) else emptySet(),
        )
    }

    private fun TrackMetadata?.toTrack() = TrackInfo(
        title = this?.title,
        artist = this?.artist,
        album = this?.album,
        albumArtist = this?.albumArtist,
        year = this?.year,
        trackNumber = this?.track,
        artworkUrl = this?.artworkUrl,
    )

    private fun controls(group: GroupState?, seekable: Boolean): Set<Control> {
        val supported = group?.supportedCommands ?: return emptySet()
        return buildSet {
            if (ControllerCommandType.PLAY in supported || ControllerCommandType.PAUSE in supported) add(Control.PLAY_PAUSE)
            if (ControllerCommandType.NEXT in supported) add(Control.NEXT)
            if (ControllerCommandType.PREVIOUS in supported) add(Control.PREVIOUS)
            if (ControllerCommandType.STOP in supported) add(Control.STOP)
            if (ControllerCommandType.SEEK in supported && seekable) add(Control.SEEK)
        }
    }

    /** Media3 commands to expose for [controls] (plus the read-only ones every session needs). */
    fun playerCommands(controls: Set<Control>): IntArray = buildList {
        add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
        add(Player.COMMAND_GET_METADATA)
        add(Player.COMMAND_GET_TIMELINE)
        if (Control.PLAY_PAUSE in controls) add(Player.COMMAND_PLAY_PAUSE)
        if (Control.STOP in controls) add(Player.COMMAND_STOP)
        if (Control.NEXT in controls) addAll(listOf(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        if (Control.PREVIOUS in controls) {
            addAll(listOf(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        }
        if (Control.SEEK in controls) {
            addAll(listOf(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD))
        }
    }.toIntArray()

    /**
     * What a Media3 seek means for MA. Next/previous always go to the server (the session has a
     * single item, MA owns the queue and decides "restart vs previous").
     */
    fun commandForSeek(seekCommand: Int, positionMs: Long): ControllerCommand? = when (seekCommand) {
        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> ControllerCommand.Next
        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> ControllerCommand.Previous
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD ->
            if (positionMs == C.TIME_UNSET) null else ControllerCommand.Seek(positionMs.coerceAtLeast(0))
        else -> null
    }

    fun commandForPlayWhenReady(playWhenReady: Boolean): ControllerCommand =
        if (playWhenReady) ControllerCommand.Play else ControllerCommand.Pause
}
