package io.github.riberrr.spinroom.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.CoverTile
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SectionRow
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.design.StationTile
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** Actions the screen can trigger; previews pass no-ops. Navigation targets come from `:app`. */
data class HomeActions(
    val onOpenNowPlaying: () -> Unit = {},
    val onOpenSources: () -> Unit = {},
    /** Argument is a Music Assistant item URI. */
    val onOpenDetails: (String) -> Unit = {},
    val onPlayStation: (StationTileState) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** "Start" screen. Stateful wrapper: collects [HomeViewModel] and renders [HomeContent]. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onOpenNowPlaying, onOpenSources, onOpenDetails) {
        HomeActions(onOpenNowPlaying, onOpenSources, onOpenDetails, viewModel::onPlayStation, viewModel::onRetry)
    }
    HomeContent(ui, actions, modifier)
}

/** Stateless screen: everything comes from [state], everything goes out through [actions]. */
@Composable
fun HomeContent(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        when {
            state.loading && !state.hasContent -> Unit
            state.failed && !state.hasContent -> ErrorState(
                message = stringResource(R.string.home_error_message),
                title = stringResource(R.string.home_error_title),
                retryLabel = stringResource(R.string.home_retry),
                onRetry = actions.onRetry,
            )
            !state.hasContent -> EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
            )
            else -> Rows(state, actions)
        }
    }
}

@Composable
private fun Rows(state: HomeUiState, actions: HomeActions) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Header(state)
        }
        item {
            TopCards(state, actions, first)
        }
        if (state.recent.isNotEmpty()) item {
            SectionRow(stringResource(R.string.home_recent)) { items(state.recent, key = { it.key }) { Tile(it, actions) } }
        }
        if (state.playlists.isNotEmpty()) item {
            SectionRow(stringResource(R.string.home_playlists)) { items(state.playlists, key = { it.key }) { Tile(it, actions) } }
        }
        if (state.stations.isNotEmpty()) item {
            SectionRow(stringResource(R.string.home_stations)) {
                items(state.stations, key = { it.key }) { s ->
                    StationTile(s.name, s.provider, s.initials, onClick = { actions.onPlayStation(s) })
                }
            }
        }
        if (state.showYoutube) item {
            SectionRow(stringResource(R.string.home_recommended)) { items(state.recommended, key = { it.key }) { Tile(it, actions) } }
        }
        item { Box(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(state: HomeUiState) {
    Row(
        Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.home_greeting), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(SpinroomColors.Surface).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val dot = if (state.isConnected) SpinroomColors.Success else SpinroomColors.TextSecondary
            Box(Modifier.size(10.dp).background(dot, CircleShape))
            Text(
                stringResource(if (state.isConnected) R.string.home_connected else R.string.home_offline),
                style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun TopCards(state: HomeUiState, actions: HomeActions, first: FocusRequester) {
    Row(Modifier.fillMaxWidth().height(144.dp).padding(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        val card = state.continueCard
        // The first card present takes the initial focus.
        if (card != null) ContinueCardView(card, actions.onOpenNowPlaying, Modifier.weight(2f).fillMaxHeight().focusRequester(first))
        val radioMod = Modifier.weight(1f).fillMaxHeight().let { if (card == null) it.focusRequester(first) else it }
        LinkCard(
            title = stringResource(R.string.home_radio),
            subtitle = if (state.stations.isEmpty()) stringResource(R.string.home_radio_empty) else stringResource(R.string.home_radio_count, state.stations.size),
            onClick = actions.onOpenSources, modifier = radioMod,
        )
        if (state.showYoutube) {
            LinkCard(
                title = stringResource(R.string.home_youtube), subtitle = stringResource(R.string.home_youtube_sub),
                onClick = actions.onOpenSources, modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ContinueCardView(card: ContinueCard, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(onClick, modifier, shape = RoundedCornerShape(16.dp), focusedScale = 1.02f) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(card.coverUrl, Modifier.size(112.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.Accent)
                Text(card.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = if (card.total > 0) stringResource(R.string.home_continue_track, card.artist, card.position, card.total) else card.artist
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(SpinroomColors.Outline)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(card.progress).background(SpinroomColors.Accent))
                }
            }
        }
    }
}

@Composable
private fun LinkCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(onClick, modifier, shape = RoundedCornerShape(16.dp), focusedScale = 1.04f) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Bottom) {
            Icon(SpinroomIcons.Sources, null, Modifier.size(28.dp), tint = SpinroomColors.Accent)
            Box(Modifier.weight(1f))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Tile(tile: HomeTile, actions: HomeActions) {
    val subtitle = tile.subtitle.ifBlank { if (tile.isPlaylist) stringResource(R.string.home_playlist_label) else "" }
    CoverTile(
        title = tile.title, subtitle = subtitle,
        onClick = { tile.uri?.let(actions.onOpenDetails) },
        cover = { Cover(tile.coverUrl, Modifier.fillMaxSize()) },
    )
}

@Composable
private fun Cover(url: String?, modifier: Modifier) {
    if (url == null) {
        CoverPlaceholder(modifier)
    } else {
        SubcomposeAsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier,
            loading = { CoverPlaceholder(Modifier.fillMaxSize()) }, error = { CoverPlaceholder(Modifier.fillMaxSize()) },
        )
    }
}

private val previewState = HomeUiState(
    connection = MaConnectionState.Connecting,
    loading = false,
    continueCard = ContinueCard("Nocne światła", "Lena Wiatr", null, 0.35f, 4, 11),
    recent = listOf(
        HomeTile("1", "Nocne światła", "Lena Wiatr", null, "library://album/1"),
        HomeTile("2", "Port 14", "Kwartet Północ", null, "library://album/2"),
        HomeTile("3", "Spokojny poranek", "", null, "library://playlist/3", isPlaylist = true),
    ),
    playlists = listOf(HomeTile("3", "Spokojny poranek", "", null, "library://playlist/3", isPlaylist = true)),
    stations = listOf(
        StationTileState("a", "Jazz Jeden", "radiobrowser", "J1", "library://radio/1"),
        StationTileState("b", "Klasyka 24", "tunein", "K2", "library://radio/2"),
    ),
    recommended = listOf(HomeTile("y", "Mix dla Ciebie", "ytmusic", null, "ytmusic://playlist/1")),
)

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun HomePreview() {
    SpinroomTheme { HomeContent(previewState, HomeActions()) }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun HomeErrorPreview() {
    SpinroomTheme { HomeContent(HomeUiState(loading = false, failed = true), HomeActions()) }
}
