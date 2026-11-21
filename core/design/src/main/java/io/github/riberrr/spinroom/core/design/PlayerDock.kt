package io.github.riberrr.spinroom.core.design

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Icon
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusProperties
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
    val nowPlayingButton: String = "Teraz odtwarzane",
    val live: String = "Na żywo",
    val previous: String = "Poprzedni",
    val next: String = "Następny",
    val play: String = "Odtwórz",
    val pause: String = "Pauza",
    val favorite: String = "Ulubione",
    val loading: String = "Ładowanie…",
)

/** The panel collapses by itself after this long without a key press. */
const val DOCK_IDLE_MS = 8_000L

private val PANEL_HEIGHT = 240.dp

/** Collapsed bar height; the shell reserves the same slot at the top of every screen. */
val DOCK_BAR_HEIGHT = 56.dp
private val BAR_HEIGHT = DOCK_BAR_HEIGHT

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
    keyHint: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var keyTick by remember { mutableIntStateOf(0) }
    val playFocus = remember { FocusRequester() }
    // While the panel is open the covered bar must not take focus (Down from the last entry), but Down must still move
    // between the entries, so keys are not swallowed: the bar is simply not focusable.
    var barLocked by remember { mutableStateOf(false) }

    BackHandler(enabled = expanded) { onCollapse() }
    LaunchedEffect(expanded, keyTick) {
        if (expanded) {
            delay(DOCK_IDLE_MS)
            onCollapse()
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            runCatching { playFocus.requestFocus() }
            barLocked = true
        } else {
            barLocked = false
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .zIndex(2f)
            .onPreviewKeyEvent { keyTick++; false }
            // Down off the panel's last control returns to the screen under it instead of jumping into the covered content.
            .focusProperties { onExit = { if (requestedFocusDirection == FocusDirection.Down) { cancelFocusChange(); onCollapse() } } }
            .onFocusChanged {
                if (it.hasFocus != expanded) {
                    expanded = it.hasFocus
                    onExpandedChange(it.hasFocus)
                }
            }
            .focusGroup(),
    ) {
        DockBar(state, coverUrl, isRadio, labels, keyHint, onOpenNowPlaying, Modifier.focusProperties { canFocus = !barLocked }.focusRequester(barFocus))
        if (expanded) {
            Box(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).height(PANEL_HEIGHT)) {
                DockPanel(
                    state, coverUrl, isRadio, favorite, entries, entriesTitle, labels,
                    onPrevious, onPlayPause, onNext, onFavorite, onOpenNowPlaying, onEntryClick, playFocus,
                )
            }
        }
    }
}

@Composable
private fun DockBar(
    state: MiniPlayerState, coverUrl: String?, isRadio: Boolean, labels: DockLabels, keyHint: String?,
    onClick: () -> Unit, modifier: Modifier,
) {
    FocusSurface(
        onClick, modifier.fillMaxWidth().height(BAR_HEIGHT).padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp),
        containerColor = SpinroomColors.GlassHigh, focusedContainerColor = SpinroomColors.Accent.copy(alpha = 0.20f), focusedScale = 1f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryThumb(state.title, coverUrl, isRadio, Modifier.size(36.dp))
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleSmall, color = SpinroomColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.artist.isNotBlank()) {
                    Text(state.artist, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).width(260.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.loading) {
                    Spacer(Modifier.weight(1f))
                    LoadingLabel(labels.loading)
                } else if (isLive(state.durationMs)) {
                    Spacer(Modifier.weight(1f))
                    Text(labels.live, style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Accent, maxLines = 1, softWrap = false)
                } else {
                    Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                    ProgressBar(state.positionMs, state.durationMs, Modifier.padding(horizontal = 8.dp).weight(1f), thickness = 2.dp)
                    Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false)
                }
            }
            // Dimmed but readable controls: not separate focus targets (focus opens the panel, where they work).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(SpinroomIcons.Previous, labels.previous, Modifier.size(24.dp), tint = SpinroomColors.TextPrimary.copy(alpha = 0.55f))
                Icon(
                    if (state.isPlaying) SpinroomIcons.Pause else SpinroomIcons.Play,
                    if (state.isPlaying) labels.pause else labels.play, Modifier.size(30.dp), tint = SpinroomColors.TextPrimary.copy(alpha = 0.75f),
                )
                Icon(SpinroomIcons.Next, labels.next, Modifier.size(24.dp), tint = SpinroomColors.TextPrimary.copy(alpha = 0.55f))
            }
            if (keyHint != null) KeyHint(keyHint, Modifier.padding(start = 20.dp))
        }
    }
}

/** Very discreet hint: a small grey remote-button pill with the assigned key's name and a pointing hand. */
@Composable
internal fun KeyHint(label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, color = SpinroomColors.TextSecondary, maxLines = 1, softWrap = false,
        )
        Icon(SpinroomIcons.Touch, null, Modifier.padding(start = 4.dp).size(18.dp), tint = SpinroomColors.TextSecondary.copy(alpha = 0.7f))
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
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))) {
        // The panel has its own backdrop (the cover); it must not replace the screen's one in the shell.
        androidx.compose.runtime.CompositionLocalProvider(LocalBackdropHost provides null) {
            ContentBackdrop(imageUrl = coverUrl, seedColor = if (isRadio) stationColor(state.title) else null)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(SpinroomColors.GlassBorder))
        Row(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryThumb(state.title, coverUrl, isRadio, Modifier.size(150.dp))
            Column(Modifier.padding(start = 24.dp).weight(1.15f)) {
                Text(labels.nowPlaying.uppercase(), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.Accent, maxLines = 1)
                Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.loading) {
                        LoadingLabel(labels.loading)
                    } else if (isLive(state.durationMs)) {
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
                {}, {}, {}, {}, {}, {}, {}, Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
