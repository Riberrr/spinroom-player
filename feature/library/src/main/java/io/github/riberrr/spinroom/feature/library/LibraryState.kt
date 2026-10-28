package io.github.riberrr.spinroom.feature.library

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

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.Albums,
    /** Item count per tab, filled in lazily (shown in the label of the selected tab). */
    val counts: Map<LibraryTab, Int> = emptyMap(),
) {
    val count: Int? get() = counts[tab]
}

/** Letters of the quick-jump rail; `#` = start of the list (non-letters sort first). */
val JumpLetters = listOf("#", "A", "D", "G", "K", "N", "R", "T", "Z")
