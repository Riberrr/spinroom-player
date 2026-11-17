package io.github.riberrr.spinroom.feature.radio

import io.github.riberrr.spinroom.core.maapi.MediaItem

/** Search hit with what the app knows about it. */
data class SearchHit(val item: MediaItem, val favorite: Boolean, val polish: Boolean)

private fun nameKey(item: MediaItem) = item.name.trim().lowercase().replace(Regex("""\s+"""), " ")

/**
 * Cleans a raw station search: drops duplicates (same name, or same uri), then orders favorites first,
 * Polish stations next, the rest last. Within a group the server's order is kept.
 */
fun orderSearchResults(items: List<MediaItem>, isFavorite: (MediaItem) -> Boolean, polishIds: Set<String>): List<SearchHit> {
    val seenNames = HashSet<String>()
    val seenUris = HashSet<String>()
    val unique = items.filter { item ->
        val uriNew = item.uri.isNullOrBlank() || seenUris.add(item.uri!!)
        val nameNew = seenNames.add(nameKey(item))
        uriNew && nameNew
    }
    return unique
        .map { SearchHit(it, isFavorite(it), it.itemId in polishIds || it.uri in polishIds) }
        .sortedBy { if (it.favorite) 0 else if (it.polish) 1 else 2 }
}

/** Ids (item id and uri) of the stations in a Radio Browser list, to recognise them in search results. */
fun idsOf(items: List<MediaItem>): Set<String> = items.flatMap { listOf(it.itemId, it.uri.orEmpty()) }.filter { it.isNotBlank() }.toSet()
