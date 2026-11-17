package io.github.riberrr.spinroom.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.SearchResults
import javax.inject.Inject
import io.github.riberrr.spinroom.core.data.LibraryFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COVER_PX = 96

/** On-screen keyboard text + debounced search. Blank query clears results without hitting the server. */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel internal constructor(
    private val backend: SearchBackend,
    debounceMs: Long,
) : ViewModel() {

    @Inject constructor(backend: SearchBackend) : this(backend, DEBOUNCE_MS)

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            query.debounce { if (it.isBlank()) 0L else debounceMs }.distinctUntilChanged().collectLatest { runSearch(it) }
        }
    }

    fun onType(text: String) = setQuery(_state.value.query + text)
    fun onBackspace() = setQuery(_state.value.query.dropLast(1))
    fun onVoiceResult(text: String) = setQuery(text.trim())
    fun onToggleSymbols() = _state.update { it.copy(symbols = !it.symbols) }
    fun onFilter(filter: SearchFilter) = _state.update { it.copy(filter = filter) }
    fun onRetry() {
        viewModelScope.launch { runSearch(query.value) }
    }
    fun onVoiceUnavailable() = _state.update { it.copy(notice = SearchUiState.Notice.NO_VOICE) }
    fun onSubmit() {
        if (query.value.isNotBlank()) viewModelScope.launch { runSearch(query.value) }
    }

    fun onPhysicalInput(input: PhysicalInput) = when (input) {
        is PhysicalInput.Text -> onType(input.text)
        PhysicalInput.Backspace -> onBackspace()
        PhysicalInput.Submit -> onSubmit()
    }

    private val checked = HashSet<String>()

    /** A row got focus: ask MA whether it is really a favorite (provider hits arrive with `favorite = false`). */
    fun onSelect(row: SearchRow) {
        val uri = row.uri
        if (row.item.favorite || !checked.add(uri)) return
        viewModelScope.launch {
            val fav = try {
                backend.isFavorite(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            // Never override what the user just did here.
            if (fav == true) _state.update { if (uri in it.favorites) it else it.copy(favorites = it.favorites + (uri to true)) }
        }
    }

    fun onPanel(open: Boolean) = _state.update { it.copy(panelOpen = open) }
    fun onToggleFavoritesOnly() = updateFilters { it.copy(favoriteOnly = !it.favoriteOnly) }
    fun onToggleDecade(decade: Int) = updateFilters { it.copy(decade = if (it.decade == decade) null else decade) }
    fun onToggleSource(domain: String) = updateFilters { it.copy(provider = if (it.provider == domain) null else domain) }
    fun onClearFilters() = updateFilters { LibraryFilter() }
    private fun updateFilters(f: (LibraryFilter) -> LibraryFilter) = _state.update { it.copy(filters = f(it.filters)) }

    /** Heart button / long OK: flips the favorite flag and shows the undo message. */
    fun onToggleFavorite(row: SearchRow) = setFavorite(row.uri, row.title, !_state.value.isFavorite(row), showToast = true)

    fun onUndo() {
        val t = _state.value.toast ?: return
        toastJob?.cancel()
        _state.update { it.copy(toast = null) }
        setFavorite(t.uri, t.title, !t.added, showToast = false)
    }

    private var toastJob: Job? = null

    private fun setFavorite(uri: String, title: String, value: Boolean, showToast: Boolean) {
        val before = _state.value.favorites[uri]
        _state.update { it.copy(favorites = it.favorites + (uri to value)) }
        if (showToast) {
            _state.update { it.copy(toast = FavoriteToast(uri, title, value)) }
            toastJob?.cancel()
            toastJob = viewModelScope.launch {
                delay(TOAST_MS)
                _state.update { it.copy(toast = null) }
            }
        }
        viewModelScope.launch {
            val ok = try {
                backend.setFavorite(uri, value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (!ok) _state.update {
                val restored = if (before == null) it.favorites - uri else it.favorites + (uri to before)
                it.copy(favorites = restored, toast = it.toast?.takeIf { t -> t.uri != uri }, notice = SearchUiState.Notice.FAVORITE_FAILED)
            }
        }
    }

    fun onNoticeShown() = _state.update { it.copy(notice = null) }

    /** Tracks and radio stations play right away; the rest is opened in details by the screen. */
    fun onPlay(row: SearchRow) {
        viewModelScope.launch {
            val notice = try {
                if (backend.play(row.uri)) null else SearchUiState.Notice.NO_PLAYER
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchUiState.Notice.PLAY_FAILED
            }
            if (notice != null) _state.update { it.copy(notice = notice) }
        }
    }

    private fun setQuery(text: String) {
        _state.update { it.copy(query = text, loading = text.isNotBlank(), failed = false) }
        query.value = text
    }

    private suspend fun runSearch(text: String) {
        if (text.isBlank()) {
            _state.update {
                it.copy(
                    loading = false, failed = false, artists = emptyList(), albums = emptyList(),
                    tracks = emptyList(), playlists = emptyList(), radio = emptyList(),
                )
            }
            return
        }
        _state.update { it.copy(loading = true, failed = false) }
        try {
            val results = backend.search(text)
            val base = backend.serverAddress.first()
            _state.update { s -> if (s.query != text) s else s.withResults(results, base).copy(loading = false) }
        } catch (e: CancellationException) {
            // Our own cancellation (a newer query) must propagate. A CancellationException raised
            // while this coroutine is still active is a timeout from the client (no connection):
            // it has to end in the failed state, or the spinner would stay forever.
            if (!currentCoroutineContext().isActive) throw e
            _state.update { it.copy(loading = false, failed = true) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    private fun SearchUiState.withResults(r: SearchResults, base: String) = copy(
        artists = r.artists.map { row(it, ResultKind.ARTIST, base) },
        albums = r.albums.map { row(it, ResultKind.ALBUM, base) },
        tracks = r.tracks.map { row(it, ResultKind.TRACK, base) },
        playlists = r.playlists.map { row(it, ResultKind.PLAYLIST, base) },
        radio = r.radio.map { row(it, ResultKind.RADIO, base) },
    )

    private fun row(item: MediaItem, kind: ResultKind, base: String): SearchRow {
        val album = if (kind == ResultKind.TRACK) item.album?.name.orEmpty() else ""
        val subtitle = when (kind) {
            ResultKind.TRACK, ResultKind.ALBUM -> listOf(item.artistLine, album).filter { it.isNotBlank() }.joinToString(" · ")
            else -> ""
        }
        return SearchRow(item, kind, item.name, subtitle, sourceLabel(item.provider), ImageUrls.url(base, item.bestImage, COVER_PX))
    }

    companion object {
        const val DEBOUNCE_MS = 400L
        const val TOAST_MS = 5_000L

        /** `ytmusic--x1y2` / `filesystem_local--ab` → readable source; unknown domains are prettified. */
        fun sourceLabel(provider: String): String = when (val domain = provider.substringBefore("--")) {
            "filesystem_local", "filesystem_smb" -> "Pliki lokalne"
            "ytmusic" -> "YouTube Music"
            "spotify" -> "Spotify"
            "tunein" -> "TuneIn"
            "radiobrowser" -> "Radio Browser"
            "library", "" -> "Biblioteka"
            else -> domain.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}
