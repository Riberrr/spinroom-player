package io.github.riberrr.spinroom.core.audio.service

import androidx.media3.common.C
import androidx.media3.common.Player
import io.github.riberrr.spinroom.core.audio.AudioEngineState
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommandType
import io.github.riberrr.spinroom.core.sendspin.api.GroupState
import io.github.riberrr.spinroom.core.sendspin.api.RepeatMode
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import io.github.riberrr.spinroom.core.sendspin.api.TrackMetadata
import io.github.riberrr.spinroom.core.sendspin.api.TrackProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMapperTest {
    private val format = StreamFormat(Codec.FLAC, 48_000, 2, 16)
    private val all = ControllerCommandType.entries.toSet()

    private fun group(playing: Boolean, commands: Set<ControllerCommandType> = all) =
        GroupState("g", "Salon", playing, 50, false, RepeatMode.OFF, false, commands)

    private fun meta(positionMs: Long = 10_000, durationMs: Long = 200_000, speed: Int = 1000) =
        TrackMetadata(
            timestampUs = 1_000_000,
            title = "Song",
            artist = "Artist",
            album = "Album",
            progress = TrackProgress(positionMs, durationMs, speed),
        )

    @Test fun disconnectedIsIdleWithoutControls() {
        val m = PlaybackMapper.map(PlaybackSnapshot(group = group(true), metadata = meta()), serverNowUs = null)
        assertEquals(MappedState.IDLE, m.state)
        assertNull(m.track)
        assertTrue(m.controls.isEmpty())
    }

    @Test fun playingWithAudioIsReadyAndExtrapolatesPosition() {
        val s = PlaybackSnapshot(connected = true, group = group(true), metadata = meta(), audio = AudioEngineState.Playing(format))
        val m = PlaybackMapper.map(s, serverNowUs = 3_000_000) // 2 s after the metadata timestamp
        assertEquals(MappedState.READY, m.state)
        assertTrue(m.playWhenReady)
        assertEquals(12_000, m.positionMs)
        assertEquals(1f, m.speed)
        assertEquals(200_000L, m.durationMs)
        assertEquals("Song", m.track?.title)
        assertEquals(setOf(Control.PLAY_PAUSE, Control.NEXT, Control.PREVIOUS, Control.STOP, Control.SEEK), m.controls)
    }

    @Test fun positionIsClampedToDuration() {
        val s = PlaybackSnapshot(connected = true, group = group(true), metadata = meta(positionMs = 199_000, durationMs = 200_000))
        assertEquals(200_000, PlaybackMapper.map(s, serverNowUs = 60_000_000).positionMs)
    }

    @Test fun playingBeforeAudioArrivesIsBuffering() {
        val s = PlaybackSnapshot(connected = true, group = group(true), metadata = meta(), audio = AudioEngineState.Idle)
        assertEquals(MappedState.BUFFERING, PlaybackMapper.map(s, null).state)
    }

    @Test fun pausedKeepsPositionAndTrack() {
        val s = PlaybackSnapshot(connected = true, group = group(false), metadata = meta(speed = 0))
        val m = PlaybackMapper.map(s, serverNowUs = 9_000_000)
        assertEquals(MappedState.READY, m.state)
        assertFalse(m.playWhenReady)
        assertEquals(10_000, m.positionMs)
        assertEquals(0f, m.speed)
    }

    @Test fun stoppedGroupWithoutMetadataIsIdleButCanResume() {
        val m = PlaybackMapper.map(PlaybackSnapshot(connected = true, group = group(false)), null)
        assertEquals(MappedState.IDLE, m.state)
        assertNull(m.track)
        assertTrue(Control.PLAY_PAUSE in m.controls)
    }

    @Test fun liveStreamHasNoSeek() {
        val s = PlaybackSnapshot(connected = true, group = group(true), metadata = meta(durationMs = 0))
        val m = PlaybackMapper.map(s, null)
        assertNull(m.durationMs)
        assertFalse(Control.SEEK in m.controls)
    }

    @Test fun controlsFollowSupportedCommands() {
        val g = group(true, setOf(ControllerCommandType.PAUSE, ControllerCommandType.NEXT))
        val m = PlaybackMapper.map(PlaybackSnapshot(connected = true, group = g, metadata = meta()), null)
        assertEquals(setOf(Control.PLAY_PAUSE, Control.NEXT), m.controls)
        val commands = PlaybackMapper.playerCommands(m.controls).toSet()
        assertTrue(Player.COMMAND_PLAY_PAUSE in commands)
        assertTrue(Player.COMMAND_SEEK_TO_NEXT in commands)
        assertFalse(Player.COMMAND_SEEK_TO_PREVIOUS in commands)
        assertTrue(Player.COMMAND_GET_METADATA in commands)
    }

    @Test fun seekCommandsMapToController() {
        assertEquals(ControllerCommand.Next, PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_TO_NEXT, C.TIME_UNSET))
        assertEquals(ControllerCommand.Next, PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, C.TIME_UNSET))
        // seekToPrevious on a single item becomes "seek to 0 with COMMAND_SEEK_TO_PREVIOUS": still MA's previous.
        assertEquals(ControllerCommand.Previous, PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_TO_PREVIOUS, 0))
        assertEquals(ControllerCommand.Seek(42_000), PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, 42_000))
        assertNull(PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, C.TIME_UNSET))
        assertNull(PlaybackMapper.commandForSeek(Player.COMMAND_SEEK_TO_DEFAULT_POSITION, 0))
        assertEquals(ControllerCommand.Play, PlaybackMapper.commandForPlayWhenReady(true))
        assertEquals(ControllerCommand.Pause, PlaybackMapper.commandForPlayWhenReady(false))
    }

    @Test fun trackUidChangesWithTrack() {
        val a = TrackInfo("A", "X", "Y", null, null, null, null)
        assertEquals(a.uid, a.copy(year = 2020).uid)
        assertNotEquals(a.uid, a.copy(title = "B").uid)
    }
}
