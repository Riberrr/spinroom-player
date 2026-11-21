package io.github.riberrr.spinroom.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.RadioLibraryRepository
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import javax.inject.Singleton

/** SFix-UI15: the whole MA radio library for the "Wszystkie stacje" list. */
@Module
@InstallIn(SingletonComponent::class)
object RadioLibraryModule {
    @Provides @Singleton fun radioLibrary(library: LibraryApi) = RadioLibraryRepository(library)
}
