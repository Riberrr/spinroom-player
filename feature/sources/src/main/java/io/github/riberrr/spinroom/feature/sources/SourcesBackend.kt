package io.github.riberrr.spinroom.feature.sources

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.BrowseRepository
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the sources ViewModel needs from the data layer; [MaSourcesBackend] is the real one, tests use a fake. */
interface SourcesBackend {
    val connection: Flow<MaConnectionState>

    /** Id of the player/queue this app controls; empty = none chosen yet. */
    val playerId: Flow<String>
    val serverAddress: Flow<String>

    /** [path] null = list of providers. */
    suspend fun browse(path: String?): List<MediaItem>
    suspend fun play(queueId: String, uri: String)
    suspend fun setFavorite(item: MediaItem, favorite: Boolean)
}

@Singleton
class MaSourcesBackend @Inject constructor(
    private val browse: BrowseRepository,
    private val players: PlayerRepository,
    private val library: LibraryRepository,
    settings: SettingsStore,
    connectionManager: ConnectionManager,
) : SourcesBackend {
    init { connectionManager.start() }

    override val connection = connectionManager.state
    override val playerId = settings.settings.map { it.playerId }
    override val serverAddress = settings.settings.map { it.serverAddress }

    override suspend fun browse(path: String?) = browse.browse(path)
    override suspend fun play(queueId: String, uri: String) = players.api.playMedia(queueId, listOf(uri), option = "replace")

    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (favorite) library.addFavorite(item) else library.removeFavorite(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SourcesModule {
    @Binds abstract fun sourcesBackend(impl: MaSourcesBackend): SourcesBackend
}
