package io.github.riberrr.spinroom.feature.nowplaying

import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.MiniPlayerBar
import io.github.riberrr.spinroom.core.design.ProgressBar
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** Actions the screen can trigger; the ViewModel implements them, previews pass no-ops. */
data class NowPlayingActions(
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onRepeat: () -> Unit = {},
    val onFavorite: () -> Unit = {},
    val onToggleQueue: () -> Unit = {},
    val onCollapseQueue: () -> Unit = {},
    val onPlayIndex: (Int) -> Unit = {},
    val onOpenPicker: () -> Unit = {},
    val onClosePicker: () -> Unit = {},
    val onSelectPlayer: (String) -> Unit = {},
) {
    companion object {
        fun of(vm: NowPlayingViewModel) = NowPlayingActions(
            vm::onPlayPause, vm::onNext, vm::onPrevious, vm::onShuffle, vm::onRepeat, vm::onFavorite,
            vm::onToggleQueue, vm::onCollapseQueue, vm::onPlayIndex, vm::onOpenPicker, vm::onClosePicker, vm::onSelectPlayer,
        )
    }
}

/**
 * One ViewModel for the whole activity, so the mini player in the shell and this screen see the same state.
 * (A nav-entry scoped `hiltViewModel()` would create a second copy and a second set of subscriptions.)
 */
@Composable
fun activityNowPlayingViewModel(): NowPlayingViewModel =
    hiltViewModel(LocalContext.current.findActivity() as ViewModelStoreOwner)

private tailrec fun android.content.Context.findActivity(): android.app.Activity = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> error("No activity in context")
}

/** "Teraz odtwarzane" screen. Stateful wrapper: collects [NowPlayingViewModel] and renders [NowPlayingContent]. */
@Composable
fun NowPlayingScreen(modifier: Modifier = Modifier, viewModel: NowPlayingViewModel = activityNowPlayingViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val debuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    NowPlayingContent(ui, NowPlayingActions.of(viewModel), debuggable, modifier)
}

/** Mini player bar for the app shell, backed by the same state as the screen. */
@Composable
fun NowPlayingMiniPlayer(onOpenNowPlaying: () -> Unit, modifier: Modifier = Modifier, viewModel: NowPlayingViewModel = activityNowPlayingViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    if (!ui.hasTrack) return
    MiniPlayerBar(
        state = ui.toMiniPlayerState(),
        onPrevious = viewModel::onPrevious,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onOpenNowPlaying = onOpenNowPlaying,
        modifier = modifier,
        nowPlayingLabel = stringResource(R.string.nowplaying_label),
        cover = { Cover(ui.coverUrl, Modifier.fillMaxSize()) },
    )
}

/** Stateless screen: everything comes from [state], everything goes out through [actions]. */
@Composable
fun NowPlayingContent(
    state: NowPlayingUiState,
    actions: NowPlayingActions,
    debuggable: Boolean,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.pickerOpen) { actions.onClosePicker() }
    BackHandler(enabled = state.queueExpanded && !state.pickerOpen) { actions.onCollapseQueue() }

    Box(modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        Backdrop(state.coverUrl)
        when {
            !state.hasPlayer -> EmptyState(
                title = stringResource(R.string.nowplaying_no_player_title),
                message = stringResource(R.string.nowplaying_no_player_message),
                actionLabel = if (debuggable) stringResource(R.string.nowplaying_pick_player) else null,
                onAction = if (debuggable) actions.onOpenPicker else null,
            )
            !state.hasTrack -> EmptyState(
                title = stringResource(R.string.nowplaying_nothing_title),
                message = if (state.isConnected) stringResource(R.string.nowplaying_nothing_message) else stringResource(R.string.nowplaying_offline),
            )
            else -> Player(state, actions)
        }
        TopBar(state, actions, debuggable, Modifier.align(Alignment.TopEnd))
        if (state.pickerOpen) PlayerPicker(state, actions)
    }
}

@Composable
private fun Backdrop(coverUrl: String?) {
    if (coverUrl != null) {
        // Blur needs API 31+; older devices just get the darker scrim.
        val blurred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(64.dp) else Modifier
        SubcomposeAsyncImage(
            model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(blurred),
            loading = {}, error = {},
        )
    }
    val scrim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.55f else 0.8f
    Box(Modifier.fillMaxSize().background(SpinroomColors.Bg.copy(alpha = if (coverUrl != null) scrim else 1f)))
}

@Composable
private fun TopBar(state: NowPlayingUiState, actions: NowPlayingActions, debuggable: Boolean, modifier: Modifier) {
    Row(modifier.padding(top = 32.dp, end = 72.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (state.hasPlayer || debuggable) {
            val label = state.playerName.ifBlank { stringResource(R.string.nowplaying_pick_player) }
            val chipText = if (state.isConnected) label else stringResource(R.string.nowplaying_offline)
            val dot = if (state.isConnected) SpinroomColors.Success else SpinroomColors.TextSecondary
            Chip(onClick = if (debuggable) actions.onOpenPicker else null) {
                Box(Modifier.size(10.dp).background(dot, CircleShape))
                Text(chipText, style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        state.volume?.let { volume ->
            Chip(onClick = null) {
                Icon(NowPlayingIcons.Volume, stringResource(R.string.nowplaying_volume), Modifier.size(18.dp), tint = SpinroomColors.TextSecondary)
                Text("$volume%", style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun Chip(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val inner: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
    if (onClick != null) {
        FocusSurface(onClick, shape = CircleShape, focusedScale = 1.04f) { inner() }
    } else {
        Box(Modifier.clip(CircleShape).background(SpinroomColors.Surface)) { inner() }
    }
}

@Composable
private fun Player(state: NowPlayingUiState, actions: NowPlayingActions) {
    Row(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 72.dp), horizontalArrangement = Arrangement.spacedBy(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(state.coverUrl, Modifier.size(440.dp).clip(RoundedCornerShape(20.dp)))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            if (state.queueExpanded) FullQueue(state, actions) else Details(state, actions)
        }
    }
}

@Composable
private fun Details(state: NowPlayingUiState, actions: NowPlayingActions) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }

    Text(stringResource(R.string.nowplaying_label).uppercase(), style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Accent, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(state.title, style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    val subtitle = listOf(state.artist, state.album).filter { it.isNotBlank() }.joinToString(" · ")
    if (subtitle.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(24.dp))
    ProgressBar(state.positionMs, state.durationMs, Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(state.positionMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
        Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
    }
    Spacer(Modifier.height(20.dp))
    Controls(state, actions, playFocus)
    Spacer(Modifier.height(24.dp))
    UpNext(state, actions)
}

@Composable
private fun Controls(state: NowPlayingUiState, actions: NowPlayingActions, playFocus: FocusRequester) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(NowPlayingIcons.Shuffle, stringResource(R.string.nowplaying_shuffle), actions.onShuffle, 52.dp, active = state.shuffle)
        ControlButton(SpinroomIcons.Previous, stringResource(R.string.nowplaying_previous), actions.onPrevious, 60.dp)
        ControlButton(
            if (state.isPlaying) SpinroomIcons.Pause else SpinroomIcons.Play,
            stringResource(if (state.isPlaying) R.string.nowplaying_pause else R.string.nowplaying_play),
            actions.onPlayPause, 80.dp, filled = true, modifier = Modifier.focusRequester(playFocus),
        )
        ControlButton(SpinroomIcons.Next, stringResource(R.string.nowplaying_next), actions.onNext, 60.dp)
        ControlButton(
            if (state.repeat == RepeatMode.ONE) NowPlayingIcons.RepeatOne else NowPlayingIcons.Repeat,
            stringResource(R.string.nowplaying_repeat), actions.onRepeat, 52.dp, active = state.repeat != RepeatMode.OFF,
        )
        Spacer(Modifier.weight(1f))
        ControlButton(
            if (state.favorite) NowPlayingIcons.Favorite else NowPlayingIcons.FavoriteOutline,
            stringResource(R.string.nowplaying_favorite), actions.onFavorite, 52.dp, active = state.favorite,
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    active: Boolean = false,
) {
    FocusSurface(
        onClick, modifier.size(size), shape = CircleShape,
        containerColor = if (filled) SpinroomColors.TextPrimary else SpinroomColors.Surface,
        focusedContainerColor = if (filled) SpinroomColors.TextPrimary else SpinroomColors.SurfaceHigh,
        focusedScale = 1.1f,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val tint = when { filled -> SpinroomColors.Bg; active -> SpinroomColors.Accent; else -> SpinroomColors.TextPrimary }
            Icon(icon, description, Modifier.size(size / 2), tint = tint)
        }
    }
}

@Composable
private fun UpNext(state: NowPlayingUiState, actions: NowPlayingActions) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SpinroomColors.Bg.copy(alpha = 0.7f)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nowplaying_up_next), style = MaterialTheme.typography.titleSmall, color = SpinroomColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            FocusSurface(actions.onToggleQueue, shape = RoundedCornerShape(8.dp), containerColor = Color.Transparent, focusedScale = 1.03f) {
                Text(
                    stringResource(R.string.nowplaying_show_queue, state.queue.size),
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.upcoming.isEmpty()) {
            Text(stringResource(R.string.nowplaying_queue_empty), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
        } else {
            state.upcoming.forEach { row -> QueueLine(row, onClick = null) }
        }
    }
}

@Composable
private fun ColumnScope.FullQueue(state: NowPlayingUiState, actions: NowPlayingActions) {
    val listState = rememberLazyListState()
    val currentFocus = remember { FocusRequester() }
    val currentPos = state.queue.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentPos)
        runCatching { currentFocus.requestFocus() }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.nowplaying_queue_title), style = MaterialTheme.typography.headlineSmall, color = SpinroomColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.nowplaying_hide_queue), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
    }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(state.queue, key = { _, row -> row.queueItemId.ifEmpty { row.index.toString() } }) { position, row ->
            QueueLine(row, onClick = { actions.onPlayIndex(row.index) }, modifier = if (position == currentPos) Modifier.focusRequester(currentFocus) else Modifier)
        }
    }
}

@Composable
private fun QueueLine(row: QueueRow, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Cover(row.coverUrl, Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)))
            val title = if (row.artist.isBlank()) row.title else "${row.title} — ${row.artist}"
            Text(
                title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (row.isCurrent) SpinroomColors.Accent else SpinroomColors.TextPrimary,
            )
            Text(formatTime(row.durationMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
        }
    }
    if (onClick != null) {
        FocusSurface(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), containerColor = Color.Transparent, focusedScale = 1.02f) { content() }
    } else {
        Box(modifier) { content() }
    }
}

@Composable
private fun PlayerPicker(state: NowPlayingUiState, actions: NowPlayingActions) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(520.dp).clip(RoundedCornerShape(20.dp)).background(SpinroomColors.Surface).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.nowplaying_pick_player), style = MaterialTheme.typography.headlineSmall, color = SpinroomColors.TextPrimary)
            val first = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.players, key = { _, p -> p.id }) { position, p ->
                    FocusSurface(
                        { actions.onSelectPlayer(p.id) }, Modifier.fillMaxWidth().then(if (position == 0) Modifier.focusRequester(first) else Modifier),
                        shape = RoundedCornerShape(10.dp), containerColor = SpinroomColors.SurfaceHigh, focusedContainerColor = SpinroomColors.Outline, focusedScale = 1.02f,
                    ) {
                        val suffix = if (p.available) "" else " (${stringResource(R.string.nowplaying_player_unavailable)})"
                        Text(
                            p.name + suffix, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge,
                            color = if (p.selected) SpinroomColors.Accent else SpinroomColors.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun Cover(url: String?, modifier: Modifier) {
    if (url == null) {
        CoverPlaceholder(modifier)
    } else {
        SubcomposeAsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier,
            loading = { CoverPlaceholder(Modifier.fillMaxSize()) }, error = { CoverPlaceholder(Modifier.fillMaxSize()) },
        )
    }
}

internal fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private val previewState = NowPlayingUiState(
    connection = MaConnectionState.Disconnected,
    playerId = "tv", playerName = "Salon TV", hasTrack = true, title = "Latarnie nad rzeką", artist = "Lena Wiatr", album = "Nocne światła",
    isPlaying = true, positionMs = 84_000, durationMs = 221_000, shuffle = true, volume = 65,
    upcoming = listOf(
        QueueRow(1, "a", "Głęboka woda", "Lena Wiatr", 258_000, null, false),
        QueueRow(2, "b", "Port 14", "Kwartet Północ", 216_000, null, false),
        QueueRow(3, "c", "Ciepły beton", "Odcienie", 232_000, null, false),
    ),
    queue = List(8) { QueueRow(it, "q$it", "Utwór $it", "Artysta", 200_000, null, it == 0) },
)

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun NowPlayingPreview() = SpinroomTheme { NowPlayingContent(previewState, NowPlayingActions(), debuggable = false) }

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun NowPlayingQueuePreview() = SpinroomTheme { NowPlayingContent(previewState.copy(queueExpanded = true), NowPlayingActions(), debuggable = false) }

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun NowPlayingEmptyPreview() = SpinroomTheme { NowPlayingContent(NowPlayingUiState(), NowPlayingActions(), debuggable = true) }
