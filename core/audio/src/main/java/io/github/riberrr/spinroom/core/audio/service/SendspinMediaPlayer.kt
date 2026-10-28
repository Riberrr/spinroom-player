package io.github.riberrr.spinroom.core.audio.service

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand

/**
 * Media3 face of the Sendspin player: MA owns the queue and transport, so this player only mirrors
 * the latest [PlaybackSnapshot] (one item = the current track) and turns MediaSession commands
 * (remote keys, notification, other controllers) into Sendspin controller commands.
 *
 * Main-thread only (application looper). Feed it with [update].
 */
@OptIn(UnstableApi::class)
class SendspinMediaPlayer(
    looper: Looper,
    /** Server clock "now" in µs, or null before the clock converged. */
    private val serverNowUs: () -> Long?,
    private val send: (ControllerCommand) -> Unit,
) : SimpleBasePlayer(looper) {
    private var snapshot = PlaybackSnapshot()

    fun update(newSnapshot: PlaybackSnapshot) {
        if (newSnapshot == snapshot) return
        snapshot = newSnapshot
        invalidateState()
    }

    override fun getState(): State {
        val m = PlaybackMapper.map(snapshot, serverNowUs())
        val builder = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAll(*PlaybackMapper.playerCommands(m.controls)).build())
            .setPlayWhenReady(m.playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setPlaybackState(
                when (m.state) {
                    MappedState.IDLE -> Player.STATE_IDLE
                    MappedState.BUFFERING -> Player.STATE_BUFFERING
                    MappedState.READY -> Player.STATE_READY
                },
            )
        val track = m.track ?: return builder.build()
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setAlbumArtist(track.albumArtist)
            .setRecordingYear(track.year)
            .setTrackNumber(track.trackNumber)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .apply {
                val art = snapshot.artwork
                if (art != null) setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
            .build()
        val item = MediaItemData.Builder(track.uid)
            .setMediaItem(MediaItem.Builder().setMediaId(track.uid).setMediaMetadata(metadata).build())
            .setMediaMetadata(metadata)
            .setDurationUs(m.durationMs?.let { it * 1000 } ?: C.TIME_UNSET)
            .setIsSeekable(Control.SEEK in m.controls)
            .build()
        return builder
            .setPlaylist(listOf(item))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(
                if (m.speed > 0f) PositionSupplier.getExtrapolating(m.positionMs, m.speed)
                else PositionSupplier.getConstant(m.positionMs),
            )
            .setPlaybackParameters(PlaybackParameters(if (m.speed > 0f) m.speed else 1f))
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        send(PlaybackMapper.commandForPlayWhenReady(playWhenReady))
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        PlaybackMapper.commandForSeek(seekCommand, positionMs)?.let(send)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        send(ControllerCommand.Stop)
        return Futures.immediateVoidFuture()
    }
}
