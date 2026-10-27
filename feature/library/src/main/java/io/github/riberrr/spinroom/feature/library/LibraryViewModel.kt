package io.github.riberrr.spinroom.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MediaItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val THUMB_PX = 200

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(private val backend: LibraryBackend) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.Albums)
    private val counts = MutableStateFlow<Map<LibraryTab, Int>>(emptyMap())
    private val scrollEvents = Channel<Int>(Channel.CONFLATED)

    val state: StateFlow<LibraryUiState> = combine(tab, counts) { t, c -> LibraryUiState(t, c) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    /** Grid items of the selected tab, mapped to [LibraryRow]. */
    val items: Flow<PagingData<LibraryRow>> = combine(tab, backend.serverAddress) { t, a -> t to a }
        .distinctUntilChanged()
        .flatMapLatest { (t, a) -> backend.pager(t).map { page -> page.map { it.toRow(a) } } }
        .cachedIn(viewModelScope)

    /** Absolute grid index the screen should scroll to (one-shot). */
    val scrollTo: Flow<Int> = scrollEvents.receiveAsFlow()

    init {
        refreshCount(LibraryTab.Albums)
    }

    fun onSelectTab(newTab: LibraryTab) {
        if (newTab == tab.value) return
        tab.value = newTab
        refreshCount(newTab)
    }

    /** Scrolls to the first item whose name starts at or after [letter] (`#` = start of the list). */
    fun onJumpToLetter(letter: String) {
        val t = tab.value
        viewModelScope.launch {
            val total = counts.value[t] ?: runCatching { backend.count(t) }.getOrNull() ?: return@launch
            val index = if (letter == "#") 0 else runCatching {
                firstIndexOf(letter.first(), total) { backend.itemAt(t, it) }
            }.getOrNull() ?: return@launch
            scrollEvents.trySend(index)
        }
    }

    private fun refreshCount(t: LibraryTab) {
        viewModelScope.launch {
            runCatching { backend.count(t) }.onSuccess { n -> counts.update { it + (t to n) } }
        }
    }
}

/** Binary search over a name-sorted list of [total] items for the first one whose first letter is >= [letter]. */
internal suspend fun firstIndexOf(letter: Char, total: Int, itemAt: suspend (Int) -> MediaItem?): Int {
    var lo = 0
    var hi = total
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        val first = itemAt(mid)?.name?.trimStart()?.firstOrNull()?.uppercaseChar()
        if (first == null || first >= letter) hi = mid else lo = mid + 1
    }
    return lo.coerceAtMost((total - 1).coerceAtLeast(0))
}

internal fun MediaItem.toRow(serverAddress: String): LibraryRow {
    val type = mediaType.ifBlank { "item" }
    return LibraryRow(
        uri = uri ?: "library://$type/$itemId",
        title = name,
        subtitle = if (mediaType == "album" || mediaType == "track") artistLine else "",
        coverUrl = if (serverAddress.isBlank()) null else ImageUrls.url(serverAddress, bestImage, THUMB_PX),
    )
}
