package io.github.riberrr.spinroom.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import io.github.riberrr.spinroom.core.design.ItemMenuHost
import io.github.riberrr.spinroom.core.design.LocalItemMenu
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.CoverTile
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme

/** What the grid area shows. */
enum class LibraryStatus { Loading, Content, Empty, Error }

data class LibraryActions(
    val onSelectTab: (LibraryTab) -> Unit = {},
    val onJumpToLetter: (String) -> Unit = {},
    val onOpenItem: (LibraryRow) -> Unit = {},
    val onRetry: () -> Unit = {},
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

    LaunchedEffect(viewModel) { viewModel.scrollTo.collect { gridState.scrollToItem(it) } }
    // A new tab starts at the top.
    LaunchedEffect(state.tab) { gridState.scrollToItem(0) }

    val refresh = items.loadState.refresh
    val status = when {
        refresh is LoadState.Error && items.itemCount == 0 -> LibraryStatus.Error
        refresh is LoadState.Loading && items.itemCount == 0 -> LibraryStatus.Loading
        items.itemCount == 0 -> LibraryStatus.Empty
        else -> LibraryStatus.Content
    }

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
        ),
        modifier = modifier,
        gridState = gridState,
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
) {
    val tabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { tabFocus.requestFocus() } }

    Column(modifier.fillMaxSize().background(SpinroomColors.Bg).padding(start = 48.dp, end = 32.dp, top = 32.dp)) {
        Text(stringResource(R.string.library_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary)
        Row(Modifier.padding(top = 20.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryTab.entries.forEach { tab ->
                val selected = tab == state.tab
                val label = stringResource(tab.labelRes) + if (selected && state.count != null) " · ${state.count}" else ""
                TabChip(label, selected, actions.onSelectTab.let { { it(tab) } }, if (selected) Modifier.focusRequester(tabFocus) else Modifier)
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (status) {
                LibraryStatus.Error -> ErrorState(
                    message = stringResource(R.string.library_error_message),
                    retryLabel = stringResource(R.string.library_retry),
                    onRetry = actions.onRetry,
                )
                LibraryStatus.Empty -> EmptyState(
                    title = stringResource(R.string.library_empty_title),
                    message = stringResource(R.string.library_empty_message),
                )
                else -> Row(Modifier.fillMaxSize()) {
                    CoverGrid(itemCount, itemAt, actions.onOpenItem, gridState, Modifier.weight(1f))
                    LetterRail(actions.onJumpToLetter)
                }
            }
        }
    }
}

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
        containerColor = if (selected) SpinroomColors.TextPrimary else SpinroomColors.Bg,
        focusedContainerColor = if (selected) SpinroomColors.TextPrimary else SpinroomColors.SurfaceHigh,
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

@Composable
private fun CoverGrid(
    itemCount: Int,
    itemAt: (Int) -> LibraryRow?,
    onOpen: (LibraryRow) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
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
                Box(Modifier.size(148.dp).background(SpinroomColors.Surface, RoundedCornerShape(12.dp)))
            } else {
                CoverTile(
                    title = row.title,
                    subtitle = row.subtitle,
                    onClick = { onOpen(row) },
                    onLongClick = { menu(row.uri, row.title) },
                    cover = { Cover(row.coverUrl) },
                )
            }
        }
    }
}

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
                containerColor = SpinroomColors.Bg,
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

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun LibraryPreview() {
    val rows = listOf("Akwarele", "Bez pośpiechu", "Ciepły beton", "Dalekie brzegi", "Długi weekend", "Echo miasta", "Głęboka woda", "Horyzont")
    SpinroomTheme {
        LibraryContent(
            state = LibraryUiState(counts = mapOf(LibraryTab.Albums to 412)),
            status = LibraryStatus.Content,
            itemCount = 40,
            itemAt = { LibraryRow("library://album/$it", rows[it % rows.size], "Odcienie", null) },
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
