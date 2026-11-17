package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import io.github.riberrr.spinroom.core.maapi.RadioApi
import io.github.riberrr.spinroom.core.maapi.RadioFilter
import io.github.riberrr.spinroom.core.maapi.RadioFilterKind
import io.github.riberrr.spinroom.core.maapi.RadioFilterOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A radio station as the UI needs it. [uri] is what `play_media` takes. */
data class Station(
    val uri: String,
    val name: String,
    val imageUrl: String?,
    /** Id at the source provider (Radio Browser uuid); lets a browse row find its favorite. Empty for library-only items. */
    val sourceId: String = "",
    /** Library id; only favorites have one. Needed to remove a favorite. */
    val libraryId: String = "",
)

/**
 * Favorite stations in the user's order + Radio Browser browsing and search.
 *
 * [favorites] is the single source of the CH+/CH- order (see README): MA favorites sorted by [FavoriteStationsOrder];
 * a station added later goes to the end. Call [refresh] after connecting and after changing favorites elsewhere.
 */
class FavoriteStationsRepository(
    private val library: LibraryApi,
    private val radio: RadioApi,
    private val order: FavoriteStationsOrder,
    private val settings: SettingsStore,
    private val prefs: DataStore<Preferences>,
) {
    private val _favorites = MutableStateFlow<List<Station>>(emptyList())

    /** Ordered favorites; the position in this list is the station number (index + 1). */
    val favorites: StateFlow<List<Station>> = _favorites

    /** Re-reads MA favorites and re-applies the saved order. */
    suspend fun refresh() {
        val address = settings.settings.first().serverAddress
        val items = library.items(MediaTypes.RADIOS, limit = FAVORITES_LIMIT, favorite = true, orderBy = "name")
        val stations = items.mapNotNull { it.toStation(address) }
        val merged = StationOrder.merge(order.current(), stations.map { it.uri })
        order.save(merged)
        val byUri = stations.associateBy { it.uri }
        _favorites.value = merged.mapNotNull { byUri[it] }
    }

    suspend fun move(station: Station, delta: Int) = reorder { StationOrder.move(it, station.uri, delta) }

    /** Removes from MA favorites and from the order. */
    suspend fun remove(station: Station) {
        if (station.libraryId.isNotBlank()) library.removeFavorite(RADIO_TYPE, station.libraryId)
        reorder { StationOrder.remove(it, station.uri) }
    }

    /** Adds a station (by its provider uri, e.g. `radiobrowser://radio/…`) to MA favorites; it lands last. */
    suspend fun add(uri: String) {
        library.addFavorite(uri)
        refresh()
    }

    /** The favorite that corresponds to a browse row, if any. */
    fun favoriteFor(item: MediaItem): Station? = favoriteIn(_favorites.value, item)

    /** Favorite that follows [current] when stepping with CH+/CH- (wraps). Null if there are no favorites. */
    fun step(current: Station?, forward: Boolean): Station? = stepIn(_favorites.value, current, forward)

    private suspend fun reorder(change: (List<String>) -> List<String>) {
        val next = change(_favorites.value.map { it.uri })
        order.save(next)
        val byUri = _favorites.value.associateBy { it.uri }
        _favorites.value = next.mapNotNull { byUri[it] }
    }

    // ---- browsing ----

    suspend fun stations(filter: RadioFilter): List<MediaItem> = radio.stations(filter)
    suspend fun search(query: String): List<MediaItem> = radio.search(query)
    suspend fun options(kind: RadioFilterKind): List<RadioFilterOption> = radio.options(kind)

    suspend fun imageUrl(item: MediaItem, sizePx: Int = 256): String? =
        ImageUrls.url(settings.settings.first().serverAddress, item.bestImage, sizePx)

    /** Last chosen filter, restored on the next start. Default: language Polish. */
    val savedFilter: Flow<RadioFilter> = prefs.data.map { filterFrom(it[FILTER_KIND], it[FILTER_KEY]) }

    suspend fun saveFilter(filter: RadioFilter) {
        prefs.edit {
            it[FILTER_KIND] = filterKind(filter)
            it[FILTER_KEY] = filterKey(filter)
        }
    }

    private fun MediaItem.toStation(address: String): Station? {
        val u = uri ?: return null
        val source = providerMappings.firstOrNull { it.providerDomain == "radiobrowser" }?.itemId.orEmpty()
        return Station(u, name, ImageUrls.url(address, bestImage, 256), source, if (provider == "library") itemId else "")
    }

    companion object {
        const val FAVORITES_LIMIT = 100
        private const val RADIO_TYPE = "radio"
        val DEFAULT_FILTER: RadioFilter = RadioFilter.Language("polish")
        private val FILTER_KIND = stringPreferencesKey("radio_filter_kind")
        private val FILTER_KEY = stringPreferencesKey("radio_filter_key")

        fun favoriteIn(favorites: List<Station>, item: MediaItem): Station? =
            favorites.firstOrNull { it.uri == item.uri || (it.sourceId.isNotBlank() && it.sourceId == item.itemId) }

        fun stepIn(list: List<Station>, current: Station?, forward: Boolean): Station? {
            if (list.isEmpty()) return null
            val i = list.indexOfFirst { current != null && (it.uri == current.uri || it.name.equals(current.name, true)) }
            return list[if (i < 0) 0 else Math.floorMod(i + if (forward) 1 else -1, list.size)]
        }

        fun filterKind(f: RadioFilter) = when (f) {
            is RadioFilter.Language -> "language"
            is RadioFilter.Country -> "country"
            is RadioFilter.Tag -> "tag"
            RadioFilter.Popular -> "popular"
        }

        fun filterKey(f: RadioFilter) = when (f) {
            is RadioFilter.Language -> f.key
            is RadioFilter.Country -> f.key
            is RadioFilter.Tag -> f.key
            RadioFilter.Popular -> ""
        }

        /** Missing or unknown values fall back to [DEFAULT_FILTER]. */
        fun filterFrom(kind: String?, key: String?): RadioFilter = when {
            kind == "popular" -> RadioFilter.Popular
            key.isNullOrBlank() -> DEFAULT_FILTER
            kind == "language" -> RadioFilter.Language(key)
            kind == "country" -> RadioFilter.Country(key)
            kind == "tag" -> RadioFilter.Tag(key)
            else -> DEFAULT_FILTER
        }
    }
}
