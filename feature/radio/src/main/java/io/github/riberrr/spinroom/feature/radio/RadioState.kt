package io.github.riberrr.spinroom.feature.radio

import io.github.riberrr.spinroom.core.data.Station
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.RadioFilter
import io.github.riberrr.spinroom.core.maapi.RadioFilterKind
import io.github.riberrr.spinroom.core.maapi.RadioFilterOption

/** A station in the browse/search list. [item] keeps the server object for add/remove favorite. */
data class RadioRow(val item: MediaItem, val name: String, val imageUrl: String?, val subtitle: String) {
    val uri: String get() = item.uri.orEmpty()
}

enum class RadioNotice { ADDED, REMOVED, NO_PLAYER, FAILED }

data class RadioUiState(
    val favorites: List<Station> = emptyList(),
    /** Ids (provider uuid and library uri) of favorites, to paint hearts on browse rows. */
    val favoriteKeys: Set<String> = emptyList<String>().toSet(),
    val filter: RadioFilter = RadioFilter.Language("polish"),
    val filterLabel: String = "",
    val rows: List<RadioRow> = emptyList(),
    /** Browse list shows the whole MA radio library (A–Z, paged) instead of a Radio Browser filter. */
    val libraryMode: Boolean = true,
    val loadingMore: Boolean = false,
    val loading: Boolean = true,
    val failed: Boolean = false,
    /** Picker of one chip is open. */
    val picker: RadioFilterKind? = null,
    val options: Map<RadioFilterKind, List<RadioFilterOption>> = emptyMap(),
    val searching: Boolean = false,
    val query: String = "",
    val symbols: Boolean = false,
    val searchRows: List<RadioRow> = emptyList(),
    /** Search shows only Polish stations (default: all, Polish first). */
    val polishOnly: Boolean = false,
    val searchLoading: Boolean = false,
    val searchFailed: Boolean = false,
    /** Context menu of a favorite tile. */
    val menuStation: Station? = null,
    /** Favorite being moved with the arrows (its uri). */
    val movingUri: String? = null,
    val notice: RadioNotice? = null,
) {
    fun isFavorite(row: RadioRow) = row.item.itemId in favoriteKeys || row.uri in favoriteKeys
}
