package io.github.riberrr.spinroom.core.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.riberrr.spinroom.core.maapi.MediaItem

/**
 * Library filter (panel "Filtry"). Genre, source and favorites go to `library_items` on the server;
 * [decade] (e.g. 1990) has no server argument, so it is applied to `year` on the client.
 */
data class LibraryFilter(
    /** Genre item id (`music/genres/library_items`). */
    val genreId: String? = null,
    val genreName: String? = null,
    /** First year of the decade, e.g. 1990. */
    val decade: Int? = null,
    /** Provider domain, e.g. `spotify`. */
    val provider: String? = null,
    val favoriteOnly: Boolean = false,
) {
    val isEmpty: Boolean get() = genreId == null && decade == null && provider == null && !favoriteOnly

    /** Number of active criteria. */
    val activeCount: Int get() = listOf(genreId != null, decade != null, provider != null, favoriteOnly).count { it }

    /** True when the server does all the filtering, so paging can use exact totals. */
    val serverSideOnly: Boolean get() = decade == null

    /** Client-side part: items without a year never match a decade. */
    fun matches(item: MediaItem): Boolean = decade == null || item.year?.let { it in decade until decade + 10 } == true

    companion object {
        /** Decades offered in the panel, newest first. */
        val Decades: List<Int> = listOf(2020, 2010, 2000, 1990, 1980, 1970, 1960)
    }
}

/** Playlist ordering in "Playlisty": the MA `order_by` value for each choice. */
enum class PlaylistSort(val orderBy: String) {
    /** Default: most recently played first. */
    Recent("last_played_desc"),
    Alphabetical("name"),
}

/**
 * Pages through [fetch] (server pages of [pageSize]) keeping only items accepted by [accept], until a page of [loadSize]
 * accepted items is gathered or the server runs out. The key is the *server* offset, so an exhausted source ends the list.
 */
class FilteringPagingSource(
    private val pageSize: Int,
    private val accept: (MediaItem) -> Boolean,
    private val fetch: suspend (limit: Int, offset: Int) -> List<MediaItem>,
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        var offset = params.key ?: 0
        val wanted = params.loadSize.coerceAtLeast(pageSize)
        val out = ArrayList<MediaItem>()
        return try {
            var exhausted = false
            while (out.size < wanted && !exhausted) {
                val page = fetch(pageSize, offset)
                offset += page.size
                out += page.filter(accept)
                if (page.size < pageSize) exhausted = true
            }
            LoadResult.Page(data = out, prevKey = null, nextKey = if (exhausted) null else offset)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? = null
}
