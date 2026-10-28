package io.github.riberrr.spinroom

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath
import io.github.riberrr.spinroom.core.audio.service.PlaybackHost
import io.github.riberrr.spinroom.core.data.LatencyStore
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.di.AppScope
import io.github.riberrr.spinroom.core.data.di.SendspinCredentials
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class SpinroomApp : Application(), PlaybackHost, SingletonImageLoader.Factory {
    @Inject lateinit var credentials: SendspinCredentials
    @Inject lateinit var settings: SettingsStore
    @Inject @AppScope lateinit var appScope: CoroutineScope
    @Inject lateinit var latency: LatencyStore

    /** Covers are small server-resized thumbnails: 20% memory cache + 100 MB disk cache keep grid scrolling off the network. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("covers").toOkioPath()).maxSizeBytes(100L * 1024 * 1024).build() }
            .build()

    override suspend fun sendspinCredentials(): CredentialStore = credentials.get()

    override val softwareVersion: String get() = BuildConfig.VERSION_NAME

    override val notificationIcon: Int get() = R.drawable.ic_launcher_monochrome

    override val audioCalibrationMs: Flow<Int> get() = latency.calibrationMs

    /**
     * MA registers a Sendspin player under its client id: make it the app's player
     * (Now Playing, `settings.playerId`) unless the user already picked another one.
     */
    override fun onSendspinConnected(clientId: String, serverName: String?) {
        appScope.launch {
            if (settings.settings.first().playerId.isEmpty()) {
                settings.setPlayerId(clientId)
                Log.i("Spinroom", "playback: player id set to this TV")
            }
        }
    }
}
