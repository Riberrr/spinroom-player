package io.github.riberrr.spinroom.core.audio.service

import androidx.annotation.DrawableRes
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * What [SpinroomPlaybackService] needs from the app. The `Application` class implements it
 * (`:app` wires `:core:data` storage), so this module stays free of DI and DataStore.
 */
interface PlaybackHost {
    /** Persistent Sendspin identity and pairing records, fully loaded. */
    suspend fun sendspinCredentials(): CredentialStore

    /** App version for `client/hello` `device_info`. */
    val softwareVersion: String

    /** Small monochrome icon for the playback notification. */
    @get:DrawableRes val notificationIcon: Int

    /** Called on every successful activation by MA, with this TV's Sendspin client id. */
    fun onSendspinConnected(clientId: String, serverName: String?) {}

    /** Saved latency calibration in ms (−500…+500, S14c); the engine follows every change live. */
    val audioCalibrationMs: Flow<Int> get() = emptyFlow()
}
