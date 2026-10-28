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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme
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
    val onVoice: () -> Unit = {},
    val onRetry: () -> Unit = {},
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
            onVoice = onVoice,
            onRetry = vm::onRetry,
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

/** Stateless UI. Left: query, mic, keyboard. Right: filter chips and grouped results. */
@Composable
fun SearchContent(
    state: SearchUiState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    onNoticeShown: () -> Unit = {},
) {
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    Row(
        modifier.fillMaxSize().background(SpinroomColors.Bg).padding(start = 48.dp, top = 36.dp, end = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(Modifier.width(380.dp)) {
            Text(stringResource(R.string.search_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                QueryField(state.query, Modifier.weight(1f))
                FocusSurface(
                    onClick = actions.onVoice,
                    modifier = Modifier.size(52.dp),
                    focusedScale = 1.08f,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(MicIcon, stringResource(R.string.search_voice), tint = SpinroomColors.Accent, modifier = Modifier.size(26.dp))
                    }
                }
            }
            Keyboard(state.symbols, actions, firstKey, Modifier.padding(top = 16.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            FilterChips(state.filter, actions.onFilter)
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                Results(state, actions)
                state.notice?.let { Notice(it, onNoticeShown, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) }
            }
        }
    }
}

@Composable
private fun QueryField(query: String, modifier: Modifier) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(12.dp)).background(SpinroomColors.Surface).padding(horizontal = 20.dp),
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
private fun Keyboard(symbols: Boolean, actions: SearchActions, firstKey: FocusRequester, modifier: Modifier) {
    val keys = if (symbols) SymbolKeys else LetterKeys
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(KEY_COLUMNS).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { col, key ->
                    KeyboardKey(key, { actions.onType(key) }, Modifier.weight(1f).then(if (rowIndex == 0 && col == 0) Modifier.focusRequester(firstKey) else Modifier))
                }
                repeat(KEY_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyboardKey(stringResource(R.string.search_space), { actions.onType(" ") }, Modifier.weight(3.2f))
            KeyboardKey(stringResource(R.string.search_delete), actions.onBackspace, Modifier.weight(2f))
            KeyboardKey(stringResource(if (symbols) R.string.search_letters else R.string.search_symbols), actions.onToggleSymbols, Modifier.weight(2f))
        }
    }
}

@Composable
private fun KeyboardKey(label: String, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(onClick = onClick, modifier = modifier.height(44.dp), shape = RoundedCornerShape(8.dp), focusedScale = 1.1f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun FilterChips(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        SearchFilter.entries.forEach { filter ->
            val on = filter == selected
            FocusSurface(
                onClick = { onSelect(filter) },
                shape = CircleShape,
                containerColor = if (on) SpinroomColors.TextPrimary else SpinroomColors.Surface,
                focusedContainerColor = if (on) SpinroomColors.TextPrimary else SpinroomColors.SurfaceHigh,
                focusedScale = 1.06f,
            ) {
                Text(
                    stringResource(filter.labelRes()),
                    color = if (on) SpinroomColors.Bg else SpinroomColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private fun SearchFilter.labelRes() = when (this) {
    SearchFilter.ALL -> R.string.search_filter_all
    SearchFilter.ARTISTS -> R.string.search_filter_artists
    SearchFilter.ALBUMS -> R.string.search_filter_albums
    SearchFilter.TRACKS -> R.string.search_filter_tracks
    SearchFilter.RADIO -> R.string.search_filter_radio
}

@Composable
private fun Results(state: SearchUiState, actions: SearchActions) {
    when {
        state.isIdle -> EmptyState(stringResource(R.string.search_idle_title), message = stringResource(R.string.search_idle_message))
        state.failed -> ErrorState(stringResource(R.string.search_error), onRetry = actions.onRetry)
        state.loading && !state.hasShown -> EmptyState(stringResource(R.string.search_loading))
        !state.hasShown -> EmptyState(stringResource(R.string.search_empty_title), message = stringResource(R.string.search_empty_message))
        else -> ResultList(state, actions)
    }
}

@Composable
private fun ResultList(state: SearchUiState, actions: SearchActions) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.best?.let { best ->
            item(key = "h-best") { Header(R.string.search_section_best) }
            item(key = "best") { BestCard(best) { actions.onOpen(best) } }
        }
        group("artists", R.string.search_section_artists, state.shownArtists, actions)
        group("albums", R.string.search_section_albums, state.shownAlbums, actions)
        group("playlists", R.string.search_section_playlists, state.shownPlaylists, actions)
        group("tracks", R.string.search_section_tracks, state.shownTracks, actions)
        group("radio", R.string.search_section_radio, state.shownRadio, actions)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.group(id: String, title: Int, rows: List<SearchRow>, actions: SearchActions) {
    if (rows.isEmpty()) return
    item(key = "h-$id") { Header(title) }
    items(rows, key = { "$id-${it.uri}-${it.item.provider}" }) { row -> ResultRow(row) { actions.onOpen(row) } }
}

@Composable
private fun Header(title: Int) {
    Text(
        stringResource(title), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Cover(row: SearchRow, size: Int, round: Boolean) {
    val shape = if (round) CircleShape else RoundedCornerShape(4.dp)
    Box(Modifier.size(size.dp).clip(shape).background(SpinroomColors.SurfaceHigh), contentAlignment = Alignment.Center) {
        if (row.imageUrl != null) {
            AsyncImage(row.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(row.title.take(2).uppercase(), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SourceBadge(label: String) {
    Text(
        label, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(SpinroomColors.SurfaceHigh).padding(horizontal = 12.dp, vertical = 5.dp),
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

@Composable
private fun BestCard(row: SearchRow, onClick: () -> Unit) {
    FocusSurface(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), focusedScale = 1.02f) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Cover(row, 72, round = row.kind == ResultKind.ARTIST)
            Column(Modifier.weight(1f)) {
                Text(row.title, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOf(kindLabel(row), if (row.kind == ResultKind.TRACK) "" else row.subtitle).filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SourceBadge(row.source)
        }
    }
}

@Composable
private fun ResultRow(row: SearchRow, onClick: () -> Unit) {
    FocusSurface(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), focusedScale = 1.02f) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Cover(row, 36, round = row.kind == ResultKind.ARTIST)
            Column(Modifier.weight(1f)) {
                Text(row.title, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = kindLabel(row)
                if (sub.isNotBlank()) {
                    Text(sub, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            SourceBadge(row.source)
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
    query = "kwartet",
    artists = listOf(previewRow("Kwartet Północ", "", "Pliki lokalne", ResultKind.ARTIST)),
    tracks = listOf(
        previewRow("Port 14", "Kwartet Północ · Horyzont", "Pliki lokalne", ResultKind.TRACK),
        previewRow("Północny wiatr", "Kwartet Północ · Horyzont", "Pliki lokalne", ResultKind.TRACK),
        previewRow("Kwartet na żywo (sesja)", "Kwartet Północ", "YouTube Music", ResultKind.TRACK),
        previewRow("Kwartet smyczkowy nr 2", "Orkiestra Miejska", "YouTube Music", ResultKind.TRACK),
    ),
)

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun SearchResultsPreview() = SpinroomTheme { SearchContent(previewState, SearchActions()) }

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun SearchIdlePreview() = SpinroomTheme { SearchContent(SearchUiState(), SearchActions()) }
