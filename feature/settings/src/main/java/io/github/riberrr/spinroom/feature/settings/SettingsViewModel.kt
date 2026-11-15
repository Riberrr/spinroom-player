package io.github.riberrr.spinroom.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.AuthRepository
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.AuthApi
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import io.github.riberrr.spinroom.core.maapi.discovery.DiscoveredServer
import io.github.riberrr.spinroom.core.maapi.discovery.MaServerDiscovery
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginStatus {
    data object Idle : LoginStatus
    data object Working : LoginStatus
    data object WrongCredentials : LoginStatus
    data object ServerUnreachable : LoginStatus
    /** Logged in and the library answered; [albums] is the album count. */
    data class Done(val albums: Int) : LoginStatus
}

data class SettingsUiState(
    val address: String = "",
    val username: String = "",
    val password: String = "",
    val loggedIn: Boolean = false,
    val autostart: Boolean = true,
    val status: LoginStatus = LoginStatus.Idle,
    val connection: MaConnectionState = MaConnectionState.Disconnected,
    /** Servers found on the local network (mDNS). */
    val servers: List<DiscoveredServer> = emptyList(),
    /** True once the scan ran long enough that an empty [servers] means "none found". */
    val scanFinished: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val auth: AuthRepository,
    private val library: LibraryRepository,
    private val connection: ConnectionManager,
    private val discovery: MaServerDiscovery,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        connection.start()
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update { it.copy(address = s.serverAddress, loggedIn = s.isConfigured) }
        }
        viewModelScope.launch { settings.settings.collect { s -> _state.update { it.copy(autostart = s.autostart) } } }
        viewModelScope.launch { connection.state.collect { c -> _state.update { it.copy(connection = c) } } }
        viewModelScope.launch {
            discovery.servers()
                .catch { e -> Log.w(TAG, "discovery: failed: ${e.javaClass.simpleName}") }
                .collect { list -> _state.update { it.copy(servers = list) } }
        }
        viewModelScope.launch {
            delay(SCAN_WINDOW_MS)
            _state.update { it.copy(scanFinished = true) }
        }
    }

    fun onAddress(v: String) = _state.update { it.copy(address = v) }
    fun onUsername(v: String) = _state.update { it.copy(username = v) }
    fun onPassword(v: String) = _state.update { it.copy(password = v) }
    fun onPickServer(server: DiscoveredServer) = _state.update { it.copy(address = server.address) }

    fun onAutostart() {
        val next = !_state.value.autostart
        _state.update { it.copy(autostart = next) }
        viewModelScope.launch { settings.setAutostart(next) }
    }

    fun login() {
        val s = _state.value
        if (s.address.isBlank() || s.username.isBlank() || s.password.isEmpty()) return
        _state.update { it.copy(status = LoginStatus.Working) }
        viewModelScope.launch {
            val status = try {
                auth.login(s.address, s.username, s.password)
                // Prove the stored token works: ask the library for its album count.
                LoginStatus.Done(library.count(MediaTypes.ALBUMS))
            } catch (e: AuthApi.LoginFailed) {
                Log.w(TAG, "login: rejected by server")
                LoginStatus.WrongCredentials
            } catch (e: CancellationException) {
                // MaClient.command times out with TimeoutCancellationException: a failure, not a cancel.
                if (e !is TimeoutCancellationException) throw e
                Log.w(TAG, "login: timed out")
                LoginStatus.ServerUnreachable
            } catch (e: Exception) {
                // Class + message only: never the password or a token.
                Log.w(TAG, "login: failed: ${e.javaClass.simpleName}: ${e.message}")
                LoginStatus.ServerUnreachable
            }
            // The password never outlives the attempt.
            _state.update { it.copy(status = status, password = "", loggedIn = status is LoginStatus.Done || it.loggedIn) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            _state.update { it.copy(loggedIn = false, status = LoginStatus.Idle) }
        }
    }

    private companion object {
        const val TAG = "Spinroom"
        const val SCAN_WINDOW_MS = 8_000L
    }
}
