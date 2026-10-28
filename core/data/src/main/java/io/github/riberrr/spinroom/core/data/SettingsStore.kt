package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    /** `host[:port]` or full URL as typed by the user; empty = not configured. */
    val serverAddress: String = "",
    /** Long-lived MA token. Never log. */
    val token: String = "",
    /** MA player id of this TV (Sendspin); empty until the player registers. */
    val playerId: String = "",
    /** Start the TV player after the TV boots (BOOT_COMPLETED), so it shows up in MA without opening the app. */
    val autostart: Boolean = true,
) {
    val isConfigured: Boolean get() = serverAddress.isNotBlank() && token.isNotBlank()
}

/** Server address, token and TV player id in DataStore. The password is never stored. */
class SettingsStore(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map {
        AppSettings(it[ADDRESS].orEmpty(), it[TOKEN].orEmpty(), it[PLAYER_ID].orEmpty(), it[AUTOSTART] ?: true)
    }

    suspend fun setServerAddress(value: String) { store.edit { it[ADDRESS] = value.trim() } }
    suspend fun setToken(value: String) { store.edit { it[TOKEN] = value } }
    suspend fun setPlayerId(value: String) { store.edit { it[PLAYER_ID] = value } }

    suspend fun setAutostart(value: Boolean) { store.edit { it[AUTOSTART] = value } }

    suspend fun saveLogin(address: String, token: String) {
        store.edit {
            it[ADDRESS] = address.trim()
            it[TOKEN] = token
        }
    }

    /** Forget the token (log out) but keep the address. */
    suspend fun clearToken() { store.edit { it.remove(TOKEN) } }

    private companion object {
        val ADDRESS = stringPreferencesKey("server_address")
        val TOKEN = stringPreferencesKey("token")
        val PLAYER_ID = stringPreferencesKey("player_id")
        val AUTOSTART = booleanPreferencesKey("autostart")
    }
}
