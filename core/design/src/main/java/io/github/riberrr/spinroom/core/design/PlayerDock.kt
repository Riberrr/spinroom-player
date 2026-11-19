package io.github.riberrr.spinroom.core.design

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.isLive
import kotlinx.coroutines.delay

/** One row of the dock panel's right-hand list (upcoming track or favorite station). [key] comes back in `onEntryClick`. */
data class DockEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val trailing: String,
    val imageUrl: String?,
    val isStation: Boolean,
)

/** Texts of [PlayerDock] (Polish, from the caller's `strings.xml`). */
data class DockLabels(
    val nowPlaying: String = "Teraz gra",
    val openFullScreen: String = "Pełny ekran",
    val live: String = "Na żywo",
    val previous: String = "Poprzedni",
    val next: String = "Następny",
    val play: String = "Odtwórz",
    val pause: String = "Pauza",
    val favorite: String = "Ulubione",
)

/** The panel collapses by itself after this long without a key press. */
const val DOCK_IDLE_MS = 8_000L

private val PANEL_HEIGHT = 240.dp

/**
 * Bottom player of every screen. Collapsed: an 80 dp glass bar (one focus target; OK opens the full player).
 * When focus enters it, it unfolds into a ~320 dp panel drawn over the content (the layout slot stays 80 dp):
 * cover, ⏮ ⏯ ⏭, ♡ and progress on the left, [entries] (queue or favorite stations) on the right, blurred cover as backdrop.
 *
 * The dock owns "expanded" = focus is inside it. Collapse by moving focus out: [onCollapse] is called for Back and after
 * [DOCK_IDLE_MS] without keys and must put focus back where it was (the shell does `screenFocus.requestFocus()`).
 * [barFocus] lets the shell open the dock from outside (focus goes to the bar, which unfolds and hands focus to play/pause).
 */
@Composable
fun PlayerDock(
    state: MiniPlayerState,
    coverUrl: String?,
    isRadio: Boolean,
    favorite: Boolean,
    entries: List<DockEntry>,
    entriesTitle: String,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onEntryClick: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    barFocus: FocusRequester = remember { FocusRequester() },
    labels: DockLabels = DockLabels(),
    onExpandedChange: (Boolean) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var keyTick by remember { mutableIntStateOf(0) }
    val playFocus = remember { FocusRequester() }

    BackHandler(enabled = expanded) { onCollapse() }
    LaunchedEffect(expanded, keyTick) {
        if (expanded) {
            delay(DOCK_IDLE_MS)
            onCollapse()
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) runCatching { playFocus.requestFocus() }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(80.dp)
            .zIndex(2f)
            .onPreviewKeyEvent { keyTick++; false }
            .onFocusChanged {
                if (it.hasFocus != expanded) {
                    expanded = it.hasFocus
                    onExpandedChange(it.hasFocus)
                }
            }
            .focusGroup(),
    ) {
        DockBar(state, coverUrl, isRadio, labels, onOpenNowPlaying, Modifier.focusRequester(barFocus))
        if (expanded) {
            // Down must not reach the (covered) bar below the panel.
            Box(
                Modifier.fillMaxWidth().wrapContentHeight(Alignment.Bottom, unbounded = true).height(PANEL_HEIGHT)
                    .onPreviewKeyEvent { it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown },
            ) {
                DockPanel(
                    state, coverUrl, isRadio, favorite, entries, entriesTitle, labels,
                    onPrevious, onPlayPause, onNext, onFavorite, onOpenNowPlaying, onEntryClick, playFocus,
                )
            }
        }
    }
}

@Composable
private fun DockBar(state: MiniPlayerState, coverUrl: String?, isRadio: Boolean, labels: DockLabels, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick, modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp, vertical = 8.dp), shape = RoundedCornerShape(18.dp),
        containerColor = SpinroomColors.GlassHigh, focusedContainerColor = SpinroomColors.Accent.copy(alpha = 0.20f), focusedScale = 1f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryThumb(state.title, coverUrl, isRadio, Modifier.size(46.dp))
            Column(Modifier.padding(start = 20.dp).width(300.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isLive(state.durationMs)) {
                Spacer(Modifier.weight(1f))
                Text(labels.live, style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Accent, maxLines = 1, softWrap = false)
            } else {
                Text(formatTime(state.positionMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                ProgressBar(state.positionMs, state.durationMs, Modifier.padding(horizontal = 12.dp).weight(1f))
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun DockPanel(
    state: MiniPlayerState,
    coverUrl: String?,
    isRadio: Boolean,
    favorite: Boolean,
    entries: List<DockEntry>,
    entriesTitle: String,
    labels: DockLabels,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onEntryClick: (String) -> Unit,
    playFocus: FocusRequester,
) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        ContentBackdrop(imageUrl = coverUrl, seedColor = if (isRadio) stationColor(state.title) else null)
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpinroomColors.GlassBorder))
        Row(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryThumb(state.title, coverUrl, isRadio, Modifier.size(150.dp))
            Column(Modifier.padding(start = 24.dp).weight(1.15f)) {
                Text(labels.nowPlaying.uppercase(), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.Accent, maxLines = 1)
                Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isLive(state.durationMs)) {
                        Text(labels.live, style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Accent, maxLines = 1)
                    } else {
                        Text(formatTime(state.positionMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                        ProgressBar(state.positionMs, state.durationMs, Modifier.padding(horizontal = 12.dp).weight(1f))
                        Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(SpinroomIcons.Previous, labels.previous, onPrevious, 42.dp)
                    RoundIconButton(
                        if (state.isPlaying) SpinroomIcons.Pause else SpinroomIcons.Play,
                        if (state.isPlaying) labels.pause else labels.play,
                        onPlayPause, 52.dp, filled = true, modifier = Modifier.padding(horizontal = 12.dp).focusRequester(playFocus),
                    )
                    RoundIconButton(SpinroomIcons.Next, labels.next, onNext, 42.dp)
                    Spacer(Modifier.weight(1f))
                    RoundIconButton(if (favorite) SpinroomIcons.HeartFilled else SpinroomIcons.HeartOutline, labels.favorite, onFavorite, 42.dp)
                }
            }
            Column(Modifier.padding(start = 32.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(entriesTitle.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.TextSecondary, maxLines = 1)
                    FocusSurface(
                        onOpenNowPlaying, shape = RoundedCornerShape(16.dp),
                        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.04f,
                    ) {
                        Text("${labels.openFullScreen} ↗", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
                // Scrolls with focus (D-pad Down): the list holds all favorite stations, not just the ones that fit.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    FocusSurface(
                        { onEntryClick(entry.key) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.02f,
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            EntryThumb(entry.title, entry.imageUrl, entry.isStation, Modifier.size(38.dp))
                            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (entry.trailing.isNotBlank()) Text(entry.trailing, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                        }
                    }
                }
                }
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 360, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun PlayerDockPreview() {
    SpinroomTheme {
        Box(Modifier.fillMaxSize()) {
            PlayerDock(
                MiniPlayerState("Latarnie nad rzeką", "Lena Wiatr", true, 84_000, 221_000), null, false, false,
                listOf(DockEntry("1", "Głęboka woda", "Lena Wiatr", "4:18", null, false)), "Dalej w kolejce",
                {}, {}, {}, {}, {}, {}, {}, Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
