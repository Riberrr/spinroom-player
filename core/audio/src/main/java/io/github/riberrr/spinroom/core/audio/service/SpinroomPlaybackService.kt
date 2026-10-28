package io.github.riberrr.spinroom.core.audio.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.session.DeviceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Foreground `mediaPlayback` service that makes the TV a Sendspin player: owns the
 * [SendspinReceiver] (server on 8928 + mDNS, audio engine), a Media3 [MediaSession] backed by
 * [SendspinMediaPlayer] (remote keys, other controllers) and its own notification.
 *
 * Runs from app start until the process dies; leaving the app (Home, recents) keeps it playing.
 * The `Application` must implement [PlaybackHost].
 */
@OptIn(UnstableApi::class)
class SpinroomPlaybackService : MediaSessionService() {
    private val main = MainScope()
    private lateinit var host: PlaybackHost
    private lateinit var player: SendspinMediaPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notification: PlaybackNotification

    @Volatile private var receiver: SendspinReceiver? = null
    private var stopped = false

    override fun onCreate() {
        super.onCreate()
        host = application as? PlaybackHost ?: error("Application must implement PlaybackHost")
        player = SendspinMediaPlayer(
            Looper.getMainLooper(),
            serverNowUs = { receiver?.serverNowUs() },
            send = ::send,
        )
        mediaSession = MediaSession.Builder(this, player)
            .apply { launchIntent()?.let(::setSessionActivity) }
            .build()
        notification = PlaybackNotification(this, host.notificationIcon, mediaSession)
        goForeground(PlaybackSnapshot())
        addSession(mediaSession)
        main.launch { run() }
    }

    private suspend fun run() {
        val r = try {
            val credentials = host.sendspinCredentials()
            withContext(worker) {
                if (stopped) return@withContext null
                SendspinReceiver(
                    context = this@SpinroomPlaybackService,
                    credentials = credentials,
                    deviceInfo = DeviceInfo(
                        productName = Build.MODEL,
                        manufacturer = Build.MANUFACTURER,
                        softwareVersion = host.softwareVersion,
                    ),
                    log = ::log,
                    onConnected = host::onSendspinConnected,
                    calibrationMs = host.audioCalibrationMs,
                ).also {
                    receiver = it
                    it.start()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "playback: start failed", e)
            null
        } ?: return
        r.snapshot.collect { s ->
            player.update(s)
            notification.post(s)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PlaybackNotification.ACTION_PLAY -> send(ControllerCommand.Play)
            PlaybackNotification.ACTION_PAUSE -> send(ControllerCommand.Pause)
            PlaybackNotification.ACTION_NEXT -> send(ControllerCommand.Next)
            PlaybackNotification.ACTION_PREVIOUS -> send(ControllerCommand.Previous)
            else -> return super.onStartCommand(intent, flags, startId)
        }
        return START_STICKY
    }

    /** Our notification is posted from [run]; Media3's own would drop the foreground while idle. */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) = Unit

    /** A receiver keeps listening after the app leaves the screen. */
    override fun onTaskRemoved(rootIntent: Intent?) = Unit

    override fun onDestroy() {
        stopped = true
        main.cancel()
        val r = receiver
        receiver = null
        mediaSession.release()
        player.release()
        // Stopping closes sockets and unregisters mDNS (blocks ~1 s): not on the main thread.
        if (r != null) executor.execute { runCatching(r::stop).onFailure { Log.w(TAG, "playback: stop failed", it) } }
        super.onDestroy()
    }

    private fun send(command: ControllerCommand) {
        receiver?.send(command) // non-blocking: queues a client/command frame
    }

    private fun goForeground(s: PlaybackSnapshot) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, PlaybackNotification.ID, notification.build(s), type)
    }

    private fun launchIntent(): PendingIntent? {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun log(msg: String) {
        Log.i(TAG, "sendspin: $msg")
    }

    companion object {
        private const val TAG = "Spinroom"

        /** Receiver start/stop block (sockets, mDNS); one thread keeps a restart from racing for port 8928. */
        private val executor = Executors.newSingleThreadExecutor { Thread(it, "Spinroom-service") }
        private val worker = executor.asCoroutineDispatcher()

        /** Starts the player; call from a visible activity (foreground-service start rules). */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SpinroomPlaybackService::class.java))
        }
    }
}
