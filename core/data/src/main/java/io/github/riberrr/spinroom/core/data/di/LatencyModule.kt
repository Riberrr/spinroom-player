package io.github.riberrr.spinroom.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.riberrr.spinroom.core.data.LatencyStore
import javax.inject.Singleton

/** S14c: latency calibration lives in the shared `settings` DataStore under its own key. */
@Module
@InstallIn(SingletonComponent::class)
object LatencyModule {
    @Provides @Singleton fun latencyStore(store: DataStore<Preferences>) = LatencyStore(store)
}
