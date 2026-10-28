package io.github.riberrr.spinroom.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.maapi.FavoritesApi
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import javax.inject.Singleton

/** S14b: favorites commands. */
@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {
    @Provides @Singleton fun favoritesApi(library: LibraryApi) = FavoritesApi(library)
}
