package io.github.riberrr.spinroom.feature.nowplaying

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.QueueItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the now-playing ViewModel needs from the data layer. A small interface so the ViewModel can be
 * tested with a fake instead of a WebSocket; [MaPlayerBackend] is the real one.
 */
interface PlayerBackend {
    val connection: Flow<MaConnectionState>

    /** Id of the player/queue this app controls; empty = none chosen yet. */
    val selectedPlayerId: Flow<String>

    /** Server address as typed by the user, used for image URLs. */
    val serverAddress: Flow<String>

    fun queue(queueId: String): Flow<PlayerQueue>
    fun queueItems(queueId: String): Flow<List<QueueItem>>
    fun players(): Flow<List<Player>>

    suspend fun selectPlayer(playerId: String)
    suspend fun playPause(queueId: String)
    suspend fun next(queueId: String)
    suspend fun previous(queueId: String)
    suspend fun setShuffle(queueId: String, enabled: Boolean)
    suspend fun setRepeat(queueId: String, mode: String)
    suspend fun playIndex(queueId: String, index: Int)
    suspend fun setFavorite(item: MediaItem, favorite: Boolean)
}

@Singleton
class MaPlayerBackend @Inject constructor(
    private val players: PlayerRepository,
    private val settings: SettingsStore,
    private val library: LibraryRepository,
    connectionManager: ConnectionManager,
) : PlayerBackend {
    init { connectionManager.start() }

    override val connection = connectionManager.state
    override val selectedPlayerId = settings.settings.map { it.playerId }
    override val serverAddress = settings.settings.map { it.serverAddress }

    override fun queue(queueId: String) = players.queue(queueId)
    override fun queueItems(queueId: String) = players.queueItems(queueId)
    override fun players() = players.players()

    override suspend fun selectPlayer(playerId: String) = settings.setPlayerId(playerId)
    override suspend fun playPause(queueId: String) = players.api.playPause(queueId)
    override suspend fun next(queueId: String) = players.api.next(queueId)
    override suspend fun previous(queueId: String) = players.api.previous(queueId)
    override suspend fun setShuffle(queueId: String, enabled: Boolean) = players.api.shuffle(queueId, enabled)
    override suspend fun setRepeat(queueId: String, mode: String) = players.api.repeat(queueId, mode)
    override suspend fun playIndex(queueId: String, index: Int) = players.api.playIndex(queueId, index)

    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (favorite) library.addFavorite(item) else library.removeFavorite(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NowPlayingModule {
    @Binds abstract fun playerBackend(impl: MaPlayerBackend): PlayerBackend
}
