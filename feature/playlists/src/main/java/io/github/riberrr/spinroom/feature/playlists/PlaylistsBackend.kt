package io.github.riberrr.spinroom.feature.playlists

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.FilteredLibrary
import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.PlayItem
import io.github.riberrr.spinroom.core.data.PlaylistSort
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the playlists screen needs from `:core:data`. Fake it in tests. */
interface PlaylistsBackend {
    val serverAddress: Flow<String>
    val connection: Flow<MaConnectionState>

    /** Playlists in [sort] order, optionally only from [provider] (a provider domain). */
    fun pager(sort: PlaylistSort, provider: String?): Flow<PagingData<MediaItem>>
    suspend fun sources(): List<SourceOption>
    suspend fun play(uri: String, shuffle: Boolean): Boolean
}

@Singleton
class MaPlaylistsBackend @Inject constructor(
    private val filtered: FilteredLibrary,
    private val player: PlayItem,
    settings: SettingsStore,
    connectionManager: ConnectionManager,
) : PlaylistsBackend {
    init { connectionManager.start() }

    override val serverAddress = settings.settings.map { it.serverAddress }
    override val connection: Flow<MaConnectionState> = connectionManager.state

    override fun pager(sort: PlaylistSort, provider: String?): Flow<PagingData<MediaItem>> =
        filtered.pager(MediaTypes.PLAYLISTS, LibraryFilter(provider = provider), orderBy = sort.orderBy)

    override suspend fun sources() = filtered.sources()
    override suspend fun play(uri: String, shuffle: Boolean) = player.play(uri, shuffle)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaylistsModule {
    @Binds abstract fun backend(impl: MaPlaylistsBackend): PlaylistsBackend
}
