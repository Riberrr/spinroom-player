package io.github.riberrr.spinroom.feature.home

import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** One square tile (album, playlist, mix, …). [uri] null = nothing to open. */
data class HomeTile(
    val key: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val uri: String?,
    val isPlaylist: Boolean = false,
)

/** A favorite radio station; clicking it plays right away. */
data class StationTileState(
    val key: String,
    val name: String,
    val provider: String,
    val initials: String,
    val uri: String?,
)

/** What is playing on the chosen player, for the large "continue listening" card. */
data class ContinueCard(
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val progress: Float,
    val position: Int,
    val total: Int,
)

data class HomeUiState(
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val continueCard: ContinueCard? = null,
    val recent: List<HomeTile> = emptyList(),
    val favorites: List<HomeTile> = emptyList(),
    val playlists: List<HomeTile> = emptyList(),
    val stations: List<StationTileState> = emptyList(),
    val recommended: List<HomeTile> = emptyList(),
    val hasPlayer: Boolean = false,
) {
    val isConnected: Boolean get() = connection is MaConnectionState.Connected
    val hasContent: Boolean
        get() = continueCard != null || recent.isNotEmpty() || favorites.isNotEmpty() || playlists.isNotEmpty() || stations.isNotEmpty() || recommended.isNotEmpty()

    /** Only when the YouTube Music provider is installed and returned recommendations. */
    val showYoutube: Boolean get() = recommended.isNotEmpty()
}
