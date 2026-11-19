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

private const val DOCK_ENTRIES = 3
private const val DOCK_STATIONS = 4

/**
 * [PlayerDock] bound to the shared now-playing state. Right-hand list: next queue items, or favorite stations while radio plays.
 * Draws nothing when no track. [barFocus] opens the dock from outside; [onCollapse] must restore the screen's focus.
 */
@Composable
fun ShellDock(
    barFocus: FocusRequester,
    onOpenNowPlaying: () -> Unit,
    onCollapse: () -> Unit,
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
    )
    val entries = remember(ui.isRadio, ui.upcoming, stations) {
        if (ui.isRadio) stations.take(DOCK_STATIONS).map { DockEntry(it.uri, it.name, "", "", it.imageUrl, true) }
        else ui.upcoming.take(DOCK_ENTRIES).map { it.toDockEntry() }
    }
    PlayerDock(
        state = ui.toMiniPlayerState(),
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
    )
}

private fun QueueRow.toDockEntry(): DockEntry {
    val s = (durationMs / 1000).coerceAtLeast(0)
    val time = if (durationMs > 0) "%d:%02d".format(s / 60, s % 60) else ""
    return DockEntry(index.toString(), title, artist, time, coverUrl, false)
}
