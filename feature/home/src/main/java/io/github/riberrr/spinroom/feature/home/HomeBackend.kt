package io.github.riberrr.spinroom.feature.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.BrowseRepository
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.RecommendationFolder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the home ViewModel needs from the data layer; [MaHomeBackend] is the real one, tests use a fake. */
interface HomeBackend {
    val connection: Flow<MaConnectionState>

    /** Id of the player/queue this app controls; empty = none chosen yet. */
    val playerId: Flow<String>
    val serverAddress: Flow<String>

    fun queue(queueId: String): Flow<PlayerQueue>

    suspend fun recentlyPlayed(): List<MediaItem>
    suspend fun playlists(): List<MediaItem>
    suspend fun favoriteRadios(): List<MediaItem>

    /** Favorite albums, artists and playlists (not radio). */
    suspend fun favorites(): List<MediaItem> = emptyList()
    suspend fun recommendations(): List<RecommendationFolder>
    suspend fun play(queueId: String, uri: String)
}

@Singleton
class MaHomeBackend @Inject constructor(
    private val browse: BrowseRepository,
    private val players: PlayerRepository,
    private val library: LibraryApi,
    settings: SettingsStore,
    connectionManager: ConnectionManager,
) : HomeBackend {
    init { connectionManager.start() }

    override val connection = connectionManager.state
    override val playerId = settings.settings.map { it.playerId }
    override val serverAddress = settings.settings.map { it.serverAddress }

    override fun queue(queueId: String) = players.queue(queueId)

    override suspend fun recentlyPlayed() = browse.recentlyPlayed(ROW_LIMIT)
    override suspend fun playlists() = library.items(MediaTypes.PLAYLISTS, limit = ROW_LIMIT, orderBy = "name")
    override suspend fun favoriteRadios() = library.items(MediaTypes.RADIOS, limit = ROW_LIMIT, favorite = true, orderBy = "name")
    override suspend fun favorites(): List<MediaItem> = coroutineScope {
        listOf(MediaTypes.ALBUMS, MediaTypes.ARTISTS, MediaTypes.PLAYLISTS)
            .map { type -> async { runCatching { library.items(type, limit = ROW_LIMIT, favorite = true, orderBy = "name") }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
    }
    override suspend fun recommendations() = browse.recommendations()
    override suspend fun play(queueId: String, uri: String) = players.api.playMedia(queueId, listOf(uri), option = "replace")

    private companion object {
        const val ROW_LIMIT = 20
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds abstract fun homeBackend(impl: MaHomeBackend): HomeBackend
}
