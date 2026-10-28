package io.github.riberrr.spinroom.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COVER_PX = 96

/**
 * Providers on the left, the `music/browse` tree of the selected provider on the right. Nothing here knows a specific
 * plugin: radio stations and YouTube Music items are just playable browse items.
 */
@HiltViewModel
class SourcesViewModel @Inject constructor(private val backend: SourcesBackend) : ViewModel() {

    private val _state = MutableStateFlow(SourcesUiState())
    val state: StateFlow<SourcesUiState> = _state

    private var serverAddress = ""
    private var playerId = ""
    private val reload = MutableStateFlow(0)
    private var browseJob: Job? = null

    init {
        viewModelScope.launch { backend.serverAddress.collect { serverAddress = it } }
        viewModelScope.launch {
            backend.playerId.collect { id ->
                playerId = id
                _state.update { it.copy(hasPlayer = id.isNotBlank(), noPlayer = false) }
            }
        }
        viewModelScope.launch {
            combine(backend.connection, reload) { c, _ -> c }.collectLatest { c ->
                _state.update { it.copy(connection = c) }
                when (c) {
                    is MaConnectionState.Connected -> loadProviders()
                    is MaConnectionState.Failed -> _state.update { it.copy(loadingProviders = false, providersFailed = true) }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadProviders() {
        _state.update { it.copy(loadingProviders = true, providersFailed = false) }
        val result = runCatching { backend.browse(null) }
        val providers = result.getOrNull().orEmpty().filter { it.path != null }.map { it.toRow() }
        _state.update { it.copy(loadingProviders = false, providersFailed = result.isFailure, providers = providers, trail = emptyList()) }
        if (providers.isNotEmpty()) onSelectProvider(_state.value.selectedProvider.coerceIn(providers.indices))
    }

    fun onRetry() { reload.update { it + 1 } }

    /** Focusing / clicking a provider shows its root folder. */
    fun onSelectProvider(index: Int) {
        val s = _state.value
        val provider = s.providers.getOrNull(index) ?: return
        val path = provider.item.path ?: return
        if (s.trail.firstOrNull()?.path == path) return
        _state.update { it.copy(selectedProvider = index, trail = listOf(Crumb(provider.name, path)), items = emptyList(), playedName = null) }
        load(path)
    }

    /** OK on a row: open a folder, or play a station / track. */
    fun onItemClick(row: SourceRow) {
        val uri = row.playUri
        val path = row.item.path
        if (uri != null) {
            play(row.name, uri)
        } else if (path != null) {
            _state.update { it.copy(trail = it.trail + Crumb(row.name, path), items = emptyList(), playedName = null) }
            load(path)
        }
    }

    /** Jump to breadcrumb [index] (0 = provider root). */
    fun onCrumb(index: Int) {
        val trail = _state.value.trail
        if (index !in 0 until trail.lastIndex) return
        val cut = trail.take(index + 1)
        _state.update { it.copy(trail = cut, items = emptyList()) }
        load(cut.last().path)
    }

    /** Back one folder; returns false when already at the provider root (caller lets Back leave the screen). */
    fun onBack(): Boolean {
        val trail = _state.value.trail
        if (trail.size <= 1) return false
        onCrumb(trail.size - 2)
        return true
    }

    fun onToggleFavorite(row: SourceRow) {
        val target = !row.favorite
        setFavoriteLocally(row.key, target)
        viewModelScope.launch {
            runCatching { backend.setFavorite(row.item, target) }.onFailure { setFavoriteLocally(row.key, !target) }
        }
    }

    private fun setFavoriteLocally(key: String, value: Boolean) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) it.copy(favorite = value) else it }) }

    private fun play(name: String, uri: String) {
        if (playerId.isBlank()) {
            _state.update { it.copy(noPlayer = true) }
            return
        }
        _state.update { it.copy(playedName = name, playFailed = false, noPlayer = false) }
        viewModelScope.launch {
            runCatching { backend.play(playerId, uri) }.onFailure { _state.update { s -> s.copy(playFailed = true) } }
        }
    }

    private fun load(path: String) {
        browseJob?.cancel()
        _state.update { it.copy(loadingItems = true, itemsFailed = false) }
        browseJob = viewModelScope.launch {
            val result = runCatching { backend.browse(path) }
            // The user may have moved on while this was loading.
            if (_state.value.trail.lastOrNull()?.path != path) return@launch
            _state.update {
                it.copy(loadingItems = false, itemsFailed = result.isFailure, items = result.getOrNull().orEmpty().map { m -> m.toRow() })
            }
        }
    }

    private fun MediaItem.toRow() = SourceRow(
        item = this,
        subtitle = when {
            artists.isNotEmpty() -> artistLine
            !metadata?.description.isNullOrBlank() -> metadata?.description.orEmpty()
            mediaType == "folder" || mediaType.isBlank() -> ""
            else -> mediaType.replaceFirstChar { it.uppercase() }
        },
        coverUrl = ImageUrls.url(serverAddress, bestImage, COVER_PX),
        favorite = favorite,
    )
}
