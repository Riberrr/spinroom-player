package io.github.riberrr.spinroom.feature.radio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.riberrr.spinroom.core.design.focusJump
import androidx.compose.ui.focus.focusRestorer
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
import io.github.riberrr.spinroom.core.data.Station
import io.github.riberrr.spinroom.core.design.ContentBackdrop
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.EntryThumb
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.KeyboardLabels
import io.github.riberrr.spinroom.core.design.OnScreenKeyboard
import io.github.riberrr.spinroom.core.design.ResultRow
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.design.StationTile
import io.github.riberrr.spinroom.core.design.stationColor
import io.github.riberrr.spinroom.core.design.AcronymFill
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.RadioFilter
import io.github.riberrr.spinroom.core.maapi.RadioFilterKind
import kotlinx.coroutines.delay

private const val NOTICE_MS = 2_500L

/** Everything the screen can trigger; the ViewModel implements it, previews pass no-ops. */
data class RadioActions(
    val onPlay: (String) -> Unit = {},
    val onToggleFavorite: (RadioRow) -> Unit = {},
    val onOpenMenu: (Station) -> Unit = {},
    val onCloseMenu: () -> Unit = {},
    val onStartMove: () -> Unit = {},
    val onStopMove: () -> Unit = {},
    val onMoveBy: (Int) -> Unit = {},
    val onRemove: () -> Unit = {},
    val onOpenPicker: (RadioFilterKind) -> Unit = {},
    val onClosePicker: () -> Unit = {},
    val onPick: (RadioFilterKind, String?, String?) -> Unit = { _, _, _ -> },
    val onPopular: () -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onCloseSearch: () -> Unit = {},
    val onType: (String) -> Unit = {},
    val onBackspace: () -> Unit = {},
    val onToggleSymbols: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRetrySearch: () -> Unit = {},
    val onNoticeShown: () -> Unit = {},
    val onTogglePolishOnly: () -> Unit = {},
    val onAllStations: () -> Unit = {},
    val onNearEnd: () -> Unit = {},
) {
    companion object {
        fun of(vm: RadioViewModel) = RadioActions(
            vm::play, vm::toggleFavorite, vm::openMenu, vm::closeMenu, vm::startMove, vm::stopMove, vm::moveBy, vm::removeMenuStation,
            vm::openPicker, vm::closePicker, vm::pickOption, vm::onPopular, vm::openSearch, vm::closeSearch,
            vm::onType, vm::onBackspace, vm::onToggleSymbols, vm::onRetry, vm::onRetrySearch, vm::onNoticeShown, vm::onTogglePolishOnly,
            vm::onAllStations, vm::onNearEnd,
        )
    }
}

/** Radio screen (route `radio`). The navigation callbacks are kept for the shell's signature; radio plays in place. */
@Composable
fun RadioScreen(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: RadioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RadioContent(state, RadioActions.of(viewModel), modifier)
}

private fun isTypedChar(event: androidx.compose.ui.input.key.KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed &&
        event.utf16CodePoint >= 32 && event.utf16CodePoint != 127

/** Stateless UI. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RadioContent(state: RadioUiState, actions: RadioActions, modifier: Modifier = Modifier) {
    var backdropName by remember { mutableStateOf<String?>(null) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    var searchFieldFocused by remember { mutableStateOf(false) }
    // Focus below the favorites (search box, chips, station grid) shrinks the favorites bar to small numbered icons.
    var lowerFocused by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }
    val searchField = remember { FocusRequester() }
    val favoritesRow = remember { FocusRequester() }
    val gridFirst = remember { FocusRequester() }
    val menuFirst = remember { FocusRequester() }
    val pickerFirst = remember { FocusRequester() }
    val searching = state.searching
    val resultsTarget = if ((if (searching) state.searchRows else state.rows).isNotEmpty()) gridFirst else null

    LaunchedEffect(searching) {
        if (searching) runCatching { firstKey.requestFocus() } else if (backdropName == null) Unit else runCatching { searchField.requestFocus() }
    }
    LaunchedEffect(state.menuStation) { if (state.menuStation != null) runCatching { menuFirst.requestFocus() } }
    LaunchedEffect(state.picker) { if (state.picker != null) runCatching { pickerFirst.requestFocus() } }

    // Innermost first: the last enabled handler wins.
    BackHandler(searching) { actions.onCloseSearch() }
    BackHandler(state.picker != null) {
        actions.onClosePicker()
        runCatching { searchField.requestFocus() }
    }
    BackHandler(state.menuStation != null) {
        actions.onCloseMenu()
        runCatching { favoritesRow.requestFocus() }
    }
    BackHandler(state.movingUri != null) { actions.onStopMove() }

    Box(
        modifier.fillMaxSize().onPreviewKeyEvent { e ->
            when {
                state.movingUri != null -> when {
                    e.key == Key.DirectionLeft || e.key == Key.DirectionRight -> {
                        if (e.type == KeyEventType.KeyDown) actions.onMoveBy(if (e.key == Key.DirectionLeft) -1 else 1)
                        true
                    }
                    e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter -> {
                        if (e.type == KeyEventType.KeyDown) actions.onStopMove()
                        true
                    }
                    else -> false
                }
                // A physical keyboard types straight into the search, also from the (unopened) field.
                (searching || searchFieldFocused) && e.type != KeyEventType.Unknown && (e.key == Key.Backspace || e.key == Key.Delete) -> {
                    if (e.type == KeyEventType.KeyDown) actions.onBackspace()
                    true
                }
                (searching || searchFieldFocused) && isTypedChar(e) -> {
                    if (!searching) actions.onOpenSearch()
                    actions.onType(String(Character.toChars(e.utf16CodePoint)))
                    true
                }
                else -> false
            }
        },
    ) {
        ContentBackdrop(imageUrl = backdropUrl, seedColor = backdropName?.let { stationColor(it) })

        Row(Modifier.fillMaxSize().padding(start = 32.dp, top = 28.dp, end = 40.dp)) {
            AnimatedVisibility(
                visible = searching,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut(),
            ) {
                SearchPane(state, actions, firstKey, resultsTarget, Modifier.padding(end = 32.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (searching) {
                    Row(Modifier.focusJump(Key.DirectionDown, resultsTarget).padding(bottom = 14.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusSurface(
                            onClick = actions.onTogglePolishOnly, shape = CircleShape, focusedScale = 1.06f,
                            containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.radio_chip_language) + ": ", color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    stringResource(if (state.polishOnly) R.string.radio_lang_polish else R.string.radio_chip_all),
                                    color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.radio_stations_count, state.searchRows.size),
                            color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Header(state)
                    Favorites(state, actions, favoritesRow, compact = lowerFocused) { name, url -> backdropName = name; backdropUrl = url }
                }
                Column(Modifier.weight(1f).fillMaxWidth().onFocusChanged { lowerFocused = it.hasFocus }) {
                    if (!searching) Controls(state, actions, searchField, resultsTarget) { searchFieldFocused = it }
                    val rows = if (searching) state.searchRows else state.rows
                    StationList(
                        state = state, rows = rows, gridFirst = gridFirst, columns = if (searching) 1 else 2, actions = actions,
                        onFocus = { name, url -> backdropName = name; backdropUrl = url },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }

        state.picker?.let { Picker(state, it, actions, pickerFirst, Modifier.align(Alignment.Center)) }
        state.menuStation?.let { TileMenu(actions, menuFirst, Modifier.align(Alignment.Center)) }
        state.notice?.let { Notice(it, actions.onNoticeShown, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) }
    }
}

@Composable
private fun Header(state: RadioUiState) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 10.dp)) {
        Text(stringResource(R.string.radio_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = SpinroomColors.TextPrimary)
        Text(
            stringResource(R.string.radio_favorites_count, state.favorites.size),
            Modifier.padding(start = 16.dp, bottom = 6.dp),
            style = MaterialTheme.typography.bodyLarge, color = SpinroomColors.TextSecondary,
        )
        Box(Modifier.weight(1f))
        Text(
            stringResource(R.string.radio_hint_hold), Modifier.padding(bottom = 6.dp), maxLines = 1,
            color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Favorites(
    state: RadioUiState,
    actions: RadioActions,
    rowRequester: FocusRequester,
    compact: Boolean,
    onFocus: (String?, String?) -> Unit,
) {
    val t by animateFloatAsState(if (compact) 1f else 0f, tween(200), label = "favoritesCompact")
    if (state.favorites.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            EmptyState(stringResource(R.string.radio_no_favorites_title), message = stringResource(R.string.radio_no_favorites_message))
        }
        return
    }
    LazyRow(
        Modifier.fillMaxWidth().focusRequester(rowRequester).focusRestorer(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = (14 - 8 * t).dp),
        horizontalArrangement = Arrangement.spacedBy((14 - 8 * t).dp),
    ) {
        itemsIndexed(state.favorites, key = { _, s -> s.uri }) { index, station ->
            val moving = state.movingUri == station.uri
            val size = (112 - 66 * t).dp
            // Captions (below the square) are clipped away as the bar shrinks; the tile stays the same focusable node.
            Box(Modifier.height(size + (44 * (1 - t)).dp).clip(FocusRoomClip)) {
                StationTile(
                    name = station.name,
                    subtitle = if (moving) stringResource(R.string.radio_moving) else "",
                    onClick = { if (state.movingUri == null) actions.onPlay(station.uri) },
                    modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                        .onFocusChanged { if (it.isFocused) onFocus(station.name, station.imageUrl) },
                    size = size,
                    onLongClick = { if (state.movingUri == null) actions.onOpenMenu(station) },
                    logo = { NumberedLogo(index + 1, station.name, station.imageUrl) },
                )
            }
        }
    }
}

private const val NEAR_END = 12

/** Clip that leaves room around a tile for its focus scale and border (captions start 10 dp below the tile). */
private val FocusRoomClip = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val m = with(density) { 16.dp.toPx() }
        return Outline.Rectangle(Rect(-m, -m, size.width + m, size.height + with(density) { 6.dp.toPx() }))
    }
}

@Composable
private fun NumberedLogo(number: Int, name: String, imageUrl: String?) {
    Box(Modifier.fillMaxSize()) {
        if (imageUrl != null) AsyncImage(imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else AcronymFill(name, Modifier.fillMaxSize(), MaterialTheme.typography.headlineMedium)
        Box(
            Modifier.padding(8.dp).size(28.dp).clipCircle().background(SpinroomColors.Bg.copy(alpha = 0.7f)).align(Alignment.TopStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SpinroomColors.TextPrimary)
        }
    }
}

private fun Modifier.clipCircle() = this.then(Modifier.clip(CircleShape))

@Composable
private fun Controls(state: RadioUiState, actions: RadioActions, searchField: FocusRequester, downTarget: FocusRequester?, onSearchFocus: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().focusJump(Key.DirectionDown, downTarget).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FocusSurface(
            onClick = actions.onOpenSearch,
            modifier = Modifier.width(180.dp).height(44.dp).focusRequester(searchField).onFocusChanged { onSearchFocus(it.isFocused) },
            containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.03f,
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(SpinroomIcons.Search, null, tint = SpinroomColors.TextSecondary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.radio_search_hint), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
        }
        val all = stringResource(R.string.radio_chip_all)
        Chip(RadioFilterKind.LANGUAGE, stringResource(R.string.radio_chip_language), state, all, actions)
        Chip(RadioFilterKind.TAG, stringResource(R.string.radio_chip_genre), state, all, actions)
        Chip(RadioFilterKind.COUNTRY, stringResource(R.string.radio_chip_country), state, all, actions)
        Box(Modifier.weight(1f))
        val mine = state.libraryMode
        FocusSurface(
            onClick = actions.onAllStations, shape = CircleShape, focusedScale = 1.06f,
            containerColor = if (mine) SpinroomColors.TextPrimary else SpinroomColors.Glass,
            focusedContainerColor = if (mine) SpinroomColors.TextPrimary else SpinroomColors.GlassHigh,
        ) {
            Text(
                stringResource(R.string.radio_all_stations), Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = if (mine) SpinroomColors.Bg else SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, softWrap = false,
            )
        }
        val popular = !state.libraryMode && state.filter == RadioFilter.Popular
        FocusSurface(
            onClick = actions.onPopular, shape = CircleShape, focusedScale = 1.06f,
            containerColor = if (popular) SpinroomColors.TextPrimary else SpinroomColors.Glass,
            focusedContainerColor = if (popular) SpinroomColors.TextPrimary else SpinroomColors.GlassHigh,
        ) {
            Text(
                stringResource(R.string.radio_popular), Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = if (popular) SpinroomColors.Bg else SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, softWrap = false,
            )
        }
    }
}

/** The server filters by one dimension at a time, so only the chip of the active filter shows a value. */
@Composable
private fun Chip(kind: RadioFilterKind, label: String, state: RadioUiState, all: String, actions: RadioActions) {
    val active = when (kind) {
        RadioFilterKind.LANGUAGE -> !state.libraryMode && state.filter is RadioFilter.Language
        RadioFilterKind.COUNTRY -> !state.libraryMode && state.filter is RadioFilter.Country
        RadioFilterKind.TAG -> !state.libraryMode && state.filter is RadioFilter.Tag
    }
    FocusSurface(
        onClick = { actions.onOpenPicker(kind) }, shape = CircleShape, focusedScale = 1.06f,
        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$label: ", color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
            Text(
                if (active) state.filterLabel else all, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
            )
        }
    }
}

@Composable
private fun StationList(
    state: RadioUiState,
    rows: List<RadioRow>,
    gridFirst: FocusRequester,

    columns: Int,
    actions: RadioActions,
    onFocus: (String?, String?) -> Unit,
    modifier: Modifier,
) {
    val searching = state.searching
    when {
        searching && state.query.isBlank() -> Box(modifier, Alignment.Center) { EmptyState(stringResource(R.string.radio_search_idle)) }
        searching && state.searchFailed -> Box(modifier, Alignment.Center) { ErrorState(stringResource(R.string.radio_error), retryLabel = stringResource(R.string.radio_retry), onRetry = actions.onRetrySearch) }
        searching && rows.isEmpty() && state.searchLoading -> Box(modifier, Alignment.Center) { EmptyState(stringResource(R.string.radio_loading)) }
        searching && rows.isEmpty() -> Box(modifier, Alignment.Center) { EmptyState(stringResource(R.string.radio_search_empty)) }
        !searching && state.failed -> Box(modifier, Alignment.Center) { ErrorState(stringResource(R.string.radio_error), retryLabel = stringResource(R.string.radio_retry), onRetry = actions.onRetry) }
        !searching && rows.isEmpty() && state.loading -> Box(modifier, Alignment.Center) { EmptyState(stringResource(R.string.radio_loading)) }
        !searching && rows.isEmpty() -> Box(modifier, Alignment.Center) { EmptyState(stringResource(R.string.radio_empty_title), message = stringResource(R.string.radio_empty_message)) }
        else -> LazyVerticalGrid(
            GridCells.Fixed(columns), modifier,
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(rows, key = { _, r -> r.uri.ifEmpty { r.name } }) { index, row ->
                ResultRow(
                    title = row.name,
                    subtitle = row.subtitle.ifBlank { stringResource(R.string.radio_provider) },
                    onClick = { actions.onPlay(row.uri) },
                    modifier = (if (index == 0) Modifier.focusRequester(gridFirst) else Modifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                onFocus(row.name, row.imageUrl)
                                if (index >= rows.size - NEAR_END) actions.onNearEnd()
                            }
                        },
                    favorite = state.isFavorite(row),
                    onLongClick = { actions.onToggleFavorite(row) },
                    thumbnail = { EntryThumb(row.name, row.imageUrl, isStation = true, modifier = Modifier.fillMaxSize()) },
                )
            }
        }
    }
}

/** Search mode, left: breadcrumb, query box ("tylko radio") and the on-screen keyboard. */
@Composable
private fun SearchPane(state: RadioUiState, actions: RadioActions, firstKey: FocusRequester, rightTarget: FocusRequester?, modifier: Modifier) {
    Column(modifier.width(400.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Text(stringResource(R.string.radio_search_crumb) + " › ", color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.radio_search_title), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        GlassSurface(Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), high = true) {
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.query.isEmpty()) stringResource(R.string.radio_search_hint) else state.query,
                    Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (state.query.isEmpty()) SpinroomColors.TextSecondary else SpinroomColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.radio_search_only), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        OnScreenKeyboard(
            symbols = state.symbols,
            labels = KeyboardLabels(
                stringResource(R.string.radio_space), stringResource(R.string.radio_delete),
                stringResource(R.string.radio_symbols), stringResource(R.string.radio_letters),
            ),
            onType = actions.onType, onBackspace = actions.onBackspace, onToggleSymbols = actions.onToggleSymbols,
            modifier = Modifier.padding(top = 14.dp), firstKey = firstKey, resultsTarget = rightTarget,
        )
    }
}

/** Filter value list for one chip. The first row sets "all" (= popular list). */
@Composable
private fun Picker(state: RadioUiState, kind: RadioFilterKind, actions: RadioActions, first: FocusRequester, modifier: Modifier) {
    val title = when (kind) {
        RadioFilterKind.LANGUAGE -> R.string.radio_chip_pick_language
        RadioFilterKind.TAG -> R.string.radio_chip_pick_genre
        RadioFilterKind.COUNTRY -> R.string.radio_chip_pick_country
    }
    GlassSurface(modifier.width(380.dp).height(520.dp), shape = RoundedCornerShape(16.dp), high = true) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SpinroomColors.TextPrimary)
            val options = state.options[kind]
            LazyColumn(Modifier.padding(top = 12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item(key = "all") { PickerRow(stringResource(R.string.radio_all_option), Modifier.focusRequester(first)) { actions.onPick(kind, null, null) } }
                if (options == null) item(key = "loading") {
                    Text(stringResource(R.string.radio_options_loading), Modifier.padding(12.dp), color = SpinroomColors.TextSecondary)
                } else items(options, key = { it.key }) { o ->
                    val label = if (kind == RadioFilterKind.LANGUAGE) languageOptionLabel(o.name) else o.name
                    PickerRow(label, Modifier) { actions.onPick(kind, o.key, o.name) }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, modifier: Modifier, onClick: () -> Unit) {
    FocusSurface(
        onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), focusedScale = 1.02f,
        containerColor = Color.Transparent, focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    }
}

/** Hold OK on a favorite tile: Przesuń / Usuń z ulubionych. */
@Composable
private fun TileMenu(actions: RadioActions, first: FocusRequester, modifier: Modifier) {
    GlassSurface(modifier.width(320.dp), shape = RoundedCornerShape(16.dp), high = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerRow(stringResource(R.string.radio_menu_move), Modifier.focusRequester(first), actions.onStartMove)
            PickerRow(stringResource(R.string.radio_menu_remove), Modifier, actions.onRemove)
            Text(stringResource(R.string.radio_move_hint), Modifier.padding(horizontal = 6.dp, vertical = 4.dp), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Notice(notice: RadioNotice, onShown: () -> Unit, modifier: Modifier) {
    LaunchedEffect(notice) {
        delay(NOTICE_MS)
        onShown()
    }
    val text = when (notice) {
        RadioNotice.ADDED -> R.string.radio_notice_added
        RadioNotice.REMOVED -> R.string.radio_notice_removed
        RadioNotice.NO_PLAYER -> R.string.radio_notice_no_player
        RadioNotice.FAILED -> R.string.radio_notice_failed
    }
    GlassSurface(modifier, shape = RoundedCornerShape(12.dp), high = true) {
        Text(stringResource(text), Modifier.padding(horizontal = 24.dp, vertical = 14.dp), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

// ---- Previews ----

private fun previewRow(name: String, sub: String) = RadioRow(MediaItem(itemId = name, provider = "radiobrowser", name = name, uri = "radiobrowser://radio/$name"), name, null, sub)

private val previewState = RadioUiState(
    favorites = listOf("Jazz Jeden", "Klasyka 24", "Lo-fi Studio", "Rock Wieczorem", "Info Non-Stop").mapIndexed { i, n -> Station("library://radio/$i", n, null) },
    filterLabel = "polski",
    rows = listOf("Spokojne Fale", "Radio 357", "Nowy Świat", "Radio Jazz").map { previewRow(it, "polski") },
    loading = false,
)

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun RadioPreview() = SpinroomTheme { RadioContent(previewState, RadioActions()) }

@Preview(widthDp = 1280, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun RadioSearchPreview() = SpinroomTheme {
    RadioContent(previewState.copy(searching = true, query = "jaz", searchRows = previewState.rows), RadioActions())
}
