package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.BrowseApi
import io.github.riberrr.spinroom.core.maapi.SearchResults

class SearchRepository(private val api: BrowseApi) {
    /** Blank queries return empty results without hitting the server. */
    suspend fun search(query: String, limit: Int = 10, libraryOnly: Boolean = false): SearchResults =
        if (query.isBlank()) SearchResults() else api.search(query.trim(), limit, libraryOnly)
}
