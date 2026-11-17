package io.github.riberrr.spinroom.feature.home

import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** One square tile (album, playlist, …). [uri] null = nothing to open. */
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
    val coverUrl: String? = null,
)

/** Rows of the Start screen. */
enum class HomeRow { Stations, Playlists, Recent }

/** Fixed order until the appearance preferences (SUI8a) provide one. TODO(SUI8a): read the order from the preferences. */
val DEFAULT_ROW_ORDER: List<HomeRow> = listOf(HomeRow.Stations, HomeRow.Playlists, HomeRow.Recent)

data class HomeUiState(
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val recent: List<HomeTile> = emptyList(),
    val playlists: List<HomeTile> = emptyList(),
    val stations: List<StationTileState> = emptyList(),
    val rowOrder: List<HomeRow> = DEFAULT_ROW_ORDER,
    val hasPlayer: Boolean = false,
    /** Name of this TV as a player; shown in the status chip. */
    val playerName: String? = null,
    /** True after the first load with at least one successful call: rows (empty ones with a hint) replace the spinner. */
    val loaded: Boolean = false,
    /** No server address/token saved: offer Settings instead of a spinner. */
    val notConfigured: Boolean = false,
) {
    val isConnected: Boolean get() = connection is MaConnectionState.Connected

    /** Stations and playlists are always shown (empty = a hint to add favorites); "recent" only when it has items. */
    val visibleRows: List<HomeRow>
        get() = rowOrder.distinct().filter { it != HomeRow.Recent || recent.isNotEmpty() }
}
