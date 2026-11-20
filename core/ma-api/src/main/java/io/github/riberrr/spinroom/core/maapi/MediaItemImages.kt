package io.github.riberrr.spinroom.core.maapi

/**
 * True when the item's only artwork is Music Assistant's own logo (system playlists such as "All favorited tracks"
 * use the `builtin` provider's `logo.png`). The project never shows the MA logo as a cover: callers fall back to their own placeholder.
 */
val MediaItem.hasOnlyMaLogo: Boolean
    get() = bestImage?.let { it.path == "logo.png" } == true

/** [bestImage] without the Music Assistant logo (null then). */
val MediaItem.coverImage: MediaItemImage? get() = if (hasOnlyMaLogo) null else bestImage
