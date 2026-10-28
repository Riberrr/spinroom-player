package io.github.riberrr.spinroom

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.github.riberrr.spinroom.core.audio.service.PlaybackHost
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.di.AppScope
import io.github.riberrr.spinroom.core.data.di.SendspinCredentials
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class SpinroomApp : Application(), PlaybackHost {
    @Inject lateinit var credentials: SendspinCredentials
    @Inject lateinit var settings: SettingsStore
    @Inject @AppScope lateinit var appScope: CoroutineScope

    override suspend fun sendspinCredentials(): CredentialStore = credentials.get()

    override val softwareVersion: String get() = BuildConfig.VERSION_NAME

    override val notificationIcon: Int get() = R.drawable.ic_launcher_monochrome

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
