package io.github.riberrr.spinroom.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.AuthRepository
import io.github.riberrr.spinroom.core.data.BrowseRepository
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SearchRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.AuthApi
import io.github.riberrr.spinroom.core.maapi.BrowseApi
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MaClient
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

/** Wires `:core:ma-api` and the repositories. Later sessions add their own modules/files instead of editing this one. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides @Singleton fun settingsStore(store: DataStore<Preferences>) = SettingsStore(store)
    @Provides @Singleton fun maClient(http: OkHttpClient, @AppScope scope: CoroutineScope) = MaClient(http, scope)
    @Provides @Singleton fun connectionManager(s: SettingsStore, c: MaClient, @AppScope scope: CoroutineScope) = ConnectionManager(s, c, scope)

    @Provides @Singleton fun libraryApi(c: MaClient) = LibraryApi(c)
    @Provides @Singleton fun browseApi(c: MaClient) = BrowseApi(c)
    @Provides @Singleton fun playerQueueApi(c: MaClient) = PlayerQueueApi(c)
    @Provides @Singleton fun authApi(http: OkHttpClient) = AuthApi(http)

    @Provides @Singleton fun libraryRepository(api: LibraryApi) = LibraryRepository(api)
    @Provides @Singleton fun browseRepository(api: BrowseApi) = BrowseRepository(api)
    @Provides @Singleton fun searchRepository(api: BrowseApi) = SearchRepository(api)
    @Provides @Singleton fun playerRepository(api: PlayerQueueApi, c: MaClient) = PlayerRepository(api, c)
    @Provides @Singleton
    fun authRepository(api: AuthApi, s: SettingsStore, @AppScope scope: CoroutineScope) = AuthRepository(api, s, scope)
}
