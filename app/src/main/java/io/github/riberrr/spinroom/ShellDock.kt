package io.github.riberrr.spinroom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.riberrr.spinroom.core.design.DockEntry
import io.github.riberrr.spinroom.core.design.DockLabels
import io.github.riberrr.spinroom.core.design.PlayerDock
import io.github.riberrr.spinroom.feature.nowplaying.QueueRow
import io.github.riberrr.spinroom.feature.nowplaying.activityNowPlayingViewModel

private const val DOCK_ENTRIES = 10
private const val DOCK_STATIONS = 30

/**
 * [PlayerDock] bound to the shared now-playing state. Right-hand list: next queue items, or favorite stations while radio plays.
 * Draws nothing when no track. [barFocus] opens the dock from outside; [onCollapse] must restore the screen's focus.
 */
@Composable
fun ShellDock(
    barFocus: FocusRequester,
    onOpenNowPlaying: () -> Unit,
    onCollapse: () -> Unit,
    focusLocked: Boolean = false,
    modifier: Modifier = Modifier,
    shell: ShellViewModel = hiltViewModel(),
) {
    val nowPlaying = activityNowPlayingViewModel()
    val ui by nowPlaying.state.collectAsStateWithLifecycle()
    val stations by shell.stations.collectAsStateWithLifecycle()
    if (!ui.hasTrack) return
    val labels = DockLabels(
        nowPlaying = stringResource(R.string.dock_now_playing),
        openFullScreen = stringResource(R.string.dock_full_screen),
        nowPlayingButton = stringResource(R.string.dock_now_playing_button),
        live = stringResource(R.string.dock_live),
        previous = stringResource(R.string.dock_previous),
        next = stringResource(R.string.dock_next),
        play = stringResource(R.string.dock_play),
        pause = stringResource(R.string.dock_pause),
        favorite = stringResource(R.string.dock_favorite),
        loading = stringResource(R.string.dock_loading),
    )
    val entries = remember(ui.isRadio, ui.upcoming, stations) {
        if (ui.isRadio) stations.take(DOCK_STATIONS).map { DockEntry(it.uri, it.name, "", "", it.imageUrl, true) }
        else ui.upcoming.take(DOCK_ENTRIES).map { it.toDockEntry() }
    }
    // Radio: the song on top and the station under it; otherwise title and artist (both lines, never the artist alone).
    val songText = ui.stations.firstOrNull { it.isCurrent }?.nowPlaying.orEmpty()
    val (line1, line2) = barLines(ui.isRadio, ui.title.ifBlank { ui.mediaName }, ui.artist, songText)
    val drawerKey by shell.drawerKey.collectAsStateWithLifecycle()
    val hint = drawerKey?.let { keyHintLabel(it) }
    PlayerDock(
        state = ui.toMiniPlayerState().copy(title = line1, artist = line2),
        keyHint = hint,
        coverUrl = ui.coverUrl,
        isRadio = ui.isRadio,
        favorite = ui.favorite,
        entries = entries,
        entriesTitle = stringResource(if (ui.isRadio) R.string.dock_stations else R.string.dock_up_next),
        onPrevious = nowPlaying::onPrevious,
        onPlayPause = nowPlaying::onPlayPause,
        onNext = nowPlaying::onNext,
        onFavorite = nowPlaying::onFavorite,
        onOpenNowPlaying = onOpenNowPlaying,
        onEntryClick = { key -> if (ui.isRadio) shell.playStation(key) else key.toIntOrNull()?.let(nowPlaying::onPlayIndex) },
        onCollapse = onCollapse,
        modifier = modifier,
        barFocus = barFocus,
        labels = labels,
        onExpandedChange = { if (it) shell.refresh() },
        focusLocked = focusLocked,
    )
}

/** Short remote-button caption for the hint next to the bar (digits, colours and the common media/TV keys). */
@Composable
private fun keyHintLabel(code: Int): String = when {
    code in 7..16 -> (code - 7).toString()
    else -> KeyHintNames[code]?.let { stringResource(it) } ?: stringResource(R.string.key_hint_other)
}

private val KeyHintNames = mapOf(
    23 to R.string.key_hint_ok, 66 to R.string.key_hint_ok, 160 to R.string.key_hint_ok,
    85 to R.string.key_hint_play_pause, 126 to R.string.key_hint_play, 127 to R.string.key_hint_pause, 86 to R.string.key_hint_stop,
    166 to R.string.key_hint_ch_up, 167 to R.string.key_hint_ch_down,
    183 to R.string.key_hint_red, 184 to R.string.key_hint_green, 185 to R.string.key_hint_yellow, 186 to R.string.key_hint_blue,
    82 to R.string.key_hint_menu, 84 to R.string.key_hint_search, 165 to R.string.key_hint_info, 172 to R.string.key_hint_guide,
)

private fun QueueRow.toDockEntry(): DockEntry {
    val s = (durationMs / 1000).coerceAtLeast(0)
    val time = if (durationMs > 0) "%d:%02d".format(s / 60, s % 60) else ""
    return DockEntry(index.toString(), title, artist, time, coverUrl, false)
}
