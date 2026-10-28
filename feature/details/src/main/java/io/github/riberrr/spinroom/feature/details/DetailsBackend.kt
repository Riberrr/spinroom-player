package io.github.riberrr.spinroom.feature.details

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the details ViewModel needs from `:core:data`. Fake it in tests. */
interface DetailsBackend {
    val serverAddress: Flow<String>

    /** Id of the player/queue the app controls; empty = none chosen. */
    val selectedPlayerId: Flow<String>

    suspend fun item(uri: String): MediaItem

    /** Album tracks, playlist tracks or the artist's top tracks. */
    suspend fun tracks(item: MediaItem): List<MediaItem>
    suspend fun artistAlbums(item: MediaItem): List<MediaItem>

    /** [option]: play | add. */
    suspend fun play(playerId: String, uris: List<String>, option: String, shuffle: Boolean?, startItem: String?)
    suspend fun setFavorite(item: MediaItem, favorite: Boolean)
}

@Singleton
class MaDetailsBackend @Inject constructor(
    private val api: LibraryApi,
    private val library: LibraryRepository,
    private val players: PlayerRepository,
    settings: SettingsStore,
    connectionManager: ConnectionManager,
) : DetailsBackend {
    init { connectionManager.start() }

    override val serverAddress = settings.settings.map { it.serverAddress }
    override val selectedPlayerId = settings.settings.map { it.playerId }

    override suspend fun item(uri: String) = api.itemByUri(uri)

    override suspend fun tracks(item: MediaItem): List<MediaItem> = when (item.mediaType) {
        "album" -> library.albumTracks(item)
        "playlist" -> library.playlistTracks(item)
        else -> library.artistTopTracks(item)
    }

    override suspend fun artistAlbums(item: MediaItem) = library.artistAlbums(item)

    override suspend fun play(playerId: String, uris: List<String>, option: String, shuffle: Boolean?, startItem: String?) =
        players.api.playMedia(playerId, uris, option, startItem = startItem, shuffle = shuffle)

    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (favorite) library.addFavorite(item) else library.removeFavorite(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DetailsModule {
    @Binds abstract fun detailsBackend(impl: MaDetailsBackend): DetailsBackend
}
