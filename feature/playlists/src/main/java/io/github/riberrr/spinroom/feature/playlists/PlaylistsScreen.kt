package io.github.riberrr.spinroom.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.PlaylistSort
import io.github.riberrr.spinroom.core.design.ContentBackdrop
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.PlaylistMosaic
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** What the grid area shows. */
enum class PlaylistsStatus { NoServer, Connecting, Loading, Content, Empty, Error }

internal fun playlistsStatus(connection: MaConnectionState, refresh: LoadState, itemCount: Int): PlaylistsStatus = when {
    connection is MaConnectionState.Failed -> PlaylistsStatus.Error
    connection is MaConnectionState.Disconnected -> PlaylistsStatus.NoServer
    connection is MaConnectionState.Connected && !connection.authenticated -> PlaylistsStatus.NoServer
    connection !is MaConnectionState.Connected && itemCount == 0 -> PlaylistsStatus.Connecting
    connection !is MaConnectionState.Connected -> PlaylistsStatus.Content
    refresh is LoadState.Error && itemCount == 0 -> PlaylistsStatus.Error
    refresh is LoadState.Loading && itemCount == 0 -> PlaylistsStatus.Loading
    itemCount == 0 -> PlaylistsStatus.Empty
    else -> PlaylistsStatus.Content
}

data class PlaylistsActions(
    val onToggleSort: () -> Unit = {},
    val onCycleSource: () -> Unit = {},
    val onPlay: (PlaylistRow, Boolean) -> Unit = { _, _ -> },
    val onOpenDetails: (PlaylistRow) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Playlisty: mosaic grid, sort "Ostatnio grane" / A-Z, source filter.
 * OK on a tile plays it; the buttons under the focused tile are "Odtworz", "Losowo" and "Szczegoly" (reach them with D-pad down).
 * [onOpenDetails] gets a Music Assistant URI (`library://playlist/3`).
 */
@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    val status = playlistsStatus(state.connection, items.loadState.refresh, items.itemCount)
    PlaylistsContent(
        state = state,
        status = status,
        itemCount = items.itemCount,
        itemAt = { items[it] },
        actions = PlaylistsActions(
            onToggleSort = viewModel::onToggleSort,
            onCycleSource = viewModel::onCycleSource,
            onPlay = viewModel::onPlay,
            onOpenDetails = { onOpenDetails(it.uri) },
            onRetry = { items.retry(); items.refresh() },
        ),
        modifier = modifier,
    )
}

private val TILE: Dp = 190.dp
private val BUTTONS_HEIGHT = 40.dp

@Composable
fun PlaylistsContent(
    state: PlaylistsUiState,
    status: PlaylistsStatus,
    itemCount: Int,
    itemAt: (Int) -> PlaylistRow?,
    actions: PlaylistsActions,
    modifier: Modifier = Modifier,
) {
    val firstChip = remember { FocusRequester() }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { firstChip.requestFocus() } }

    Box(modifier.fillMaxSize()) {
        ContentBackdrop(imageUrl = if (status == PlaylistsStatus.Content) backdropUrl else null)
        Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 32.dp, top = 32.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.playlists_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Chip(
                        stringResource(if (state.sort == PlaylistSort.Recent) R.string.playlists_sort_recent else R.string.playlists_sort_alpha),
                        actions.onToggleSort, Modifier.focusRequester(firstChip),
                    )
                    Chip(
                        state.sources.firstOrNull { it.domain == state.provider }?.name ?: stringResource(R.string.playlists_source_all),
                        actions.onCycleSource,
                    )
                }
            }
            Box(Modifier.fillMaxSize().padding(top = 16.dp)) {
                when (status) {
                    PlaylistsStatus.NoServer -> EmptyState(
                        title = stringResource(R.string.playlists_no_server_title),
                        message = stringResource(R.string.playlists_no_server_message),
                    )
                    PlaylistsStatus.Connecting -> EmptyState(title = stringResource(R.string.playlists_connecting))
                    PlaylistsStatus.Loading -> EmptyState(title = stringResource(R.string.playlists_loading))
                    PlaylistsStatus.Error -> ErrorState(
                        message = stringResource(R.string.playlists_error_message),
                        retryLabel = stringResource(R.string.playlists_retry),
                        onRetry = actions.onRetry,
                    )
                    PlaylistsStatus.Empty -> EmptyState(
                        title = stringResource(R.string.playlists_empty_title),
                        message = stringResource(R.string.playlists_empty_message),
                    )
                    PlaylistsStatus.Content -> PlaylistGrid(itemCount, itemAt, actions) { backdropUrl = it }
                }
            }
        }
    }
}

@Composable
private fun PlaylistGrid(
    itemCount: Int,
    itemAt: (Int) -> PlaylistRow?,
    actions: PlaylistsActions,
    onFocusCover: (String?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(TILE),
        state = rememberLazyGridState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(itemCount, key = null, contentType = { "playlist" }) { index ->
            val row = itemAt(index)
            if (row == null) {
                Box(Modifier.size(TILE).padding(bottom = 0.dp).background(SpinroomColors.Glass, RoundedCornerShape(12.dp)))
            } else {
                PlaylistTile(row, actions) { onFocusCover(row.coverUrl) }
            }
        }
    }
}

/** Mosaic tile; while the tile or one of its buttons has focus the action row shows under the title. */
@Composable
private fun PlaylistTile(row: PlaylistRow, actions: PlaylistsActions, onFocused: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    Column(
        Modifier.width(TILE).onFocusChanged { active = it.hasFocus; if (it.hasFocus) onFocused() },
    ) {
        FocusSurface(
            onClick = { actions.onPlay(row, false) },
            modifier = Modifier.size(TILE),
            focusedScale = 1.06f,
        ) { PlaylistMosaic(listOfNotNull(row.coverUrl), Modifier.fillMaxSize(), name = row.title) }
        Text(row.title, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            row.source.ifBlank { " " }, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.height(BUTTONS_HEIGHT).padding(top = 6.dp)) {
            if (active) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallButton(stringResource(R.string.playlists_play), true, { actions.onPlay(row, false) })
                SmallButton(stringResource(R.string.playlists_shuffle), false, { actions.onPlay(row, true) })
                SmallButton(stringResource(R.string.playlists_details), false, { actions.onOpenDetails(row) })
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        containerColor = SpinroomColors.Glass,
        focusedContainerColor = SpinroomColors.GlassHigh,
        focusedScale = 1.04f,
    ) {
        Text(label, Modifier.padding(horizontal = 18.dp, vertical = 9.dp), style = MaterialTheme.typography.titleSmall, color = SpinroomColors.TextPrimary)
    }
}

@Composable
private fun SmallButton(label: String, primary: Boolean, onClick: () -> Unit, icon: Boolean = false) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        containerColor = if (primary) SpinroomColors.Accent else SpinroomColors.GlassHigh,
        focusedContainerColor = if (primary) SpinroomColors.Accent else SpinroomColors.Glass,
        focusedScale = 1.06f,
    ) {
        val color = if (primary) SpinroomColors.Bg else SpinroomColors.TextPrimary
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon) Icon(SpinroomIcons.Play, null, Modifier.size(14.dp), tint = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun PlaylistsPreview() {
    val names = listOf("Spokojny poranek", "Mix dla Ciebie", "Jazz na wieczór", "Do samochodu", "Impreza")
    SpinroomTheme {
        PlaylistsContent(
            state = PlaylistsUiState(),
            status = PlaylistsStatus.Content,
            itemCount = 10,
            itemAt = { PlaylistRow("library://playlist/$it", names[it % names.size], "YouTube Music", null) },
            actions = PlaylistsActions(),
        )
    }
}
