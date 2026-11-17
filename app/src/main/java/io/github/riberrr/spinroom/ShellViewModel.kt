package io.github.riberrr.spinroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.LibraryApi
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaTypes
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A favorite station or playlist as the shell shows it (menu "Przypięte", dock station list). */
data class Favorite(val uri: String, val name: String, val imageUrl: String?, val isStation: Boolean)

/** Max pinned entries in the menu. */
const val PINNED_COUNT = 5

/**
 * Shell-level state: favorite stations/playlists (menu + dock) and the actions that need them
 * (play a station, step through stations for CH+/CH-). Favorites are read from `library_items(favorite = true)`;
 * order = order MA returns (stations first, then playlists), which is also the CH+/CH- order.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val library: LibraryApi,
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
    connection: ConnectionManager,
) : ViewModel() {
    private val _stations = MutableStateFlow<List<Favorite>>(emptyList())
    val stations: StateFlow<List<Favorite>> = _stations

    private val _playlists = MutableStateFlow<List<Favorite>>(emptyList())
    val playlists: StateFlow<List<Favorite>> = _playlists

    private val _openDrawer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the remote asked for the player drawer (hold Play/Pause). */
    val openDrawer: SharedFlow<Unit> = _openDrawer

    init {
        viewModelScope.launch {
            connection.state.collect { if (it is MaConnectionState.Connected) refresh() }
        }
    }

    /** Pinned = first [PINNED_COUNT] of stations followed by playlists. */
    fun pinned(): List<Favorite> = pinnedOf(_stations.value, _playlists.value)

    fun refresh() {
        viewModelScope.launch {
            val address = settings.settings.first().serverAddress
            runCatching { library.items(MediaTypes.RADIOS, limit = 30, favorite = true) }
                .onSuccess { items -> _stations.value = items.mapNotNull { it.toFavorite(address, true) } }
            runCatching { library.items(MediaTypes.PLAYLISTS, limit = 30, favorite = true) }
                .onSuccess { items -> _playlists.value = items.mapNotNull { it.toFavorite(address, false) } }
        }
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

    private fun MediaItem.toFavorite(address: String, station: Boolean): Favorite? {
        val u = uri ?: return null
        return Favorite(u, name, ImageUrls.url(address, bestImage, 256), station)
    }
}

internal fun pinnedOf(stations: List<Favorite>, playlists: List<Favorite>): List<Favorite> = (stations + playlists).take(PINNED_COUNT)

/** Index of the playing station among [stations]: by uri, else by name (the queue may carry another provider's uri); -1 if absent. */
internal fun stationIndex(stations: List<Favorite>, mediaUri: String, mediaName: String): Int {
    val byUri = stations.indexOfFirst { it.uri == mediaUri }
    if (byUri >= 0) return byUri
    return if (mediaName.isBlank()) -1 else stations.indexOfFirst { it.name.equals(mediaName, ignoreCase = true) }
}
