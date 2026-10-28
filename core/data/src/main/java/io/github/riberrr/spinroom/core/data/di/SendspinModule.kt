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
import io.github.riberrr.spinroom.core.data.SendspinCredentialStore
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SendspinSecrets

/** Loads [SendspinCredentialStore] once per process (the playback service asks for it at start). */
@Singleton
class SendspinCredentials @Inject constructor(
    @SendspinSecrets private val store: DataStore<Preferences>,
    @AppScope private val scope: CoroutineScope,
) {
    private val lock = Mutex()
    private var loaded: SendspinCredentialStore? = null

    suspend fun get(): SendspinCredentialStore = lock.withLock {
        loaded ?: SendspinCredentialStore.load(store, scope).also { loaded = it }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SendspinModule {
    @Provides @Singleton @SendspinSecrets
    fun sendspinDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("sendspin_credentials") }
}
