package io.github.riberrr.spinroom.core.audio.service

import androidx.annotation.DrawableRes
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore

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
}
