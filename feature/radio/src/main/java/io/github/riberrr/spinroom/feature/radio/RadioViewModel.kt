package io.github.riberrr.spinroom.feature.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.data.ConnectionManager
import io.github.riberrr.spinroom.core.data.FavoriteStationsRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.data.Station
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import io.github.riberrr.spinroom.core.maapi.RadioFilter
import io.github.riberrr.spinroom.core.maapi.RadioFilterKind
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LANGUAGE_OPTIONS = 40
private const val SEARCH_DEBOUNCE_MS = 350L

/** Radio screen logic: favorites (order, move, remove), Radio Browser list for one filter, name search. */
@OptIn(FlowPreview::class)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val repo: FavoriteStationsRepository,
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
    connection: ConnectionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RadioUiState())
    val state: StateFlow<RadioUiState> = _state

    private val query = MutableStateFlow("")
    private var browseJob: Job? = null

    init {
        viewModelScope.launch {
            repo.favorites.collect { favs ->
                _state.update { it.copy(favorites = favs, favoriteKeys = favs.flatMap { s -> listOf(s.uri, s.sourceId) }.filter { k -> k.isNotBlank() }.toSet()) }
            }
        }
        viewModelScope.launch {
            query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }.distinctUntilChanged().collectLatest { runSearch(it) }
        }
        viewModelScope.launch {
            connection.state.first { it is MaConnectionState.Connected }
            val filter = repo.savedFilter.first()
            _state.update { it.copy(filter = filter, filterLabel = filterValueLabel(filter)) }
            launch { runCatchingCancellable { repo.refresh() } }
            loadStations(filter)
        }
    }

    // ---- browse ----

    private fun loadStations(filter: RadioFilter) {
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            val result = runCatchingCancellable { repo.stations(filter) }
            result.onSuccess { items ->
                val label = _state.value.filterLabel
                _state.update { it.copy(rows = toRows(items, label), loading = false) }
            }.onFailure { _state.update { it.copy(loading = false, failed = true) } }
        }
    }

    private suspend fun toRows(items: List<MediaItem>, subtitle: String): List<RadioRow> =
        items.map { RadioRow(it, it.name, repo.imageUrl(it), subtitle) }

    fun onRetry() = loadStations(_state.value.filter)

    fun openPicker(kind: RadioFilterKind) {
        _state.update { it.copy(picker = kind) }
        if (_state.value.options[kind] != null) return
        viewModelScope.launch {
            runCatchingCancellable { repo.options(kind) }.onSuccess { list ->
                val shown = if (kind == RadioFilterKind.LANGUAGE) list.take(LANGUAGE_OPTIONS) else list
                _state.update { it.copy(options = it.options + (kind to shown)) }
            }.onFailure { _state.update { s -> s.copy(picker = null, notice = RadioNotice.FAILED) } }
        }
    }

    fun closePicker() = _state.update { it.copy(picker = null) }

    /** [key] null = "all": falls back to the popular list (the server cannot combine or clear filters). */
    fun pickOption(kind: RadioFilterKind, key: String?, serverName: String?) {
        val filter = when {
            key == null -> RadioFilter.Popular
            kind == RadioFilterKind.LANGUAGE -> RadioFilter.Language(key)
            kind == RadioFilterKind.COUNTRY -> RadioFilter.Country(key)
            else -> RadioFilter.Tag(key)
        }
        val label = filterValueLabel(filter, serverName)
        _state.update { it.copy(filter = filter, filterLabel = label, picker = null) }
        viewModelScope.launch { repo.saveFilter(filter) }
        loadStations(filter)
    }

    fun onPopular() = pickOption(RadioFilterKind.LANGUAGE, null, null)

    // ---- play / favorite ----

    fun play(uri: String) {
        viewModelScope.launch {
            val playerId = settings.settings.first().playerId
            if (playerId.isBlank()) return@launch notice(RadioNotice.NO_PLAYER)
            runCatchingCancellable { queues.playMedia(playerId, listOf(uri), "play") }.onFailure { notice(RadioNotice.FAILED) }
        }
    }

    /** Heart on a browse row: add the station to MA favorites (it lands last) or remove it. */
    fun toggleFavorite(row: RadioRow) {
        viewModelScope.launch {
            val existing = repo.favoriteFor(row.item)
            val result = runCatchingCancellable { if (existing != null) repo.remove(existing) else repo.add(row.uri) }
            if (result.isSuccess) notice(if (existing != null) RadioNotice.REMOVED else RadioNotice.ADDED) else notice(RadioNotice.FAILED)
        }
    }

    fun onNoticeShown() = _state.update { it.copy(notice = null) }
    private fun notice(n: RadioNotice) = _state.update { it.copy(notice = n) }

    // ---- favorite tiles: menu, move, remove ----

    fun openMenu(station: Station) = _state.update { it.copy(menuStation = station) }
    fun closeMenu() = _state.update { it.copy(menuStation = null) }

    fun startMove() = _state.update { s -> s.copy(movingUri = s.menuStation?.uri, menuStation = null) }
    fun stopMove() = _state.update { it.copy(movingUri = null) }

    fun moveBy(delta: Int) {
        val uri = _state.value.movingUri ?: return
        val station = _state.value.favorites.firstOrNull { it.uri == uri } ?: return
        viewModelScope.launch { runCatchingCancellable { repo.move(station, delta) } }
    }

    fun removeMenuStation() {
        val station = _state.value.menuStation ?: return
        _state.update { it.copy(menuStation = null) }
        viewModelScope.launch {
            runCatchingCancellable { repo.remove(station) }.onSuccess { notice(RadioNotice.REMOVED) }.onFailure { notice(RadioNotice.FAILED) }
        }
    }

    // ---- search (radio only) ----

    fun openSearch() = _state.update { it.copy(searching = true) }

    fun closeSearch() {
        query.value = ""
        _state.update { it.copy(searching = false, query = "", searchRows = emptyList(), searchLoading = false, searchFailed = false, symbols = false) }
    }

    fun onType(text: String) = setQuery(_state.value.query + text)
    fun onBackspace() = setQuery(_state.value.query.dropLast(1))
    fun onToggleSymbols() = _state.update { it.copy(symbols = !it.symbols) }

    private fun setQuery(text: String) {
        query.value = text
        _state.update { it.copy(query = text, searchLoading = text.isNotBlank(), searchRows = if (text.isBlank()) emptyList() else it.searchRows) }
    }

    private suspend fun runSearch(text: String) {
        if (text.isBlank()) {
            _state.update { it.copy(searchRows = emptyList(), searchLoading = false, searchFailed = false) }
            return
        }
        _state.update { it.copy(searchLoading = true, searchFailed = false) }
        runCatchingCancellable { repo.search(text) }
            .onSuccess { items ->
                val polish = polishIds()
                allHits = orderSearchResults(items, { repo.favoriteFor(it) != null }, polish)
                applySearchRows()
            }
            .onFailure { _state.update { it.copy(searchLoading = false, searchFailed = true) } }
    }

    private var allHits: List<SearchHit> = emptyList()
    private var polishCache: Set<String>? = null

    /** Ids of Polish stations (Radio Browser, language Polish), fetched once; empty when that fails. */
    private suspend fun polishIds(): Set<String> =
        polishCache ?: runCatchingCancellable { idsOf(repo.stations(RadioFilter.Language("polish"))) }.getOrNull()?.also { polishCache = it } ?: emptySet()

    private suspend fun applySearchRows() {
        val onlyPolish = _state.value.polishOnly
        val polishLabel = filterValueLabel(RadioFilter.Language("polish"))
        val rows = allHits.filter { !onlyPolish || it.polish }.map { hit ->
            RadioRow(hit.item, hit.item.name, repo.imageUrl(hit.item), if (hit.polish) polishLabel else "")
        }
        _state.update { it.copy(searchRows = rows, searchLoading = false) }
    }

    fun onTogglePolishOnly() {
        _state.update { it.copy(polishOnly = !it.polishOnly) }
        viewModelScope.launch { applySearchRows() }
    }

    fun onRetrySearch() {
        viewModelScope.launch { runSearch(query.value) }
    }

    private suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
