package io.github.riberrr.spinroom.core.maapi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

/**
 * Login flow: connect without token, `auth/login` (short-lived token), `auth`, `auth/token/create`
 * (long-lived, 1 year). Only the long token is returned for storage; the password is never kept.
 */
class AuthApi(private val http: OkHttpClient) {

    class LoginFailed(message: String) : Exception(message)

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
        deviceName: String,
        scope: CoroutineScope,
    ): String {
        val client = MaClient(http, scope)
        try {
            client.connect(baseUrl, token = null)
            withTimeout(15_000) { client.state.first { it is MaConnectionState.Connected || it is MaConnectionState.Failed } }
            val login = client.command(
                "auth/login",
                args("username" to username, "password" to password, "provider_id" to "builtin", "device_name" to deviceName),
            ).decode<AuthLoginResult>()
            val shortToken = login.token
            if (!login.success || shortToken == null) throw LoginFailed(login.error ?: "Nieprawidłowy login lub hasło")
            client.command("auth", args("token" to shortToken))
            return client.command("auth/token/create", args("name" to deviceName)).jsonPrimitive.content
        } finally {
            client.disconnect()
        }
    }
}
