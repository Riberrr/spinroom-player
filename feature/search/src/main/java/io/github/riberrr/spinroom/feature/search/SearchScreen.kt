package io.github.riberrr.spinroom.feature.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import io.github.riberrr.spinroom.core.design.focusJump
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.design.ContentBackdrop
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FavoriteHeart
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.design.stationColor
import io.github.riberrr.spinroom.core.design.AcronymFill
import io.github.riberrr.spinroom.feature.library.FilterPanel
import io.github.riberrr.spinroom.feature.library.FilterPanelActions
import kotlinx.coroutines.delay

private val LetterKeys = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż".map { it.toString() }
private val SymbolKeys = "1234567890.-'’,&!?:;/()+@#%_*".map { it.toString() }
private const val KEY_COLUMNS = 7
private const val NOTICE_MS = 3_000L

/** Actions the screen can trigger; the ViewModel implements them, previews pass no-ops. */
data class SearchActions(
    val onType: (String) -> Unit = {},
    val onBackspace: () -> Unit = {},
    val onToggleSymbols: () -> Unit = {},
    val onFilter: (SearchFilter) -> Unit = {},
    val onOpen: (SearchRow) -> Unit = {},
    val onPlay: (SearchRow) -> Unit = {},
    val onFavorite: (SearchRow) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onSelect: (SearchRow) -> Unit = {},
    val onVoice: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onPhysical: (PhysicalInput) -> Unit = {},
    val onPanel: (Boolean) -> Unit = {},
    val panel: FilterPanelActions = FilterPanelActions(),
) {
    companion object {
        fun of(vm: SearchViewModel, onOpenDetails: (String) -> Unit, onVoice: () -> Unit) = SearchActions(
            onType = vm::onType,
            onBackspace = vm::onBackspace,
            onToggleSymbols = vm::onToggleSymbols,
            onFilter = vm::onFilter,
            onOpen = { row ->
                when (row.kind) {
                    ResultKind.TRACK, ResultKind.RADIO -> vm.onPlay(row)
                    else -> onOpenDetails(row.uri)
                }
            },
            onPlay = vm::onPlay,
            onFavorite = vm::onToggleFavorite,
            onUndo = vm::onUndo,
            onSelect = vm::onSelect,
            onVoice = onVoice,
            onRetry = vm::onRetry,
            onPhysical = vm::onPhysicalInput,
            onPanel = vm::onPanel,
            panel = FilterPanelActions(
                onToggleDecade = vm::onToggleDecade,
                onToggleSource = vm::onToggleSource,
                onToggleFavorites = vm::onToggleFavoritesOnly,
                onClear = vm::onClearFilters,
                onClose = { vm.onPanel(false) },
            ),
        )
    }
}

/** Search screen (route `search`). [onOpenDetails] gets a Music Assistant URI of an artist, album or playlist. */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onOpenDetails: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = stringResource(R.string.search_voice_prompt)

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::onVoiceResult)
        }
    }
    val startVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onVoiceUnavailable()
        }
    }

    val actions = SearchActions.of(viewModel, onOpenDetails, startVoice)
    SearchContent(
        state = state,
        actions = actions,
        onNoticeShown = viewModel::onNoticeShown,
        modifier = modifier.onPreviewKeyEvent {
            // Remote's search/mic key.
            if (it.key == Key.Search && it.type == KeyEventType.KeyUp) {
                startVoice()
                true
            } else {
                false
            }
        },
    )
}

/** Stateless UI. Left: query, mic, keyboard. Right: categories, filters and grouped results. */
@Composable
fun SearchContent(
    state: SearchUiState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    onNoticeShown: () -> Unit = {},
) {
    val firstKey = remember { FocusRequester() }
    val filtersChip = remember { FocusRequester() }
    val firstResult = remember { FocusRequester() }
    var selected by remember { mutableStateOf<SearchRow?>(null) }
    var panelWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
    LaunchedEffect(state.panelOpen) {
        if (panelWasOpen && !state.panelOpen) runCatching { filtersChip.requestFocus() }
        panelWasOpen = state.panelOpen
    }

    val backdropRow = selected?.takeIf { state.hasShown }
    Box(
        modifier.fillMaxSize().onPreviewKeyEvent { e ->
            // Hardware keyboard: characters go to the query without focusing anything. D-pad keys have no character and pass through.
            if (state.panelOpen || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val input = mapPhysicalKey(
                isBackspace = e.key == Key.Backspace,
                isEnter = e.key == Key.Enter || e.key == Key.NumPadEnter,
                codePoint = e.utf16CodePoint,
                hasModifier = e.isCtrlPressed || e.isAltPressed || e.isMetaPressed,
            ) ?: return@onPreviewKeyEvent false
            actions.onPhysical(input)
            // Enter still activates a focused button; typing and Backspace are consumed.
            input != PhysicalInput.Submit
        },
    ) {
        ContentBackdrop(
            imageUrl = backdropRow?.imageUrl,
            seedColor = backdropRow?.takeIf { it.imageUrl == null && it.kind == ResultKind.RADIO }?.let { stationColor(it.title) },
        )
        Row(
            Modifier.fillMaxSize().padding(start = 48.dp, top = 36.dp, end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(Modifier.width(350.dp)) {
                Text(stringResource(R.string.search_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    QueryField(state.query, Modifier.weight(1f))
                    FocusSurface(
                        onClick = actions.onVoice,
                        modifier = Modifier.size(52.dp),
                        focusedScale = 1.08f,
                        containerColor = SpinroomColors.Glass,
                        focusedContainerColor = SpinroomColors.GlassHigh,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(MicIcon, stringResource(R.string.search_voice), tint = SpinroomColors.Accent, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Keyboard(state.symbols, actions, firstKey, firstResult, state.hasShown, Modifier.padding(top = 16.dp))
                Text(
                    stringResource(R.string.search_hint_keyboard), color = SpinroomColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 14.dp),
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                CategoryChips(state, actions, filtersChip, if (state.hasShown) firstResult else null)
                ActiveFilterChips(state.filters, actions.panel)
                // No results (idle, empty, loading, offline): nothing here may take focus, so ← from the keyboard
                // reaches the menu instead of an invisible target on the right.
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp).focusProperties { canFocus = state.hasShown }) {
                    Results(state, actions, firstResult) { selected = it; actions.onSelect(it) }
                }
            }
        }
        state.toast?.let { Toast(it, actions.onUndo, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) }
        state.notice?.let { Notice(it, onNoticeShown, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) }
        if (state.panelOpen) {
            val first = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            FilterPanel(
                filter = state.filters,
                genres = emptyList(),
                sources = state.sources,
                actions = actions.panel,
                modifier = Modifier.align(Alignment.CenterStart),
                firstFocus = first,
            )
        }
    }
}

@Composable
private fun QueryField(query: String, modifier: Modifier) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(12.dp)).background(SpinroomColors.Glass).padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(stringResource(R.string.search_hint), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        } else {
            Text(query, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Keyboard(symbols: Boolean, actions: SearchActions, firstKey: FocusRequester, firstResult: FocusRequester, hasResults: Boolean, modifier: Modifier) {
    // → on the right-most keys goes to the first result explicitly: the geometric search would stop on the list container.
    val target = if (hasResults) firstResult else null
    val edge = Modifier.focusJump(Key.DirectionRight, target)
    val bottom = Modifier.focusJump(Key.DirectionDown, target)
    val keys = if (symbols) SymbolKeys else LetterKeys
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(KEY_COLUMNS).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { col, key ->
                    KeyboardKey(key, { actions.onType(key) }, Modifier.weight(1f).then(if (rowIndex == 0 && col == 0) Modifier.focusRequester(firstKey) else Modifier).then(if (col == row.size - 1) edge else Modifier))
                }
                repeat(KEY_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyboardKey(stringResource(R.string.search_space), { actions.onType(" ") }, Modifier.weight(3.2f).then(bottom))
            KeyboardKey(stringResource(R.string.search_delete), actions.onBackspace, Modifier.weight(2f).then(bottom))
            KeyboardKey(stringResource(if (symbols) R.string.search_letters else R.string.search_symbols), actions.onToggleSymbols, Modifier.weight(2f).then(bottom).then(edge))
        }
    }
}

@Composable
private fun KeyboardKey(label: String, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick = onClick, modifier = modifier.height(44.dp), shape = RoundedCornerShape(8.dp), focusedScale = 1.1f,
        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = if (on) SpinroomColors.TextPrimary else SpinroomColors.Glass,
        focusedContainerColor = if (on) SpinroomColors.TextPrimary else SpinroomColors.GlassHigh,
        focusedScale = 1.06f,
    ) {
        Text(
            label,
            color = if (on) SpinroomColors.Bg else SpinroomColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1, softWrap = false,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CategoryChips(state: SearchUiState, actions: SearchActions, filtersChip: FocusRequester, resultsTarget: FocusRequester?) {
    // LazyRow: chips scroll sideways and stop short of the list's scrollbar at the right edge.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 32.dp),
        modifier = Modifier.padding(top = 4.dp).fillMaxWidth().focusJump(Key.DirectionDown, resultsTarget),
    ) {
        items(SearchFilter.entries, key = { it.name }) { filter ->
            Pill(stringResource(filter.labelRes()), filter == state.filter, { actions.onFilter(filter) })
        }
        item(key = "filters") {
            val n = state.filters.activeCount
            Pill(
                stringResource(if (n == 0) R.string.search_filters else R.string.search_filters_count, n), n > 0,
                { actions.onPanel(true) }, Modifier.focusRequester(filtersChip),
            )
        }
    }
}

/** Active filters as chips with ✕; OK on one removes it. */
@Composable
private fun ActiveFilterChips(filters: LibraryFilter, panel: FilterPanelActions) {
    if (filters.isEmpty) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 32.dp),
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
    ) {
        if (filters.favoriteOnly) item("fav") { Pill(stringResource(R.string.search_filter_remove, stringResource(io.github.riberrr.spinroom.feature.library.R.string.filter_favorites_only)), true, panel.onToggleFavorites) }
        filters.decade?.let { d ->
            item("decade") { Pill(stringResource(R.string.search_filter_remove, stringResource(io.github.riberrr.spinroom.feature.library.R.string.filter_decade_format, d)), true, { panel.onToggleDecade(d) }) }
        }
        filters.provider?.let { p ->
            item("source") { Pill(stringResource(R.string.search_filter_remove, SearchViewModel.sourceLabel(p)), true, { panel.onToggleSource(p) }) }
        }
    }
}

private fun SearchFilter.labelRes() = when (this) {
    SearchFilter.ALL -> R.string.search_filter_all
    SearchFilter.RADIO -> R.string.search_filter_radio
    SearchFilter.PLAYLISTS -> R.string.search_filter_playlists
    SearchFilter.ALBUMS -> R.string.search_filter_albums
    SearchFilter.ARTISTS -> R.string.search_filter_artists
    SearchFilter.TRACKS -> R.string.search_filter_tracks
}

@Composable
private fun Results(state: SearchUiState, actions: SearchActions, firstResult: FocusRequester, onSelect: (SearchRow) -> Unit) {
    when {
        state.isIdle -> EmptyState(stringResource(R.string.search_idle_title), message = stringResource(R.string.search_idle_message))
        state.failed -> ErrorState(stringResource(R.string.search_error), onRetry = actions.onRetry)
        state.loading && !state.hasShown -> EmptyState(stringResource(R.string.search_loading))
        !state.hasShown -> EmptyState(stringResource(R.string.search_empty_title), message = stringResource(R.string.search_empty_message))
        else -> ResultList(state, actions, firstResult, onSelect)
    }
}

@Composable
private fun ResultList(state: SearchUiState, actions: SearchActions, firstResult: FocusRequester, onSelect: (SearchRow) -> Unit) {
    val first = listOf(state.shownRadio, state.shownPlaylists, state.shownAlbums, state.shownArtists, state.shownTracks).firstOrNull { it.isNotEmpty() }?.first()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        group("radio", R.string.search_section_radio, state.shownRadio, state, actions, first, firstResult, onSelect)
        group("playlists", R.string.search_section_playlists, state.shownPlaylists, state, actions, first, firstResult, onSelect)
        group("albums", R.string.search_section_albums, state.shownAlbums, state, actions, first, firstResult, onSelect)
        group("artists", R.string.search_section_artists, state.shownArtists, state, actions, first, firstResult, onSelect)
        group("tracks", R.string.search_section_tracks, state.shownTracks, state, actions, first, firstResult, onSelect)
    }
}

private fun LazyListScope.group(
    id: String, title: Int, rows: List<SearchRow>, state: SearchUiState, actions: SearchActions, first: SearchRow?, firstResult: FocusRequester, onSelect: (SearchRow) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "h-$id") { Header(title) }
    items(rows, key = { "$id-${it.uri}-${it.item.provider}" }) { row -> ResultItem(row, state.isFavorite(row), actions, onSelect, if (row === first) Modifier.focusRequester(firstResult) else Modifier) }
}

@Composable
private fun Header(title: Int) {
    Text(
        stringResource(title), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Cover(row: SearchRow, size: Int) {
    val station = row.kind == ResultKind.RADIO
    val shape = if (row.kind == ResultKind.ARTIST) CircleShape else RoundedCornerShape(if (station) 8.dp else 4.dp)
    val bg = if (station) stationColor(row.title) else SpinroomColors.SurfaceHigh
    Box(Modifier.size(size.dp).clip(shape).background(bg), contentAlignment = Alignment.Center) {
        if (row.imageUrl != null) {
            AsyncImage(row.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (station || row.kind == ResultKind.PLAYLIST) {
            AcronymFill(row.title, Modifier.fillMaxSize(), MaterialTheme.typography.titleMedium)
        } else {
            Text(row.title.take(2).uppercase(), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SourceBadge(label: String) {
    Text(
        label, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(SpinroomColors.GlassHigh).padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun kindLabel(row: SearchRow): String = when (row.kind) {
    ResultKind.ARTIST -> stringResource(R.string.search_kind_artist)
    ResultKind.ALBUM -> stringResource(R.string.search_kind_album)
    ResultKind.PLAYLIST -> stringResource(R.string.search_kind_playlist)
    ResultKind.RADIO -> stringResource(R.string.search_kind_radio)
    ResultKind.TRACK -> row.subtitle
}

/**
 * One hit. While it (or one of its buttons) has focus the "Odtwórz" and heart buttons appear to its right;
 * OK = open/play, long OK = toggle favorite, → moves to the buttons.
 */
@Composable
private fun ResultItem(row: SearchRow, favorite: Boolean, actions: SearchActions, onSelect: (SearchRow) -> Unit, surfaceModifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().onFocusChanged {
            selected = it.hasFocus
            if (it.hasFocus) onSelect(row)
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FocusSurface(
            { actions.onOpen(row) }, Modifier.weight(1f).then(surfaceModifier), shape = RoundedCornerShape(12.dp), focusedScale = 1.02f,
            onLongClick = { actions.onFavorite(row) },
            containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Cover(row, 48)
                Column(Modifier.weight(1f)) {
                    Text(row.title, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = kindLabel(row)
                    if (sub.isNotBlank()) {
                        Text(sub, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!selected) {
                    SourceBadge(row.source)
                    if (favorite) FavoriteHeart(true)
                }
            }
        }
        if (selected) {
            // Icon only (like the heart button): the long title stays readable.
            ActionButton(null, primary = true, onClick = { actions.onPlay(row) })
            // Icon only: the row keeps room for its title. Filled heart = already a favorite, OK removes it.
            ActionButton(null, primary = false, favorite = favorite, onClick = { actions.onFavorite(row) })
        }
    }
}

@Composable
private fun ActionButton(label: String?, primary: Boolean, onClick: () -> Unit, favorite: Boolean? = null) {
    FocusSurface(
        onClick, shape = CircleShape, focusedScale = 1.06f,
        containerColor = if (primary) SpinroomColors.Accent else SpinroomColors.Glass,
        focusedContainerColor = if (primary) SpinroomColors.Accent else SpinroomColors.GlassHigh,
    ) {
        Row(Modifier.padding(horizontal = if (label == null) 12.dp else 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (primary) {
                Icon(SpinroomIcons.Play, stringResource(R.string.search_play), Modifier.size(18.dp), tint = SpinroomColors.Bg)
            } else if (favorite != null) {
                FavoriteHeart(favorite, Modifier.size(18.dp))
            }
            if (label != null) Text(
                label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                color = if (primary) SpinroomColors.Bg else SpinroomColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun Toast(toast: FavoriteToast, onUndo: () -> Unit, modifier: Modifier) {
    GlassSurface(modifier, shape = RoundedCornerShape(16.dp), color = SpinroomColors.Surface) {
        Row(Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(if (toast.added) R.string.search_toast_added else R.string.search_toast_removed), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(toast.title, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(240.dp))
            ActionButton(stringResource(R.string.search_undo), primary = false, onClick = onUndo)
        }
    }
}

@Composable
private fun Notice(notice: SearchUiState.Notice, onShown: () -> Unit, modifier: Modifier) {
    LaunchedEffect(notice) {
        delay(NOTICE_MS)
        onShown()
    }
    val text = when (notice) {
        SearchUiState.Notice.NO_VOICE -> R.string.search_notice_no_voice
        SearchUiState.Notice.NO_PLAYER -> R.string.search_notice_no_player
        SearchUiState.Notice.PLAY_FAILED -> R.string.search_notice_play_failed
        SearchUiState.Notice.FAVORITE_FAILED -> R.string.search_notice_fav_failed
    }
    Text(
        stringResource(text), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SpinroomColors.SurfaceHigh).padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

// ---- Previews ----

private fun previewRow(name: String, sub: String, source: String, kind: ResultKind) = SearchRow(
    io.github.riberrr.spinroom.core.maapi.MediaItem(itemId = name, provider = "x", name = name, uri = "x://$name"), kind, name, sub, source, null,
)

private val previewState = SearchUiState(
    query = "jazz",
    radio = listOf(previewRow("Radio Jazz", "", "TuneIn", ResultKind.RADIO)),
    albums = listOf(previewRow("Jazzowy Port", "Kwartet Północ", "Pliki lokalne", ResultKind.ALBUM)),
    tracks = listOf(previewRow("Port 14", "Kwartet Północ · Horyzont", "Pliki lokalne", ResultKind.TRACK)),
    toast = FavoriteToast("x://Radio Jazz", "Radio Jazz", true),
)

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun SearchResultsPreview() = SpinroomTheme { SearchContent(previewState, SearchActions()) }

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun SearchIdlePreview() = SpinroomTheme { SearchContent(SearchUiState(), SearchActions()) }
