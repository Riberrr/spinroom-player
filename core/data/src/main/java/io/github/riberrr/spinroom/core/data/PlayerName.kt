package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.riberrr.spinroom.core.maapi.MaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Validation of the TV player's display name (SUI8a). */
object PlayerName {
    const val MAX_LENGTH = 40
    val SUGGESTIONS = listOf("Salon", "Sypialnia", "Kuchnia", "Gabinet", "Pokój dziecięcy")

    sealed interface Result {
        data class Ok(val name: String) : Result
        data object Empty : Result
        data object TooLong : Result
    }

    fun validate(raw: String): Result {
        val name = raw.trim()
        return when {
            name.isEmpty() -> Result.Empty
            name.length > MAX_LENGTH -> Result.TooLong
            else -> Result.Ok(name)
        }
    }
}

/** User-chosen name of this TV's player; null = none chosen (device name is used). */
class PlayerNameStore(private val store: DataStore<Preferences>) {
    val name: Flow<String?> = store.data.map { it[KEY]?.takeIf(String::isNotBlank) }
    suspend fun set(name: String) { store.edit { it[KEY] = name } }

    private companion object { val KEY = stringPreferencesKey("player_name") }
}

/**
 * Renames a player in MA through its player config (`config/players/save`, key `name`).
 * The player keeps its id, so no re-pairing is needed.
 */
class PlayerRenamer(private val client: MaClient) {
    suspend fun rename(playerId: String, name: String) {
        client.command(
            "config/players/save",
            buildJsonObject {
                put("player_id", playerId)
                put("values", buildJsonObject { put("name", name) })
            },
        )
    }
}
