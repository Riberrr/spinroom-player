package io.github.riberrr.spinroom.feature.search

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SearchRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.FavoritesApi
import io.github.riberrr.spinroom.core.maapi.SearchResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What the search ViewModel needs from the data layer (fakeable in tests). */
interface SearchBackend {
    val serverAddress: Flow<String>

    suspend fun search(query: String): SearchResults

    /** Starts [uri] on the selected player; false when no player is chosen yet. */
    suspend fun play(uri: String): Boolean

    /** Sets or clears the MA favorite flag of [uri]; false when it could not be resolved. */
    suspend fun setFavorite(uri: String, favorite: Boolean): Boolean

    /** Real favorite flag of [uri] (search hits from providers do not carry it); null when unknown. */
    suspend fun isFavorite(uri: String): Boolean?
}

@Singleton
class MaSearchBackend @Inject constructor(
    private val search: SearchRepository,
    private val players: PlayerRepository,
    private val settings: SettingsStore,
    private val favorites: FavoritesApi,
    connectionManager: ConnectionManager,
) : SearchBackend {
    init { connectionManager.start() }

    override val serverAddress = settings.settings.map { it.serverAddress }

    override suspend fun search(query: String) = search.search(query, limit = SEARCH_LIMIT)

    override suspend fun play(uri: String): Boolean {
        val playerId = settings.settings.first().playerId
        if (playerId.isBlank()) return false
        players.api.playMedia(playerId, listOf(uri))
        return true
    }

    override suspend fun setFavorite(uri: String, favorite: Boolean) = favorites.set(uri, favorite)

    override suspend fun isFavorite(uri: String): Boolean? = runCatching { favorites.resolve(uri).favorite }.getOrNull()

    private companion object {
        const val SEARCH_LIMIT = 12
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchBackendModule {
    @Binds abstract fun backend(impl: MaSearchBackend): SearchBackend
}
