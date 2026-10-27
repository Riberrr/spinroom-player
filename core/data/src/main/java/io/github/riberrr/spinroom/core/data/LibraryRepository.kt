package io.github.riberrr.spinroom.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MediaItem
import kotlinx.coroutines.flow.Flow

/** Library lists (paged) and item details. */
class LibraryRepository(private val api: LibraryApi) {

    /** Paged list of `MediaTypes.*` items; [search]/[favorite]/[orderBy] map straight to `library_items`. */
    fun pager(
        type: String,
        favorite: Boolean? = null,
        search: String? = null,
        orderBy: String? = "name",
        pageSize: Int = PAGE_SIZE,
    ): Flow<PagingData<MediaItem>> = Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) {
        LibraryPagingSource(pageSize) { limit, offset -> api.items(type, limit, offset, favorite, search, orderBy) }
    }.flow

    suspend fun count(type: String, favoriteOnly: Boolean = false) = api.count(type, favoriteOnly)
    suspend fun album(item: MediaItem) = api.album(item.itemId, item.provider)
    suspend fun albumTracks(item: MediaItem) = api.albumTracks(item.itemId, item.provider)
    suspend fun artist(item: MediaItem) = api.artist(item.itemId, item.provider)
    suspend fun artistAlbums(item: MediaItem) = api.artistAlbums(item.itemId, item.provider)
    suspend fun artistTopTracks(item: MediaItem) = api.artistTopTracks(item.itemId, item.provider)
    suspend fun playlist(item: MediaItem) = api.playlist(item.itemId, item.provider)
    suspend fun playlistTracks(item: MediaItem) = api.playlistTracks(item.itemId, item.provider)
    suspend fun addFavorite(item: MediaItem) { item.uri?.let { api.addFavorite(it) } }
    suspend fun removeFavorite(item: MediaItem) = api.removeFavorite(item.mediaType, item.itemId)

    companion object {
        const val PAGE_SIZE = 50
    }
}

/** MA has no end marker: a page shorter than the requested limit is the last one. */
class LibraryPagingSource(
    private val pageSize: Int,
    private val fetch: suspend (limit: Int, offset: Int) -> List<MediaItem>,
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(pageSize)
        return try {
            val items = fetch(limit, offset)
            LoadResult.Page(
                data = items,
                prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0),
                nextKey = if (items.size < limit) null else offset + items.size,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? = null
}
