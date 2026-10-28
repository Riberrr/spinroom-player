package io.github.riberrr.spinroom.feature.settings.latency

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.riberrr.spinroom.core.data.LatencyStore
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.di.AppScope
import java.net.Inet4Address
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SignalState { Idle, Starting, Playing, Failed }

data class LatencyUiState(
    val offsetMs: Int = 0,
    /** Null = the TV is not grouped with another speaker in MA (or MA is not connected). */
    val group: CalibrationGroup? = null,
    val signal: SignalState = SignalState.Idle,
)

/** Latency calibration: slider saved in DataStore (the playback service follows it live) + test signal via MA. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LatencyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val latency: LatencyStore,
    settings: SettingsStore,
    private val players: PlayerRepository,
    @AppScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(LatencyUiState())
    val state: StateFlow<LatencyUiState> = _state

    private var server: TestSignalServer? = null
    private var signalQueueId: String? = null
    private var target: Int? = null

    init {
        viewModelScope.launch {
            // Ignore echoes of older writes while the user is still pressing.
            latency.calibrationMs.collect { v -> if (target == null || target == v) { target = null; _state.update { it.copy(offsetMs = v) } } }
        }
        viewModelScope.launch {
            settings.settings.map { it.playerId }.distinctUntilChanged()
                .flatMapLatest { id -> if (id.isEmpty()) flowOf(null) else players.players().map { CalibrationGroups.find(it, id) } }
                .collect { g -> _state.update { it.copy(group = g) } }
        }
    }

    /** One remote press = [STEP_MS]; the value is clamped to −500…+500 by the store. */
    fun nudge(steps: Int) {
        val next = ((target ?: _state.value.offsetMs) + steps * STEP_MS).coerceIn(LatencyStore.MIN_MS, LatencyStore.MAX_MS)
        target = next
        _state.update { it.copy(offsetMs = next) } // instant UI; the store write follows
        viewModelScope.launch { latency.setCalibrationMs(next) }
    }

    fun reset() {
        target = 0
        _state.update { it.copy(offsetMs = 0) }
        viewModelScope.launch { latency.setCalibrationMs(0) }
    }

    fun playSignal() {
        val group = _state.value.group ?: return
        if (_state.value.signal == SignalState.Starting) return
        _state.update { it.copy(signal = SignalState.Starting) }
        viewModelScope.launch {
            val ok = runCatching {
                val url = withContext(Dispatchers.IO) { signalUrl() } ?: error("no LAN address")
                players.api.playMedia(group.queueId, listOf(url), option = "play")
                signalQueueId = group.queueId
            }.onFailure { Log.w(TAG, "latency: test signal failed: ${it.javaClass.simpleName}") }.isSuccess
            _state.update { it.copy(signal = if (ok) SignalState.Playing else SignalState.Failed) }
        }
    }

    fun stopSignal() {
        val queue = signalQueueId ?: return
        signalQueueId = null
        _state.update { it.copy(signal = SignalState.Idle) }
        appScope.launch { runCatching { players.api.stop(queue) } }
    }

    private fun signalUrl(): String? {
        val ip = lanAddress() ?: return null
        val s = server ?: TestSignalServer().also { server = it }
        return "http://$ip:${s.port}${TestSignalServer.PATH}"
    }

    /** The TV's IPv4 on the active network (MA must be able to reach it, as it does for Sendspin). */
    private fun lanAddress(): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return props.linkAddresses.map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }

    override fun onCleared() {
        // Leaving the screen ends the signal; the server only lives while the screen is open.
        signalQueueId?.let { q -> appScope.launch { runCatching { players.api.stop(q) } } }
        server?.close()
    }

    companion object {
        const val STEP_MS = 10
        private const val TAG = "Spinroom"
    }
}
