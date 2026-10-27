package io.github.riberrr.spinroom.feature.settings

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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val status: LoginStatus = LoginStatus.Idle,
    val connection: MaConnectionState = MaConnectionState.Disconnected,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val auth: AuthRepository,
    private val library: LibraryRepository,
    private val connection: ConnectionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        connection.start()
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update { it.copy(address = s.serverAddress, loggedIn = s.isConfigured) }
        }
        viewModelScope.launch { connection.state.collect { c -> _state.update { it.copy(connection = c) } } }
    }

    fun onAddress(v: String) = _state.update { it.copy(address = v) }
    fun onUsername(v: String) = _state.update { it.copy(username = v) }
    fun onPassword(v: String) = _state.update { it.copy(password = v) }

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
                LoginStatus.WrongCredentials
            } catch (e: Exception) {
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
}
