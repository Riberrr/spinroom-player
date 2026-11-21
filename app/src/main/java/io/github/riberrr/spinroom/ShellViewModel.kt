package io.github.riberrr.spinroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.FavoriteStationsRepository
import io.github.riberrr.spinroom.core.data.PlayerRepository
import io.github.riberrr.spinroom.core.data.KeyAction
import io.github.riberrr.spinroom.core.data.KeyAssignments
import io.github.riberrr.spinroom.core.data.KeyBindingsRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.feature.settings.resolveOwnPlayerId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A favorite station or playlist as the shell shows it (top player bar station list). */
data class Favorite(val uri: String, val name: String, val imageUrl: String?, val isStation: Boolean)


/**
 * Shell-level state: favorite stations/playlists (menu + dock) and the actions that need them
 * (play a station, step through stations for CH+/CH-). Favorites are read from `library_items(favorite = true)`;
 * order = order MA returns (stations first, then playlists), which is also the CH+/CH- order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
    private val stationFavorites: FavoriteStationsRepository,
    private val playerRepository: PlayerRepository,
    keyBindings: KeyBindingsRepository,
    connection: ConnectionManager,
) : ViewModel() {
    private val _stations = MutableStateFlow<List<Favorite>>(emptyList())
    val stations: StateFlow<List<Favorite>> = _stations

    /** Key code that opens the player panel (hint at the top bar); null when the shortcut was removed. */
    val drawerKey: StateFlow<Int?> = keyBindings.assignments.map { drawerKeyCode(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, drawerKeyCode(KeyAssignments()))

    private val _openDrawer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the remote asked for the player drawer (hold Play/Pause). */
    val openDrawer: SharedFlow<Unit> = _openDrawer

    init {
        viewModelScope.launch {
            connection.state.collect { if (it is MaConnectionState.Connected) refresh() }
        }
        // MA gives this TV a new player id when its Sendspin identity changes (reinstall, new signature): when the chosen
        // player is gone or offline, follow the TV's own available player instead of sending music nowhere.
        viewModelScope.launch {
            connection.state.map { it is MaConnectionState.Connected && it.authenticated }.distinctUntilChanged()
                .flatMapLatest { ready -> if (ready) playerRepository.players().catch { emit(emptyList()) } else emptyFlow() }
                .collect { players -> followOwnPlayer(players) }
        }
        // Stations come from the SUI4 repository, so the dock and CH+/CH- follow the user's order.
        viewModelScope.launch {
            stationFavorites.favorites.collect { list -> _stations.value = list.map { Favorite(it.uri, it.name, it.imageUrl, true) } }
        }
    }

    private suspend fun followOwnPlayer(players: List<Player>) {
        val selected = settings.settings.first().playerId
        if (players.firstOrNull { it.playerId == selected }?.available == true) return
        val own = resolveOwnPlayerId(players.filter { it.available }, "", android.os.Build.MODEL, selected) ?: return
        if (own == selected) return
        settings.setPlayerId(own)
        android.util.Log.i("Spinroom", "playback: selected player offline, switched to this TV")
    }

    fun refresh() {
        viewModelScope.launch { runCatching { stationFavorites.refresh() } }
    }

    private val _navigate = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Emits a route when a remote shortcut asked to open a screen (SUI8b). */
    val navigate: SharedFlow<String> = _navigate

    fun requestRoute(route: String) {
        _navigate.tryEmit(route)
    }

    /** Remote shortcut "ulubiona stacja nr N" (1-based); false when there is no such favorite. */
    fun playStationNumber(n: Int): Boolean {
        val target = _stations.value.getOrNull(n - 1) ?: return false
        playStation(target.uri)
        return true
    }

    fun requestDrawer() {
        _openDrawer.tryEmit(Unit)
    }

    fun playStation(uri: String) {
        viewModelScope.launch {
            val playerId = settings.settings.first().playerId
            if (playerId.isBlank()) return@launch
            runCatching { queues.playMedia(playerId, listOf(uri), "play") }
        }
    }

    /** CH+/CH-: next/previous favorite station relative to the one playing. Returns false when there are no favorites. */
    fun stepStation(mediaUri: String, mediaName: String, forward: Boolean): Boolean {
        val list = _stations.value
        val target = list.getOrNull(cycleIndex(stationIndex(list, mediaUri, mediaName), list.size, forward)) ?: return false
        playStation(target.uri)
        return true
    }

}

/** The key that opens the player panel: the first assignment of [KeyAction.OPEN_DRAWER] (a short press wins over a hold). */
internal fun drawerKeyCode(a: KeyAssignments): Int? =
    a.items.filter { it.action == KeyAction.OPEN_DRAWER }.let { l -> l.firstOrNull { !it.long } ?: l.firstOrNull() }?.keyCode

/**
 * What the bar shows: song (radio: from the stream, else the station) / title on top, below it the station (radio) or artist.
 * [songText] is the "artist – title" line of the playing station; the station name goes underneath so the title is never missing.
 */
internal fun barLines(isRadio: Boolean, title: String, artist: String, songText: String): Pair<String, String> =
    if (isRadio && songText.isNotBlank()) songText to title else title to artist

/** Index of the playing station among [stations]: by uri, else by name (the queue may carry another provider's uri); -1 if absent. */
internal fun stationIndex(stations: List<Favorite>, mediaUri: String, mediaName: String): Int {
    val byUri = stations.indexOfFirst { it.uri == mediaUri }
    if (byUri >= 0) return byUri
    return if (mediaName.isBlank()) -1 else stations.indexOfFirst { it.name.equals(mediaName, ignoreCase = true) }
}
