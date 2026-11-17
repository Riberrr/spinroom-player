package io.github.riberrr.spinroom.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.FavoriteStationsOrder
import io.github.riberrr.spinroom.core.data.FavoriteStationsRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.BrowseApi
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MaClient
import io.github.riberrr.spinroom.core.maapi.RadioApi
import javax.inject.Singleton

/** SUI4: radio browsing and the user's favorite-station order. */
@Module
@InstallIn(SingletonComponent::class)
object RadioModule {
    @Provides @Singleton fun radioApi(c: MaClient, browse: BrowseApi) = RadioApi(c, browse)
    @Provides @Singleton fun stationsOrder(store: DataStore<Preferences>) = FavoriteStationsOrder(store)

    @Provides @Singleton
    fun favoriteStations(
        library: LibraryApi,
        radio: RadioApi,
        order: FavoriteStationsOrder,
        settings: SettingsStore,
        store: DataStore<Preferences>,
    ) = FavoriteStationsRepository(library, radio, order, settings, store)
}
