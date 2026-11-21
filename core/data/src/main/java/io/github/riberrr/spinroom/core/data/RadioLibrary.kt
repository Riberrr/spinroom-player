package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaTypes

/** One page of the MA radio library. [end] is true when the server has nothing after it. */
data class RadioLibraryPage(val items: List<MediaItem>, val nextOffset: Int, val end: Boolean)

/** All radio stations saved in Music Assistant (favorites or not), A–Z, read in pages. */
class RadioLibraryRepository(private val library: LibraryApi) {

    suspend fun page(offset: Int, limit: Int = PAGE_SIZE): RadioLibraryPage {
        val items = library.items(MediaTypes.RADIOS, limit = limit, offset = offset, orderBy = "name")
        return RadioLibraryPage(items, offset + items.size, items.size < limit)
    }

    companion object {
        const val PAGE_SIZE = 100

        /** Appends [next] to [current] skipping stations already present (by uri or item id). */
        fun append(current: List<MediaItem>, next: List<MediaItem>): List<MediaItem> {
            val seen = current.mapTo(HashSet()) { it.uri ?: it.itemId }
            return current + next.filter { seen.add(it.uri ?: it.itemId) }
        }
    }
}
