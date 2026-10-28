package io.github.riberrr.spinroom.feature.search

import io.github.riberrr.spinroom.core.maapi.MediaItem

enum class SearchFilter { ALL, ARTISTS, ALBUMS, TRACKS, RADIO }

enum class ResultKind { ARTIST, ALBUM, TRACK, PLAYLIST, RADIO }

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
) {
    enum class Notice { NO_VOICE, NO_PLAYER, PLAY_FAILED }

    private fun visible(wanted: SearchFilter, rows: List<SearchRow>) =
        if (filter == SearchFilter.ALL || filter == wanted) rows else emptyList()

    val shownArtists get() = visible(SearchFilter.ARTISTS, artists)
    val shownAlbums get() = visible(SearchFilter.ALBUMS, albums)
    val shownPlaylists get() = visible(SearchFilter.ALBUMS, playlists)
    val shownTracks get() = visible(SearchFilter.TRACKS, tracks)
    val shownRadio get() = visible(SearchFilter.RADIO, radio)

    /** Highlighted hit for the "all" tab: first artist, else album, else track. */
    val best: SearchRow? get() =
        if (filter != SearchFilter.ALL) null else artists.firstOrNull() ?: albums.firstOrNull() ?: tracks.firstOrNull()

    val hasResults get() = artists.isNotEmpty() || albums.isNotEmpty() || tracks.isNotEmpty() || playlists.isNotEmpty() || radio.isNotEmpty()
    val hasShown get() = best != null || shownArtists.isNotEmpty() || shownAlbums.isNotEmpty() || shownPlaylists.isNotEmpty() ||
        shownTracks.isNotEmpty() || shownRadio.isNotEmpty()
    val isIdle get() = query.isBlank()
}
