package io.github.riberrr.spinroom.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.SearchResults
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
            throw e
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
