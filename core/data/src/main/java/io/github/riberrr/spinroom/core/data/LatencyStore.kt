package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Local audio latency calibration (S14c), −500…+500 ms, on top of the Sendspin `output_delay_ms` that MA sets.
 * Positive = this TV plays earlier (slow soundbar/HDMI chain), negative = later.
 */
class LatencyStore(private val store: DataStore<Preferences>) {
    val calibrationMs: Flow<Int> = store.data.map { (it[KEY] ?: 0).coerceIn(MIN_MS, MAX_MS) }.distinctUntilChanged()

    suspend fun setCalibrationMs(value: Int) {
        store.edit { it[KEY] = value.coerceIn(MIN_MS, MAX_MS) }
    }

    companion object {
        const val MIN_MS = -500
        const val MAX_MS = 500
        private val KEY = intPreferencesKey("audio_calibration_ms")
    }
}
