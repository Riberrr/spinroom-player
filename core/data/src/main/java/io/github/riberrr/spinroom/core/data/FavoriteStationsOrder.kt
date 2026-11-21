package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Pure ordering rules for favorite stations (keys are library URIs, e.g. `library://radio/3`). */
object StationOrder {
    /** Keeps [saved] order for keys still in [current]; keys new in [current] go to the end in [current]'s order. */
    fun merge(saved: List<String>, current: List<String>): List<String> {
        val live = current.toSet()
        val kept = saved.filter { it in live }.distinct()
        val keptSet = kept.toSet()
        return kept + current.filter { it !in keptSet }.distinct()
    }

    /** Moves [key] by [delta] places (negative = towards the front), clamped to the list. Unknown key: unchanged. */
    fun move(order: List<String>, key: String, delta: Int): List<String> {
        val from = order.indexOf(key)
        if (from < 0) return order
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return order
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }

    /**
     * Gives [key] the number [number] (1-based). Numbers are positions, so the station that held [number] swaps places with
     * [key] and takes its old number (nobody is left without one). Unknown key or a number past the end: unchanged.
     */
    fun assign(order: List<String>, key: String, number: Int): List<String> {
        val from = order.indexOf(key)
        val to = number - 1
        if (from < 0 || to !in order.indices || to == from) return order
        return order.toMutableList().apply { this[from] = this[to]; this[to] = key }
    }

    fun remove(order: List<String>, key: String): List<String> = order.filter { it != key }

    fun add(order: List<String>, key: String): List<String> = if (key in order) order else order + key
}

/**
 * User-defined order of favorite stations (1…n; also the CH+/CH- order). Stored in the shared settings DataStore.
 * The stations themselves come from MA favorites; [FavoriteStationsRepository] joins the two.
 */
class FavoriteStationsOrder(private val store: DataStore<Preferences>) {
    val order: Flow<List<String>> = store.data.map { decode(it[ORDER]) }.distinctUntilChanged()

    suspend fun current(): List<String> = decode(store.data.first()[ORDER])

    suspend fun save(order: List<String>) {
        store.edit { it[ORDER] = order.joinToString(SEP) }
    }

    private fun decode(raw: String?): List<String> = raw?.split(SEP)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val SEP = "\n"
        val ORDER = stringPreferencesKey("favorite_stations_order")
    }
}
