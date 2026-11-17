package io.github.riberrr.spinroom.feature.search

import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.MediaItem

/** Category tabs, in the order the mockup shows them. */
enum class SearchFilter { ALL, RADIO, PLAYLISTS, ALBUMS, ARTISTS, TRACKS }

enum class ResultKind { ARTIST, ALBUM, TRACK, PLAYLIST, RADIO }

/** Bottom message after adding/removing a favorite; "Cofnij" reverts it. */
data class FavoriteToast(val uri: String, val title: String, val added: Boolean)

/** One search hit as the UI shows it. [item] is kept for navigation/playback. */
data class SearchRow(
    val item: MediaItem,
    val kind: ResultKind,
    val title: String,
    val subtitle: String,
    val source: String,
    val imageUrl: String?,
) {
    val uri: String get() = item.uri ?: "${item.provider}://${item.mediaType}/${item.itemId}"
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val symbols: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val artists: List<SearchRow> = emptyList(),
    val albums: List<SearchRow> = emptyList(),
    val tracks: List<SearchRow> = emptyList(),
    val playlists: List<SearchRow> = emptyList(),
    val radio: List<SearchRow> = emptyList(),
    /** Shown once after an action that needs the user's attention (e.g. no voice search on this TV). */
    val notice: Notice? = null,
    /** Active "Filtry" criteria (genre is not used: search results carry no genre). */
    val filters: LibraryFilter = LibraryFilter(),
    val panelOpen: Boolean = false,
    /** Favorite flags set in this screen; they win over `item.favorite` from the (older) search response. */
    val favorites: Map<String, Boolean> = emptyMap(),
    val toast: FavoriteToast? = null,
) {
    enum class Notice { NO_VOICE, NO_PLAYER, PLAY_FAILED, FAVORITE_FAILED }

    fun isFavorite(row: SearchRow): Boolean = favorites[row.uri] ?: row.item.favorite

    private fun passes(row: SearchRow): Boolean =
        (!filters.favoriteOnly || isFavorite(row)) &&
            (filters.provider == null || row.item.provider.substringBefore("--") == filters.provider) &&
            filters.matches(row.item)

    private fun visible(wanted: SearchFilter, rows: List<SearchRow>) =
        if (filter == SearchFilter.ALL || filter == wanted) rows.filter(::passes) else emptyList()

    /** Sources present in the current results, for the filter panel. */
    val sources: List<SourceOption> get() =
        (artists + albums + tracks + playlists + radio)
            .map { it.item.provider.substringBefore("--") to it.source }.distinct().map { SourceOption(it.first, it.second) }

    val shownArtists get() = visible(SearchFilter.ARTISTS, artists)
    val shownAlbums get() = visible(SearchFilter.ALBUMS, albums)
    val shownPlaylists get() = visible(SearchFilter.PLAYLISTS, playlists)
    val shownTracks get() = visible(SearchFilter.TRACKS, tracks)
    val shownRadio get() = visible(SearchFilter.RADIO, radio)

    val hasResults get() = artists.isNotEmpty() || albums.isNotEmpty() || tracks.isNotEmpty() || playlists.isNotEmpty() || radio.isNotEmpty()
    val hasShown get() = shownArtists.isNotEmpty() || shownAlbums.isNotEmpty() || shownPlaylists.isNotEmpty() ||
        shownTracks.isNotEmpty() || shownRadio.isNotEmpty()
    val isIdle get() = query.isBlank()
}
