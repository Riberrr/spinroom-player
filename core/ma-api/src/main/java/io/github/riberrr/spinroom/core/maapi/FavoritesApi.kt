package io.github.riberrr.spinroom.core.maapi

/** Parts of an MA URI `provider://media_type/item_id`. */
data class ParsedUri(val provider: String, val mediaType: String, val itemId: String)

fun parseMediaUri(uri: String): ParsedUri? {
    val (provider, rest) = uri.split("://", limit = 2).takeIf { it.size == 2 } ?: return null
    val (type, id) = rest.split('/', limit = 2).takeIf { it.size == 2 } ?: return null
    return ParsedUri(provider, type, id)
}

/** favorites commands (`music/favorites/add_item`, `remove_item`). Removal needs the *library* item id, so items from other providers are resolved first. */
class FavoritesApi(private val library: LibraryApi) {

    suspend fun add(uri: String) = library.addFavorite(uri)

    suspend fun remove(mediaType: String, libraryItemId: String) = library.removeFavorite(mediaType, libraryItemId)

    /** Full item for [uri] (carries `favorite` and, for library items, the library id). */
    suspend fun resolve(uri: String): MediaItem = library.itemByUri(uri)

    /** Sets the favorite flag of [uri]. Returns false if the item could not be resolved to a library id (only on removal). */
    suspend fun set(uri: String, favorite: Boolean): Boolean {
        if (favorite) {
            add(uri)
            return true
        }
        val item = resolve(uri)
        val libraryId = if (item.provider == "library") item.itemId else parseMediaUri(uri)?.takeIf { it.provider == "library" }?.itemId
        val id = libraryId ?: item.itemId.takeIf { it.isNotBlank() } ?: return false
        remove(item.mediaType.ifBlank { parseMediaUri(uri)?.mediaType ?: return false }, id)
        return true
    }
}
