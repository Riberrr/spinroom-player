package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.AuthApi
import io.github.riberrr.spinroom.core.maapi.MaClient
import kotlinx.coroutines.CoroutineScope

/** Logs in to MA and persists only the resulting long-lived token. */
class AuthRepository(
    private val authApi: AuthApi,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    /** Throws [AuthApi.LoginFailed] for bad credentials, other exceptions for network problems. */
    suspend fun login(address: String, username: String, password: String, deviceName: String = "Spinroom TV") {
        val token = authApi.login(MaClient.normalizeBaseUrl(address), username, password, deviceName, scope)
        settings.saveLogin(address, token)
    }

    suspend fun logout() = settings.clearToken()
}
