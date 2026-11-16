package io.github.riberrr.spinroom.feature.library

import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaTypes

enum class LibraryTab(val mediaType: String) {
    Artists(MediaTypes.ARTISTS),
    Albums(MediaTypes.ALBUMS),
    Tracks(MediaTypes.TRACKS),
    Playlists(MediaTypes.PLAYLISTS),
}

/** One grid cell or list row, already mapped from the MA model. [coverUrl] is sized for the grid thumbnail. */
data class LibraryRow(
    val uri: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val year: Int? = null,
    val favorite: Boolean = false,
    /** Singular MA media type: `album`, `artist`, `track`, `playlist`. */
    val mediaType: String = "",
)

/** Where focus last was inside the library screen, so coming back (Back from Now Playing) restores it. */
sealed interface LibraryFocus {
    data object Tab : LibraryFocus
    data class Item(val index: Int) : LibraryFocus
}

data class FilterOption(val id: String, val name: String)

data class PreviewTrack(val number: Int, val title: String, val durationSec: Int?)

/** Right-hand preview of the selected row (list mode). Tracks arrive a moment after the selection settles. */
data class PreviewState(
    val row: LibraryRow? = null,
    val tracks: List<PreviewTrack> = emptyList(),
    val trackCount: Int = 0,
    val totalMinutes: Int? = null,
    val loading: Boolean = false,
)

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.Albums,
    /** Item count per tab, filled in lazily (shown in the label of the selected tab). */
    val counts: Map<LibraryTab, Int> = emptyMap(),
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val focus: LibraryFocus = LibraryFocus.Tab,
    val filter: LibraryFilter = LibraryFilter(),
    val listMode: Boolean = false,
    val genres: List<FilterOption> = emptyList(),
    val sources: List<SourceOption> = emptyList(),
    val preview: PreviewState = PreviewState(),
) {
    val count: Int? get() = counts[tab]
}

/** Letters of the quick-jump rail; `#` = start of the list (non-letters sort first). */
val JumpLetters = listOf("#", "A", "D", "G", "K", "N", "R", "T", "Z")
