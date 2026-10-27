package io.github.riberrr.spinroom.core.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import io.github.riberrr.spinroom.core.audio.R

/**
 * The service's own foreground notification. Media3's notification only stays in the foreground
 * while something plays; the Sendspin receiver must stay reachable while idle too.
 */
@OptIn(UnstableApi::class)
internal class PlaybackNotification(
    private val context: Context,
    @param:DrawableRes private val icon: Int,
    private val session: MediaSession,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var lastArtwork: ByteArray? = null
    private var lastBitmap: Bitmap? = null

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.spinroom_playback_channel), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
        }
    }

    fun build(s: PlaybackSnapshot): Notification {
        val m = PlaybackMapper.map(s, serverNowUs = null)
        val track = m.track
        val status = when {
            !s.connected -> context.getString(R.string.spinroom_playback_waiting)
            !s.playbackAllowed -> context.getString(R.string.spinroom_playback_needs_approval)
            s.serverName.isNullOrBlank() -> context.getString(R.string.spinroom_playback_connected_unnamed)
            else -> context.getString(R.string.spinroom_playback_connected, s.serverName)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(icon)
            .setContentTitle(
                track?.title ?: if (track != null) context.getString(R.string.spinroom_playback_unknown_title)
                else context.getString(R.string.spinroom_playback_idle_title),
            )
            .setContentText(track?.artist ?: status)
            .setSubText(if (track != null) track.album else null)
            .setLargeIcon(bitmap(s.artwork))
            .setContentIntent(session.sessionActivity)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        val compact = ArrayList<Int>()
        if (Control.PREVIOUS in m.controls) {
            builder.addAction(android.R.drawable.ic_media_previous, context.getString(R.string.spinroom_action_previous), action(ACTION_PREVIOUS))
            compact += compact.size
        }
        if (Control.PLAY_PAUSE in m.controls) {
            if (m.playWhenReady) {
                builder.addAction(android.R.drawable.ic_media_pause, context.getString(R.string.spinroom_action_pause), action(ACTION_PAUSE))
            } else {
                builder.addAction(android.R.drawable.ic_media_play, context.getString(R.string.spinroom_action_play), action(ACTION_PLAY))
            }
            compact += compact.size
        }
        if (Control.NEXT in m.controls) {
            builder.addAction(android.R.drawable.ic_media_next, context.getString(R.string.spinroom_action_next), action(ACTION_NEXT))
            compact += compact.size
        }
        builder.setStyle(MediaStyleNotificationHelper.MediaStyle(session).setShowActionsInCompactView(*compact.toIntArray()))
        return builder.build()
    }

    fun post(s: PlaybackSnapshot) {
        runCatching { manager?.notify(ID, build(s)) } // posting can be refused (no permission) — the service still runs
    }

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        context,
        name.hashCode(),
        Intent(name).setClass(context, SpinroomPlaybackService::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun bitmap(bytes: ByteArray?): Bitmap? {
        if (bytes === lastArtwork) return lastBitmap
        lastArtwork = bytes
        lastBitmap = bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
        return lastBitmap
    }

    companion object {
        const val ID = 0x5350 // "SP"
        private const val CHANNEL = "spinroom_playback"
        const val ACTION_PLAY = "io.github.riberrr.spinroom.action.PLAY"
        const val ACTION_PAUSE = "io.github.riberrr.spinroom.action.PAUSE"
        const val ACTION_NEXT = "io.github.riberrr.spinroom.action.NEXT"
        const val ACTION_PREVIOUS = "io.github.riberrr.spinroom.action.PREVIOUS"
    }
}
