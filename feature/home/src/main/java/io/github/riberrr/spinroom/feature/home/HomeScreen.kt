package io.github.riberrr.spinroom.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.riberrr.spinroom.core.design.ContentBackdrop
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.CoverTile
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.ItemMenuHost
import io.github.riberrr.spinroom.core.design.LocalItemMenu
import io.github.riberrr.spinroom.core.design.PlaylistMosaic
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.design.StationTile
import io.github.riberrr.spinroom.core.design.stationColor
import io.github.riberrr.spinroom.core.design.AcronymFill
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

private val TILE_SIZE = 100.dp

/** Favorite station tiles: small, so about 11 fit on screen; the row scrolls sideways for the rest. */
private val STATION_SIZE = 68.dp

/** Actions the screen can trigger; previews pass no-ops. Navigation targets come from `:app`. */
data class HomeActions(
    val onOpenNowPlaying: () -> Unit = {},
    val onOpenSources: () -> Unit = {},
    /** Argument is a Music Assistant item URI. */
    val onOpenDetails: (String) -> Unit = {},
    val onPlayStation: (StationTileState) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    /** "Wszystkie stacje" tile. Wired by `:app` once the Radio route exists. */
    val onOpenRadio: () -> Unit = {},
    /** "Wszystkie playlisty" tile. Wired by `:app` once the Playlists route exists. */
    val onOpenPlaylists: () -> Unit = {},
)

/** "Start" screen. Stateful wrapper: collects [HomeViewModel] and renders [HomeContent]. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRadio: () -> Unit = onOpenSources,
    onOpenPlaylists: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onOpenNowPlaying, onOpenSources, onOpenDetails, onOpenSettings, onOpenRadio, onOpenPlaylists) {
        HomeActions(onOpenNowPlaying, onOpenSources, onOpenDetails, viewModel::onPlayStation, viewModel::onRetry, onOpenSettings, onOpenRadio, onOpenPlaylists)
    }
    // Back from Library/Details: favorites may have changed there.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    ItemMenuHost { HomeContent(ui, actions, modifier) }
}

/** What the backdrop shows for the focused tile: its cover, or a seed color for a station without a logo. */
private data class BackdropTarget(val imageUrl: String?, val seed: Color?)

/** Stateless screen: everything comes from [state], everything goes out through [actions]. */
@Composable
fun HomeContent(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    var backdrop by remember { mutableStateOf(BackdropTarget(null, null)) }
    Box(modifier.fillMaxSize()) {
        ContentBackdrop(imageUrl = backdrop.imageUrl, seedColor = backdrop.seed)
        when {
            state.loaded -> Rows(state, actions) { backdrop = it }
            state.notConfigured -> EmptyState(
                title = stringResource(R.string.home_connect_title),
                message = stringResource(R.string.home_connect_message),
                actionLabel = stringResource(R.string.home_connect_action),
                onAction = actions.onOpenSettings,
            )
            state.failed -> ErrorState(
                message = stringResource(R.string.home_error_message),
                title = stringResource(R.string.home_error_title),
                retryLabel = stringResource(R.string.home_retry),
                onRetry = actions.onRetry,
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_connecting), style = MaterialTheme.typography.headlineSmall, color = SpinroomColors.TextSecondary)
            }
        }
    }
}

/** Remembers which tile was focused last (survives Back from Details) and hands out per-tile focus requesters. */
private class FocusMemory(var lastKey: String?, val onChange: (String) -> Unit, val onBackdrop: (BackdropTarget) -> Unit) {
    val requesters = HashMap<String, FocusRequester>()
    fun requester(key: String) = requesters.getOrPut(key) { FocusRequester() }

    /** [backdrop] null = leave the backdrop as it is (the "all" tiles). */
    fun Modifier.track(key: String, backdrop: BackdropTarget? = null): Modifier =
        this.focusRequester(requester(key)).onFocusChanged {
            if (it.hasFocus) {
                if (lastKey != key) { lastKey = key; onChange(key) }
                if (backdrop != null) onBackdrop(backdrop)
            }
        }
}

@Composable
private fun Rows(state: HomeUiState, actions: HomeActions, onBackdrop: (BackdropTarget) -> Unit) {
    var savedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val memory = remember { FocusMemory(savedKey, { savedKey = it }, onBackdrop) }
    LaunchedEffect(Unit) {
        // Restored tile after Back; otherwise the first favorite station, then the "all stations" tile.
        val target = memory.lastKey?.let { memory.requesters[it] }
            ?: state.stations.firstOrNull()?.let { memory.requesters["st:${it.key}"] }
            ?: memory.requesters["all:stations"]
        runCatching { target?.requestFocus() }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Header(state) }
        for (row in state.visibleRows) when (row) {
            HomeRow.Stations -> stationsRow(state, actions, memory)
            HomeRow.Playlists -> playlistsRow(state, actions, memory)
            HomeRow.Recent -> recentRow(state, actions, memory)
        }
        item { Box(Modifier.height(24.dp)) }
    }
}

private fun LazyListScope.stationsRow(state: HomeUiState, actions: HomeActions, memory: FocusMemory) = item(key = "row:stations") {
    HomeSection(
        stringResource(R.string.home_stations), stringResource(R.string.home_all_stations), actions.onOpenRadio,
        with(memory) { Modifier.track("all:stations") },
    ) {
        if (state.stations.isEmpty()) item(key = "hint:stations") { EmptyHint(stringResource(R.string.home_stations_empty)) }
        items(state.stations, key = { it.key }) { s ->
            val menu = LocalItemMenu.current
            with(memory) {
                StationTile(
                    name = s.name,
                    subtitle = "",
                    size = STATION_SIZE,
                    onClick = { actions.onPlayStation(s) },
                    modifier = Modifier.track("st:${s.key}", BackdropTarget(s.coverUrl, stationColor(s.name))),
                    onLongClick = s.uri?.let { u -> { menu(u, s.name) } },
                    logo = s.coverUrl?.let { url -> { StationLogo(url, s.name) } },
                )
            }
        }
    }
}

private fun LazyListScope.playlistsRow(state: HomeUiState, actions: HomeActions, memory: FocusMemory) = item(key = "row:playlists") {
    HomeSection(
        stringResource(R.string.home_playlists), stringResource(R.string.home_all_playlists), actions.onOpenPlaylists,
        with(memory) { Modifier.track("all:playlists") },
    ) {
        if (state.playlists.isEmpty()) item(key = "hint:playlists") { EmptyHint(stringResource(R.string.home_playlists_empty)) }
        items(state.playlists, key = { it.key }) { p ->
            val menu = LocalItemMenu.current
            Column(Modifier.width(TILE_SIZE)) {
                with(memory) {
                    FocusSurface(
                        onClick = { p.uri?.let(actions.onOpenDetails) },
                        modifier = Modifier.size(TILE_SIZE).track("pl:${p.key}", BackdropTarget(p.coverUrl, null)),
                        focusedScale = 1.08f,
                        onLongClick = p.uri?.let { u -> { menu(u, p.title) } },
                    ) { PlaylistMosaic(listOfNotNull(p.coverUrl), Modifier.fillMaxSize(), name = p.title) }
                }
                Text(p.title, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun LazyListScope.recentRow(state: HomeUiState, actions: HomeActions, memory: FocusMemory) = item(key = "row:recent") {
    HomeSection(stringResource(R.string.home_recent)) {
        items(state.recent, key = { it.key }) { t ->
            with(memory) { Tile(t, actions, Modifier.track("recent:${t.key}", BackdropTarget(t.coverUrl, null))) }
        }
    }
}

@Composable
private fun Header(state: HomeUiState) {
    Row(
        Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // MA gives no user name, so nothing follows the greeting.
        Text(stringResource(R.string.home_greeting), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        GlassSurface(shape = RoundedCornerShape(50)) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val dot = if (state.isConnected) SpinroomColors.Success else SpinroomColors.TextSecondary
                Box(Modifier.size(10.dp).background(dot, CircleShape))
                Text(
                    when {
                        !state.isConnected -> stringResource(R.string.home_offline)
                        state.playerName != null -> stringResource(R.string.home_connected_named, state.playerName)
                        else -> stringResource(R.string.home_connected)
                    },
                    style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary,
                )
            }
        }
    }
}

/** Section title with an optional "see all" link on the right (mockup 03B), then a horizontally scrolling row. */
@Composable
private fun HomeSection(
    title: String,
    linkLabel: String? = null,
    onLink: () -> Unit = {},
    linkModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (linkLabel != null) {
                FocusSurface(
                    onLink, linkModifier, shape = RoundedCornerShape(8.dp), focusedScale = 1.05f,
                    containerColor = Color.Transparent, focusedContainerColor = SpinroomColors.GlassHigh,
                ) {
                    Text(
                        linkLabel, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary,
                    )
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Shown in place of tiles when a favorites row is empty. */
@Composable
private fun EmptyHint(text: String) {
    GlassSurface(Modifier.height(TILE_SIZE).width(280.dp), shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = SpinroomColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Tile(tile: HomeTile, actions: HomeActions, modifier: Modifier = Modifier) {
    val menu = LocalItemMenu.current
    val subtitle = tile.subtitle.ifBlank { if (tile.isPlaylist) stringResource(R.string.home_playlist_label) else "" }
    CoverTile(
        title = tile.title, subtitle = subtitle,
        onClick = { tile.uri?.let(actions.onOpenDetails) }, modifier = modifier,
        onLongClick = tile.uri?.let { u -> { menu(u, tile.title) } },
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
    loaded = true,
    recent = listOf(
        HomeTile("1", "Nocne światła", "Lena Wiatr", null, "library://album/1"),
        HomeTile("2", "Port 14", "Kwartet Północ", null, "library://album/2"),
    ),
    playlists = listOf(HomeTile("3", "Spokojny poranek", "", null, "library://playlist/3", isPlaylist = true)),
    stations = listOf(
        StationTileState("a", "Jazz Jeden", "radiobrowser", "J1", "library://radio/1"),
        StationTileState("b", "Klasyka 24", "tunein", "K2", "library://radio/2"),
    ),
)

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun HomePreview() {
    SpinroomTheme { HomeContent(previewState, HomeActions()) }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    SpinroomTheme { HomeContent(HomeUiState(loading = false, loaded = true), HomeActions()) }
}

/** Station logo; while loading or when it fails, the initials on the tile's color (not the music-note placeholder). */
@Composable
private fun StationLogo(url: String, name: String) {
    val initials: @Composable () -> Unit = { AcronymFill(name, Modifier.fillMaxSize(), MaterialTheme.typography.headlineMedium) }
    SubcomposeAsyncImage(
        model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        loading = { initials() }, error = { initials() },
    )
}
