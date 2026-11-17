package io.github.riberrr.spinroom.feature.library

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.FilteredLibrary
import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.PlayItem
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.data.ViewModePrefs
import io.github.riberrr.spinroom.core.maapi.FavoritesApi
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.parseMediaUri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** What the library screen needs from `:core:data`. Fake it in tests. */
interface LibraryBackend {
    val serverAddress: Flow<String>

    /** Connection to the Music Assistant server (drives the offline / connecting states). */
    val connection: Flow<MaConnectionState>

    /**
     * Paged items of [tab] sorted by name. Without a [filter] placeholders are enabled so the grid can jump to any index;
     * with one the list is plain (the total is unknown).
     */
    fun pager(tab: LibraryTab, filter: LibraryFilter = LibraryFilter()): Flow<PagingData<MediaItem>>
    suspend fun count(tab: LibraryTab): Int

    /** Single item at [offset] of the name-sorted list (used to find where a letter starts), null past the end. */
    suspend fun itemAt(tab: LibraryTab, offset: Int): MediaItem?

    /** Grid (false) or list with preview (true), remembered. */
    val listMode: Flow<Boolean> get() = flowOf(false)
    suspend fun setListMode(list: Boolean) {}

    /** Options of the filter panel. */
    suspend fun genres(): List<MediaItem> = emptyList()
    suspend fun sources(): List<SourceOption> = emptyList()

    /** Tracks of an album or playlist [row] for the preview (empty for other types). */
    suspend fun tracks(row: LibraryRow): List<MediaItem> = emptyList()

    suspend fun play(uri: String, shuffle: Boolean): Boolean = false
    suspend fun setFavorite(uri: String, favorite: Boolean) {}
}

/** Page source with a known [total], so Paging can show placeholders and the grid can be scrolled to any index at once. */
internal class CountedLibrarySource(
    private val total: Int,
    private val pageSize: Int,
    private val fetch: suspend (limit: Int, offset: Int) -> List<MediaItem>,
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        return try {
            val items = fetch(limit, offset)
            val end = offset + items.size
            val last = items.size < limit || end >= total
            LoadResult.Page(
                data = items,
                prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0),
                nextKey = if (last) null else end,
                itemsBefore = offset,
                itemsAfter = if (last) 0 else total - end,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? =
        state.anchorPosition?.let { (it - state.config.initialLoadSize / 2).coerceAtLeast(0) }
}

@Singleton
class MaLibraryBackend @Inject constructor(
    private val api: LibraryApi,
    private val filtered: FilteredLibrary,
    private val player: PlayItem,
    private val favorites: FavoritesApi,
    private val viewMode: ViewModePrefs,
    settings: SettingsStore,
    connectionManager: ConnectionManager,
) : LibraryBackend {
    init { connectionManager.start() }

    override val serverAddress = settings.settings.map { it.serverAddress }
    override val connection: Flow<MaConnectionState> = connectionManager.state

    override fun pager(tab: LibraryTab, filter: LibraryFilter): Flow<PagingData<MediaItem>> =
        if (!filter.isEmpty) {
            filtered.pager(tab.mediaType, filter)
        } else flow {
            val total = api.count(tab.mediaType)
            emitAll(
                Pager(PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE * 2, prefetchDistance = 24, enablePlaceholders = true)) {
                    CountedLibrarySource(total, PAGE_SIZE) { limit, offset -> api.items(tab.mediaType, limit, offset, orderBy = "name") }
                }.flow,
            )
        }

    override suspend fun count(tab: LibraryTab) = api.count(tab.mediaType)

    override suspend fun itemAt(tab: LibraryTab, offset: Int): MediaItem? =
        api.items(tab.mediaType, 1, offset, orderBy = "name").firstOrNull()

    override val listMode: Flow<Boolean> = viewMode.libraryList
    override suspend fun setListMode(list: Boolean) = viewMode.setLibraryList(list)
    override suspend fun genres() = filtered.genres()
    override suspend fun sources() = filtered.sources()
    override suspend fun play(uri: String, shuffle: Boolean) = player.play(uri, shuffle)
    override suspend fun setFavorite(uri: String, favorite: Boolean) { favorites.set(uri, favorite) }

    override suspend fun tracks(row: LibraryRow): List<MediaItem> {
        val parsed = parseMediaUri(row.uri) ?: return emptyList()
        return when (row.mediaType) {
            "album" -> api.albumTracks(parsed.itemId, parsed.provider)
            "playlist" -> api.playlistTracks(parsed.itemId, parsed.provider)
            else -> emptyList()
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds abstract fun backend(impl: MaLibraryBackend): LibraryBackend
}
