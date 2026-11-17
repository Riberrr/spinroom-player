package io.github.riberrr.spinroom.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.Appearance
import io.github.riberrr.spinroom.core.data.AppearanceSettings
import io.github.riberrr.spinroom.core.data.AuthRepository
import io.github.riberrr.spinroom.core.data.HomeLayout
import io.github.riberrr.spinroom.core.data.HomeLayoutSettings
import io.github.riberrr.spinroom.core.data.PlayerName
import io.github.riberrr.spinroom.core.data.PlayerNameStore
import io.github.riberrr.spinroom.core.data.PlayerRenamer
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.LibraryRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.di.SendspinCredentials
import io.github.riberrr.spinroom.core.maapi.AuthApi
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.discovery.DiscoveredServer
import io.github.riberrr.spinroom.core.maapi.discovery.MaServerDiscovery
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /** MA players to play on; [playerId] is the chosen one (empty = none). */
    val players: List<Player> = emptyList(),
    val playerId: String = "",
    /** MA player id of this TV (its Sendspin client id); empty until known. */
    val ownPlayerId: String = "",
    val appearance: Appearance = Appearance(),
    val homeLayout: HomeLayout = HomeLayout(),
    /** Name the user chose for this TV; null = none yet (MA shows the device name). */
    val playerName: String? = null,
    val renameFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val auth: AuthRepository,
    private val library: LibraryRepository,
    private val connection: ConnectionManager,
    private val discovery: MaServerDiscovery,
    private val playerRepository: PlayerRepository,
    private val sendspinCredentials: SendspinCredentials,
    private val appearanceSettings: AppearanceSettings,
    private val homeLayoutSettings: HomeLayoutSettings,
    private val playerNameStore: PlayerNameStore,
    private val renamer: PlayerRenamer,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        connection.start()
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update { it.copy(address = s.serverAddress, loggedIn = s.isConfigured) }
        }
        viewModelScope.launch {
            runCatching { sendspinCredentials.get().identity.id }
                .onSuccess { id -> _state.update { it.copy(ownPlayerId = id) } }
        }
        viewModelScope.launch { settings.settings.collect { s -> _state.update { it.copy(autostart = s.autostart, playerId = s.playerId) } } }
        viewModelScope.launch {
            // (Re)load the player list each time an authenticated session comes up.
            connection.state
                .map { it is MaConnectionState.Connected && it.authenticated }
                .distinctUntilChanged()
                .flatMapLatest { ready -> if (ready) playerRepository.players().catch { emit(emptyList()) } else flowOf(emptyList()) }
                .collect { list -> _state.update { it.copy(players = list) } }
        }
        viewModelScope.launch { appearanceSettings.appearance.collect { a -> _state.update { it.copy(appearance = a) } } }
        viewModelScope.launch { homeLayoutSettings.layout.collect { l -> _state.update { it.copy(homeLayout = l) } } }
        viewModelScope.launch { playerNameStore.name.collect { n -> _state.update { it.copy(playerName = n) } } }
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
    fun onPickPlayer(player: Player) { viewModelScope.launch { settings.setPlayerId(player.playerId) } }

    fun setTheme(id: String) { viewModelScope.launch { appearanceSettings.setTheme(id) } }
    fun setAccent(id: String) { viewModelScope.launch { appearanceSettings.setAccent(id) } }
    fun setPlayerBackground(id: String) { viewModelScope.launch { appearanceSettings.setPlayerBackground(id) } }
    fun setScreensaver(on: Boolean) { viewModelScope.launch { appearanceSettings.setScreensaver(on) } }
    fun moveRow(id: String, delta: Int) { viewModelScope.launch { homeLayoutSettings.update { it.moved(id, delta) } } }
    fun toggleRow(id: String) { viewModelScope.launch { homeLayoutSettings.update { it.toggled(id) } } }

    /** Saves the name locally and renames the TV's player in MA. Returns false when [raw] is invalid. */
    fun renamePlayer(raw: String): Boolean {
        val name = (PlayerName.validate(raw) as? PlayerName.Result.Ok)?.name ?: return false
        _state.update { it.copy(renameFailed = false) }
        viewModelScope.launch {
            playerNameStore.set(name)
            val id = _state.value.ownPlayerId
            if (id.isEmpty()) return@launch
            try {
                renamer.rename(id, name)
                Log.i(TAG, "player renamed in MA")
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                _state.update { it.copy(renameFailed = true) }
            } catch (e: Exception) {
                Log.w(TAG, "rename failed: ${e.javaClass.simpleName}: ${e.message}")
                _state.update { it.copy(renameFailed = true) }
            }
        }
        return true
    }

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
