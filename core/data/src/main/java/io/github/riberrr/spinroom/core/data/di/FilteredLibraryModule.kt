package io.github.riberrr.spinroom.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.FilteredLibrary
import io.github.riberrr.spinroom.core.maapi.MaClient
import javax.inject.Singleton

/** SUI5: filtered library queries and the data of the filter panel. */
@Module
@InstallIn(SingletonComponent::class)
object FilteredLibraryModule {
    @Provides @Singleton fun filteredLibrary(c: MaClient) = FilteredLibrary(c)

    @Provides @Singleton
    fun viewModePrefs(store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) =
        io.github.riberrr.spinroom.core.data.ViewModePrefs(store)
}
