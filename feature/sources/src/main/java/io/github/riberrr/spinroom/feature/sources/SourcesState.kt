package io.github.riberrr.spinroom.feature.sources

import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem

/** One line in either list: a provider on the left, a folder / station / track on the right. */
data class SourceRow(
    val item: MediaItem,
    val subtitle: String,
    val coverUrl: String?,
    val favorite: Boolean,
) {
    val key: String get() = item.uri ?: item.path ?: "${item.provider}/${item.itemId}"
    val name: String get() = item.name

    /** Folders (and anything not playable) open; the rest plays. */
    val isFolder: Boolean get() = item.mediaType == "folder" || (item.isPlayable != true && item.path != null)
    val playUri: String? get() = if (isFolder) null else item.uri
    val canFavorite: Boolean get() = !isFolder && item.uri != null

    val initials: String
        get() = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }
}

/** One step of the breadcrumb trail; the first one is the provider. */
data class Crumb(val name: String, val path: String)

data class SourcesUiState(
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    val hasPlayer: Boolean = false,
    val loadingProviders: Boolean = true,
    val providersFailed: Boolean = false,
    val providers: List<SourceRow> = emptyList(),
    val selectedProvider: Int = 0,
    val trail: List<Crumb> = emptyList(),
    val loadingItems: Boolean = false,
    val itemsFailed: Boolean = false,
    val items: List<SourceRow> = emptyList(),
    /** Name of the item last sent to the player, shown as a hint under the list. */
    val playedName: String? = null,
    val playFailed: Boolean = false,
    /** User pressed play but no player is chosen yet. */
    val noPlayer: Boolean = false,
) {
    val canGoUp: Boolean get() = trail.size > 1
}
