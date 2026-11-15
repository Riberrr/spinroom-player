package io.github.riberrr.spinroom.feature.library

import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaTypes

enum class LibraryTab(val mediaType: String) {
    Artists(MediaTypes.ARTISTS),
    Albums(MediaTypes.ALBUMS),
    Tracks(MediaTypes.TRACKS),
    Playlists(MediaTypes.PLAYLISTS),
}

/** One grid cell, already mapped from the MA model. [coverUrl] is sized for the grid thumbnail. */
data class LibraryRow(
    val uri: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
)

/** Where focus last was inside the library screen, so coming back (Back from Now Playing) restores it. */
sealed interface LibraryFocus {
    data object Tab : LibraryFocus
    data class Item(val index: Int) : LibraryFocus
}

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.Albums,
    /** Item count per tab, filled in lazily (shown in the label of the selected tab). */
    val counts: Map<LibraryTab, Int> = emptyMap(),
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val focus: LibraryFocus = LibraryFocus.Tab,
) {
    val count: Int? get() = counts[tab]
}

/** Letters of the quick-jump rail; `#` = start of the list (non-letters sort first). */
val JumpLetters = listOf("#", "A", "D", "G", "K", "N", "R", "T", "Z")
