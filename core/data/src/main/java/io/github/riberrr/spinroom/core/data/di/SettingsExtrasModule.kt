package io.github.riberrr.spinroom.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.HomeLayoutSettings
import io.github.riberrr.spinroom.core.data.PlayerNameStore
import io.github.riberrr.spinroom.core.data.PlayerRenamer
import io.github.riberrr.spinroom.core.maapi.MaClient
import javax.inject.Singleton

/** SUI8a: TV player name and Start-row layout. */
@Module
@InstallIn(SingletonComponent::class)
object SettingsExtrasModule {
    @Provides @Singleton fun playerNameStore(store: DataStore<Preferences>) = PlayerNameStore(store)
    @Provides @Singleton fun playerRenamer(c: MaClient) = PlayerRenamer(c)
    @Provides @Singleton fun homeLayoutSettings(store: DataStore<Preferences>) = HomeLayoutSettings(store)
}
