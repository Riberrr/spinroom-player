package io.github.riberrr.spinroom.feature.details

/** Kind of item shown on the details screen. */
enum class DetailsKind { Album, Artist, Playlist }

enum class DetailsStatus { Loading, Content, Error }

/** One track line. [uri] is what gets sent to the player. */
data class TrackRow(val uri: String, val number: Int, val title: String, val subtitle: String, val durationMs: Long)

/** One album tile in the artist view. */
data class AlbumRow(val uri: String, val title: String, val subtitle: String, val coverUrl: String?)

data class DetailsUiState(
    val status: DetailsStatus = DetailsStatus.Loading,
    val kind: DetailsKind = DetailsKind.Album,
    val title: String = "",
    /** Album artist / playlist owner; empty for artists. */
    val byline: String = "",
    val year: Int? = null,
    val trackCount: Int = 0,
    val totalMinutes: Int = 0,
    val coverUrl: String? = null,
    val favorite: Boolean = false,
    val tracks: List<TrackRow> = emptyList(),
    val albums: List<AlbumRow> = emptyList(),
    /** One-shot feedback after an action (play / queue). */
    val message: DetailsMessage? = null,
)

enum class DetailsMessage { Playing, Queued, NoPlayer, Failed }
