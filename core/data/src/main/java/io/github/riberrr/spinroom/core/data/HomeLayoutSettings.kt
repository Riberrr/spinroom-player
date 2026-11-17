package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Rows of the Start screen that the user can reorder or hide (SUI8a). Ids are stable strings. */
object HomeRows {
    const val STATIONS = "stations"
    const val PLAYLISTS = "playlists"
    const val RECENT = "recent"
    val ALL = listOf(STATIONS, PLAYLISTS, RECENT)
}

data class HomeLayout(
    /** Order of all rows (always every id in [HomeRows.ALL] exactly once). */
    val order: List<String> = HomeRows.ALL,
    val hidden: Set<String> = emptySet(),
) {
    /** Rows to show, in order. */
    val visible: List<String> get() = order.filter { it !in hidden }

    fun moved(id: String, delta: Int): HomeLayout {
        val i = order.indexOf(id)
        val j = i + delta
        if (i < 0 || j !in order.indices) return this
        return copy(order = order.toMutableList().also { it.add(j, it.removeAt(i)) })
    }

    fun toggled(id: String): HomeLayout =
        if (id !in HomeRows.ALL) this else copy(hidden = if (id in hidden) hidden - id else hidden + id)

    internal fun encode(): String = order.joinToString(",") + "|" + hidden.joinToString(",")

    companion object {
        /** Tolerant: unknown ids dropped, missing ones appended, duplicates ignored. */
        internal fun decode(raw: String?): HomeLayout {
            if (raw.isNullOrBlank()) return HomeLayout()
            val parts = raw.split("|", limit = 2)
            val order = (parts[0].split(",").filter { it in HomeRows.ALL } + HomeRows.ALL).distinct()
            val hidden = parts.getOrElse(1) { "" }.split(",").filter { it in HomeRows.ALL }.toSet()
            return HomeLayout(order, hidden)
        }
    }
}

class HomeLayoutSettings(private val store: DataStore<Preferences>) {
    val layout: Flow<HomeLayout> = store.data.map { HomeLayout.decode(it[KEY]) }.distinctUntilChanged()

    suspend fun update(transform: (HomeLayout) -> HomeLayout) {
        store.edit { it[KEY] = transform(HomeLayout.decode(it[KEY])).encode() }
    }

    private companion object { val KEY = stringPreferencesKey("home_layout") }
}
