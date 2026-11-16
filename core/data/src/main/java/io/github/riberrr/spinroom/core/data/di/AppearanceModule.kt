package io.github.riberrr.spinroom.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.AppearanceSettings
import javax.inject.Singleton

/** SUI1: appearance preferences live in the shared `settings` DataStore under their own keys. */
@Module
@InstallIn(SingletonComponent::class)
object AppearanceModule {
    @Provides @Singleton fun appearanceSettings(store: DataStore<Preferences>) = AppearanceSettings(store)
}
