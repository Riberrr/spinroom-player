package io.github.riberrr.spinroom.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import io.github.riberrr.spinroom.core.design.ContentBackdrop
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.CoverTile
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FavoriteHeart
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.ItemMenuHost
import io.github.riberrr.spinroom.core.design.LocalItemMenu
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import kotlinx.coroutines.delay

/** What the grid area shows. */
enum class LibraryStatus { NoServer, Connecting, Loading, Content, Empty, Error }

/** Picks the grid-area state. Connection problems win over list state, but a list already on screen stays while reconnecting. */
internal fun libraryStatus(connection: MaConnectionState, refresh: LoadState, itemCount: Int): LibraryStatus = when {
    connection is MaConnectionState.Failed -> LibraryStatus.Error
    connection is MaConnectionState.Disconnected -> LibraryStatus.NoServer
    connection is MaConnectionState.Connected && !connection.authenticated -> LibraryStatus.NoServer
    connection !is MaConnectionState.Connected && itemCount == 0 -> LibraryStatus.Connecting
    connection !is MaConnectionState.Connected -> LibraryStatus.Content
    refresh is LoadState.Error && itemCount == 0 -> LibraryStatus.Error
    refresh is LoadState.Loading && itemCount == 0 -> LibraryStatus.Loading
    itemCount == 0 -> LibraryStatus.Empty
    else -> LibraryStatus.Content
}

data class LibraryActions(
    val onSelectTab: (LibraryTab) -> Unit = {},
    val onJumpToLetter: (String) -> Unit = {},
    val onOpenItem: (LibraryRow) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onFocused: (LibraryFocus) -> Unit = {},
    val onPreview: (LibraryRow?) -> Unit = {},
    val onSetListMode: (Boolean) -> Unit = {},
    val onPlay: (LibraryRow, Boolean) -> Unit = { _, _ -> },
    val onToggleFavorite: (LibraryRow) -> Unit = {},
    val filter: FilterPanelActions = FilterPanelActions(),
)

/** Library (route `library`). [onOpenItem] gets a Music Assistant URI such as `library://album/12`. */
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onOpenItem: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    var jumpTarget by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(viewModel) { viewModel.scrollTo.collect { jumpTarget = it } }
    // A new tab or filter starts at the top.
    LaunchedEffect(state.tab, state.filter) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }

    val status = libraryStatus(state.connection, items.loadState.refresh, items.itemCount)

    ItemMenuHost { LibraryContent(
        state = state,
        status = status,
        itemCount = items.itemCount,
        itemAt = { index -> items[index] },
        actions = LibraryActions(
            onSelectTab = viewModel::onSelectTab,
            onJumpToLetter = viewModel::onJumpToLetter,
            onOpenItem = { onOpenItem(it.uri) },
            onRetry = { items.retry(); items.refresh() },
            onFocused = viewModel::onFocused,
            onPreview = viewModel::onPreview,
            onSetListMode = viewModel::onSetListMode,
            onPlay = viewModel::onPlay,
            onToggleFavorite = viewModel::onToggleFavorite,
            filter = FilterPanelActions(
                onToggleGenre = viewModel::onToggleGenre,
                onToggleDecade = viewModel::onToggleDecade,
                onToggleSource = viewModel::onToggleSource,
                onToggleFavorites = viewModel::onToggleFavoritesOnly,
                onClear = viewModel::onClearFilters,
            ),
        ),
        modifier = modifier,
        gridState = gridState,
        listState = listState,
        jumpTarget = jumpTarget,
        onJumpHandled = { jumpTarget = null },
    ) }
}

@Composable
fun LibraryContent(
    state: LibraryUiState,
    status: LibraryStatus,
    itemCount: Int,
    itemAt: (Int) -> LibraryRow?,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    listState: LazyListState = rememberLazyListState(),
    jumpTarget: Int? = null,
    onJumpHandled: () -> Unit = {},
) {
    val tabFocus = remember { FocusRequester() }
    val itemFocus = remember { FocusRequester() }
    val jumpFocus = remember { FocusRequester() }
    val filterChipFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    // Focus as it was when the screen was entered: after Back we return to the last tile, not to the first chip.
    val restoreIndex = remember { (state.focus as? LibraryFocus.Item)?.index }
    var restored by remember { mutableStateOf(restoreIndex == null) }
    var filterOpen by remember { mutableStateOf(false) }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    val hasContent = status == LibraryStatus.Content
    val listMode = state.listMode

    LaunchedEffect(Unit) { if (restoreIndex == null) runCatching { tabFocus.requestFocus() } }
    // The grid fills in after the connection is up; restore once the target tile exists.
    LaunchedEffect(status, itemCount) {
        if (!restored && restoreIndex != null && status == LibraryStatus.Content && itemCount > restoreIndex) {
            if (listMode) listState.scrollToItem(restoreIndex) else gridState.scrollToItem(restoreIndex)
            withFrameNanos { }
            restored = runCatching { itemFocus.requestFocus() }.isSuccess
        }
    }
    // Letter jump (rail or keyboard): scroll, then put focus on the target once its row exists.
    LaunchedEffect(jumpTarget) {
        val target = jumpTarget ?: return@LaunchedEffect
        if (listMode) listState.scrollToItem(target) else gridState.scrollToItem(target)
        for (attempt in 0 until 10) {
            withFrameNanos { }
            if (runCatching { jumpFocus.requestFocus() }.isSuccess) break
            delay(100)
        }
        onJumpHandled()
    }
    LaunchedEffect(filterOpen) {
        if (filterOpen) runCatching { panelFocus.requestFocus() }
    }

    fun closePanel() {
        filterOpen = false
        runCatching { filterChipFocus.requestFocus() }
    }

    val canJump = hasContent && state.filter.isEmpty && !filterOpen
    Box(
        modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e -> if (canJump) letterOf(e)?.let { actions.onJumpToLetter(it.toString()); true } ?: false else false },
    ) {
        ContentBackdrop(imageUrl = if (hasContent) backdropUrl else null)

        Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 32.dp, top = 32.dp)) {
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary)
            Row(
                Modifier.padding(top = 20.dp, bottom = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryTab.entries.forEach { tab ->
                    val selected = tab == state.tab
                    val shown = libraryCountLabel(state.count, itemCount, hasContent, status == LibraryStatus.Empty)
                    val label = stringResource(tab.labelRes) + if (selected && shown != null && state.filter.isEmpty) " · $shown" else ""
                    var chip: Modifier = Modifier.onFocusChanged { if (it.isFocused) actions.onFocused(LibraryFocus.Tab) }
                    if (selected) chip = chip.focusRequester(tabFocus)
                    TabChip(label, selected, actions.onSelectTab.let { { it(tab) } }, chip)
                }
                Spacer(Modifier.weight(1f))
                if (!state.filter.isEmpty) {
                    FilterChip(stringResource(R.string.filter_clear), false, actions.filter.onClear)
                }
                FilterChip(
                    stringResource(R.string.filter_button) + if (state.filter.activeCount > 0) " · ${state.filter.activeCount}" else "",
                    state.filter.activeCount > 0, { filterOpen = true }, Modifier.focusRequester(filterChipFocus),
                )
                FilterChip(stringResource(R.string.library_view_grid), !listMode, { actions.onSetListMode(false) })
                FilterChip(stringResource(R.string.library_view_list), listMode, { actions.onSetListMode(true) })
            }
            Box(Modifier.fillMaxSize()) {
                when (status) {
                    LibraryStatus.NoServer -> EmptyState(
                        title = stringResource(R.string.library_no_server_title),
                        message = stringResource(R.string.library_no_server_message),
                    )
                    LibraryStatus.Connecting -> EmptyState(title = stringResource(R.string.library_connecting))
                    LibraryStatus.Loading -> EmptyState(title = stringResource(R.string.library_loading))
                    LibraryStatus.Error -> ErrorState(
                        message = stringResource(R.string.library_error_message),
                        retryLabel = stringResource(R.string.library_retry),
                        onRetry = actions.onRetry,
                    )
                    LibraryStatus.Empty -> EmptyState(
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(
                            if (state.filter.isEmpty) R.string.library_empty_message else R.string.library_empty_filtered,
                        ),
                        actionLabel = if (state.filter.isEmpty) null else stringResource(R.string.filter_clear),
                        onAction = if (state.filter.isEmpty) null else actions.filter.onClear,
                    )
                    LibraryStatus.Content -> Row(Modifier.fillMaxSize()) {
                        val onFocusRow: (Int, LibraryRow) -> Unit = { index, row ->
                            actions.onFocused(LibraryFocus.Item(index))
                            actions.onPreview(row)
                            backdropUrl = row.coverUrl
                        }
                        if (listMode) {
                            ListPane(
                                itemCount, itemAt, actions.onOpenItem, listState, Modifier.weight(1f),
                                restoreIndex, itemFocus, jumpTarget, jumpFocus, onFocusRow, { filterOpen = true },
                            )
                            PreviewPane(state.preview, actions, Modifier.width(PREVIEW_WIDTH))
                        } else {
                            CoverGrid(
                                itemCount, itemAt, actions.onOpenItem, gridState, Modifier.weight(1f),
                                restoreIndex, itemFocus, jumpTarget, jumpFocus, onFocusRow, { filterOpen = true },
                            )
                            // The letter rail only makes sense when there is something to jump through.
                            if (itemCount > 0 && state.filter.isEmpty) LetterRail(actions.onJumpToLetter)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = filterOpen,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            FilterPanel(
                filter = state.filter,
                genres = state.genres,
                sources = state.sources,
                actions = actions.filter.copy(onClose = ::closePanel),
                firstFocus = panelFocus,
            )
        }
    }
}

private val PREVIEW_WIDTH = 460.dp

/** Physical keyboard: an unmodified letter key (A–Z) on key-down gives the letter, else null. */
internal fun letterOf(e: KeyEvent): Char? {
    if (e.type != KeyEventType.KeyDown) return null
    val code = e.key.keyCode
    // Compose Key.A..Key.Z are consecutive.
    val a = Key.A.keyCode
    return if (code in a..(a + 25)) 'A' + (code - a).toInt() else null
}

/** Count for the selected chip. A finished load with no items shows 0 (the server counter may include artists it will not list). Once the grid shows items its (paged) size is the truth, so a stray 0 from the counter cannot win. */
internal fun libraryCountLabel(count: Int?, itemCount: Int, hasContent: Boolean, loadedEmpty: Boolean = false): Int? =
    if (loadedEmpty) 0 else
    if (hasContent && itemCount > 0) itemCount else count

private val LibraryTab.labelRes: Int
    get() = when (this) {
        LibraryTab.Artists -> R.string.library_tab_artists
        LibraryTab.Albums -> R.string.library_tab_albums
        LibraryTab.Tracks -> R.string.library_tab_tracks
        LibraryTab.Playlists -> R.string.library_tab_playlists
    }

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        containerColor = if (selected) SpinroomColors.TextPrimary else SpinroomColors.Glass,
        focusedContainerColor = if (selected) SpinroomColors.TextPrimary else SpinroomColors.GlassHigh,
        focusedScale = 1.04f,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) SpinroomColors.Bg else SpinroomColors.TextSecondary,
        )
    }
}

/** Left key on the first column / any list row opens the filter panel instead of leaving the screen. */
private fun Modifier.opensFilterOnLeft(enabled: Boolean, open: () -> Unit): Modifier = onPreviewKeyEvent {
    if (enabled && it.key == Key.DirectionLeft) {
        if (it.type == KeyEventType.KeyDown) open()
        true
    } else false
}

@Composable
private fun CoverGrid(
    itemCount: Int,
    itemAt: (Int) -> LibraryRow?,
    onOpen: (LibraryRow) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    focusIndex: Int? = null,
    itemFocus: FocusRequester = FocusRequester(),
    jumpTarget: Int? = null,
    jumpFocus: FocusRequester = FocusRequester(),
    onFocusRow: (Int, LibraryRow) -> Unit = { _, _ -> },
    onOpenFilter: () -> Unit = {},
) {
    val menu = LocalItemMenu.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(148.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(itemCount, key = null, contentType = { "cover" }) { index ->
            val row = itemAt(index)
            if (row == null) {
                Box(Modifier.size(148.dp).background(SpinroomColors.Glass, RoundedCornerShape(12.dp)))
            } else {
                CoverTile(
                    title = row.title,
                    subtitle = row.subtitle,
                    onClick = { onOpen(row) },
                    modifier = Modifier
                        .onFocusChanged { if (it.hasFocus) onFocusRow(index, row) }
                        .opensFilterOnLeft(isFirstColumn(gridState, index), onOpenFilter)
                        .then(if (index == focusIndex) Modifier.focusRequester(itemFocus) else Modifier)
                        .then(if (index == jumpTarget) Modifier.focusRequester(jumpFocus) else Modifier),
                    onLongClick = { menu(row.uri, row.title) },
                    cover = { Cover(row.coverUrl) },
                )
            }
        }
    }
}

private fun isFirstColumn(state: LazyGridState, index: Int): Boolean =
    state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.column == 0

@Composable
private fun ListPane(
    itemCount: Int,
    itemAt: (Int) -> LibraryRow?,
    onOpen: (LibraryRow) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    focusIndex: Int? = null,
    itemFocus: FocusRequester = FocusRequester(),
    jumpTarget: Int? = null,
    jumpFocus: FocusRequester = FocusRequester(),
    onFocusRow: (Int, LibraryRow) -> Unit = { _, _ -> },
    onOpenFilter: () -> Unit = {},
) {
    val menu = LocalItemMenu.current
    LazyColumn(
        state = listState,
        modifier = modifier.padding(end = 20.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(itemCount, key = null, contentType = { "row" }) { index ->
            val row = itemAt(index)
            if (row == null) {
                Box(Modifier.fillMaxWidth().height(LIST_ROW_HEIGHT).background(SpinroomColors.Glass, RoundedCornerShape(12.dp)))
            } else {
                FocusSurface(
                    onClick = { onOpen(row) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.hasFocus) onFocusRow(index, row) }
                        .opensFilterOnLeft(true, onOpenFilter)
                        .then(if (index == focusIndex) Modifier.focusRequester(itemFocus) else Modifier)
                        .then(if (index == jumpTarget) Modifier.focusRequester(jumpFocus) else Modifier),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = SpinroomColors.Glass,
                    focusedContainerColor = SpinroomColors.GlassHigh,
                    focusedScale = 1.02f,
                    onLongClick = { menu(row.uri, row.title) },
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))) { Cover(row.coverUrl) }
                        Column(Modifier.weight(1f)) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (row.subtitle.isNotBlank()) {
                                Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        row.year?.takeIf { it > 0 }?.let {
                            Text(it.toString(), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

private val LIST_ROW_HEIGHT = 68.dp

/** Glass preview of the focused row: cover, metadata, Odtwórz / Losowo / heart and the first tracks. */
@Composable
private fun PreviewPane(preview: PreviewState, actions: LibraryActions, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxHeight().padding(bottom = 24.dp), shape = RoundedCornerShape(20.dp)) {
        val row = preview.row ?: return@GlassSurface
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.size(132.dp).clip(RoundedCornerShape(12.dp))) { Cover(row.coverUrl) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(row.kindRes).uppercase(), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.Accent,
                    )
                    Text(row.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = previewMeta(row, preview)
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusSurface(
                    onClick = { actions.onPlay(row, false) },
                    shape = RoundedCornerShape(50),
                    containerColor = SpinroomColors.Accent,
                    focusedContainerColor = SpinroomColors.Accent,
                    focusedScale = 1.05f,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(SpinroomIcons.Play, null, Modifier.size(18.dp), tint = SpinroomColors.Bg)
                        Text(stringResource(R.string.preview_play), style = MaterialTheme.typography.titleSmall, color = SpinroomColors.Bg)
                    }
                }
                FilterChip(stringResource(R.string.preview_shuffle), false, { actions.onPlay(row, true) })
                FocusSurface(
                    onClick = { actions.onToggleFavorite(row) },
                    shape = RoundedCornerShape(50),
                    containerColor = SpinroomColors.Glass,
                    focusedContainerColor = SpinroomColors.GlassHigh,
                    focusedScale = 1.05f,
                ) { FavoriteHeart(row.favorite, Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
            }
            preview.tracks.forEach { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(t.number.toString(), Modifier.width(22.dp), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
                    Text(t.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    t.durationSec?.takeIf { it > 0 }?.let {
                        Text("%d:%02d".format(it / 60, it % 60), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
                    }
                }
            }
        }
    }
}

private val LibraryRow.kindRes: Int
    get() = when (mediaType) {
        "artist" -> R.string.kind_artist
        "track" -> R.string.kind_track
        "playlist" -> R.string.kind_playlist
        else -> R.string.kind_album
    }

@Composable
private fun previewMeta(row: LibraryRow, preview: PreviewState): String {
    val parts = buildList {
        if (row.subtitle.isNotBlank()) add(row.subtitle)
        row.year?.takeIf { it > 0 }?.let { add(it.toString()) }
        if (preview.trackCount > 0) add(pluralStringTracks(preview.trackCount))
        preview.totalMinutes?.let { add(stringResource(R.string.preview_minutes, it)) }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun pluralStringTracks(n: Int): String = stringResource(R.string.preview_tracks, n)

@Composable
private fun Cover(url: String?) {
    if (url == null) {
        CoverPlaceholder(Modifier.fillMaxSize())
        return
    }
    val request = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .size(Size(THUMB_PX, THUMB_PX))
        .crossfade(true)
        .build()
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = { CoverPlaceholder(Modifier.fillMaxSize()) },
        error = { CoverPlaceholder(Modifier.fillMaxSize()) },
    )
}

@Composable
private fun LetterRail(onJump: (String) -> Unit) {
    Column(
        Modifier.fillMaxHeight().padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        JumpLetters.forEach { letter ->
            FocusSurface(
                onClick = { onJump(letter) },
                shape = RoundedCornerShape(8.dp),
                containerColor = SpinroomColors.Glass,
                focusedScale = 1.15f,
            ) {
                Text(
                    letter,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = SpinroomColors.TextSecondary,
                )
            }
        }
    }
}

private val previewRows = listOf("Akwarele", "Bez pośpiechu", "Ciepły beton", "Dalekie brzegi", "Długi weekend", "Echo miasta", "Głęboka woda", "Horyzont")

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun LibraryPreview() {
    SpinroomTheme {
        LibraryContent(
            state = LibraryUiState(counts = mapOf(LibraryTab.Albums to 412)),
            status = LibraryStatus.Content,
            itemCount = 40,
            itemAt = { LibraryRow("library://album/$it", previewRows[it % previewRows.size], "Odcienie", null) },
            actions = LibraryActions(),
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun LibraryListPreview() {
    val row = LibraryRow("library://album/7", "Horyzont", "Kwartet Północ", null, year = 2021, mediaType = "album")
    SpinroomTheme {
        LibraryContent(
            state = LibraryUiState(
                counts = mapOf(LibraryTab.Albums to 412), listMode = true,
                preview = PreviewState(row, listOf(PreviewTrack(1, "Port 14", 216), PreviewTrack(2, "Północny wiatr", 242)), 11, 46),
            ),
            status = LibraryStatus.Content,
            itemCount = 40,
            itemAt = { LibraryRow("library://album/$it", previewRows[it % previewRows.size], "Odcienie", null, year = 2015 + it % 8, mediaType = "album") },
            actions = LibraryActions(),
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
    SpinroomTheme {
        LibraryContent(LibraryUiState(), LibraryStatus.Empty, 0, { null }, LibraryActions())
    }
}
