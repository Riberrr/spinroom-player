package io.github.riberrr.spinroom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.CompositionLocalProvider
import io.github.riberrr.spinroom.core.design.BackdropHost
import io.github.riberrr.spinroom.core.design.HostedBackdrop
import io.github.riberrr.spinroom.core.design.LocalBackdropHost
import io.github.riberrr.spinroom.core.design.SideMenu
import io.github.riberrr.spinroom.core.design.SideMenuItem
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.RAIL_WIDTH
import io.github.riberrr.spinroom.feature.nowplaying.activityNowPlayingViewModel
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.PlayerPill
import io.github.riberrr.spinroom.feature.nowplaying.PlayerChoice

/**
 * App chrome: side menu on the left, [content] (the nav host) in the middle, mini player at the bottom.
 * D-pad left on the content's left edge opens the menu (Up/Down never leave the content); Back inside the menu returns focus to the content.
 * The mini player shows the shared now-playing state and hides itself when nothing is playing.
 */
@Composable
fun SpinroomShell(navController: NavHostController, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val menuFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val screenFocus = remember { FocusRequester() }
    val dockFocus = remember { FocusRequester() }
    val pillFocus = remember { FocusRequester() }
    val shell: ShellViewModel = hiltViewModel()
    val nowPlaying = activityNowPlayingViewModel()
    var pickerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shell.openDrawer.collect { android.util.Log.i("Spinroom", "drawer request"); android.util.Log.i("Spinroom", "dock requestFocus=" + runCatching { dockFocus.requestFocus() }) } }
    LaunchedEffect(Unit) { shell.navigate.collect { target -> navController.openTopLevel(target) } }
    var menuHasFocus by remember { mutableStateOf(false) }
    var menuOpenedByUser by remember { mutableStateOf(false) }
    var leftEdgeHit by remember { mutableStateOf(false) }
    var menuItemId by remember { mutableStateOf("") }

    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val baseItems = listOf(
        SideMenuItem(Routes.HOME, stringResource(R.string.menu_home), SpinroomIcons.Home),
        SideMenuItem(Routes.RADIO, stringResource(R.string.menu_radio), SpinroomIcons.Radio),
        SideMenuItem(Routes.PLAYLISTS, stringResource(R.string.menu_playlists), SpinroomIcons.Playlists),
        SideMenuItem(Routes.LIBRARY, stringResource(R.string.menu_library), SpinroomIcons.Library),
        SideMenuItem(Routes.SEARCH, stringResource(R.string.menu_search), SpinroomIcons.Search),
    )
    // The player entry exists only while something plays; it opens the full-screen player (Back returns).
    val npState by nowPlaying.state.collectAsState()
    val hasTrack = npState.hasTrack
    val fullPlayer = route == Routes.NOW_PLAYING
    val items = if (hasTrack && !fullPlayer) baseItems + SideMenuItem(MENU_PLAYER, stringResource(R.string.menu_player), SpinroomIcons.Player) else baseItems
    val pillName = npState.playerName.ifBlank { stringResource(R.string.player_pill_none) }
    val pillConnected = npState.isConnected && npState.players.firstOrNull { it.selected }?.available != false
    val bottom = listOf(SideMenuItem(Routes.SETTINGS, stringResource(R.string.menu_settings), SpinroomIcons.Settings))
    // Full-screen player: the menu slides behind the left edge and is not composed, so it cannot take focus.
    LaunchedEffect(fullPlayer) { if (fullPlayer) { menuHasFocus = false; menuOpenedByUser = false } }
    // Coming back from the full-screen player nothing is focused, and the bar (first in the tree) would take focus: give it to the screen.
    var wasFull by remember { mutableStateOf(false) }
    LaunchedEffect(fullPlayer) {
        if (wasFull && !fullPlayer) { kotlinx.coroutines.delay(500); runCatching { screenFocus.requestFocus() } }
        wasFull = fullPlayer
    }
    val slot by androidx.compose.animation.core.animateDpAsState(if (fullPlayer) 0.dp else RAIL_WIDTH, label = "menuSlot")

    // Composed only while the menu has focus: a handler added on entering the composition is the newest one, so it
    // runs before the NavHost's own (registered earlier), which would otherwise pop the screen.
    if (menuHasFocus) BackHandler { runCatching { screenFocus.requestFocus() } }
    // Back on Start leaves the app for real (finish), instead of parking the task (Android 12+) with the last screen restored.
    val activity = androidx.activity.compose.LocalActivity.current
    BackHandler(enabled = route == Routes.HOME && !menuHasFocus) { activity?.finish() }

    // One backdrop for the whole window: screens report their cover/seed (ContentBackdrop) and it shows through the glass menu and dock.
    val backdropHost = remember { BackdropHost() }
    Box(Modifier.fillMaxSize().background(SpinroomColors.Bg)) {
    HostedBackdrop(backdropHost)
    CompositionLocalProvider(LocalBackdropHost provides backdropHost) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(slot).fillMaxHeight().zIndex(1f)) {
        androidx.compose.animation.AnimatedVisibility(
            !fullPlayer,
            enter = androidx.compose.animation.slideInHorizontally { -it },
            exit = androidx.compose.animation.slideOutHorizontally { -it },
        ) {
        Box {
        SideMenu(
            // → from the menu enters the screen itself (restored/first control). The default focus search would pick the
            // geometrically nearest button instead: the mini player, for the bottom "Ustawienia" item.
            topInset = 12.dp,
            header = { expanded ->
                PlayerPill(
                    pillName, pillConnected, expanded, onClick = { pickerOpen = true },
                    icon = { Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(32.dp).scale(1.35f)) },
                    modifier = Modifier.focusRequester(pillFocus).onFocusChanged { if (it.isFocused) menuItemId = "pill" },
                )
            },
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight && menuHasFocus) {
                    runCatching { screenFocus.requestFocus() }.isSuccess
                } else false
            },
            items = items,
            selectedId = route.orEmpty(),
            onSelect = { target ->
                android.util.Log.i("Spinroom", "menu select: $target")
                if (target == MENU_PLAYER) { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true }; return@SideMenu }
                // Always the entry's main view, even from a screen deeper in it.
                navController.openTopLevel(target)
            },
            bottomItems = bottom,
            onItemFocus = { menuItemId = it; android.util.Log.i("Spinroom", "menu focus: $it") },
            enterFocusRequester = menuFocus,
            onFocusChange = {
                menuHasFocus = it
                if (it) shell.refresh()
                if (it && !menuOpenedByUser) {
                    // Focus fell into the menu because a focused control vanished: keep it in the content.
                    runCatching { contentFocus.requestFocus() }
                }
                if (!it) menuOpenedByUser = false
            },
        )
        }
        }
        }
        Column(
            Modifier
                .weight(1f)
                // Focus never leaves the content geometrically (that would pick the menu item at the same
                // height); the menu opens only with Left, handled below, and always on the current screen.
                .focusRequester(contentFocus)
                .focusProperties {
                    onExit = {
                        // cancelFocusChange() still makes moveFocus() report success, so remember the edge hit.
                        if (requestedFocusDirection == FocusDirection.Left) leftEdgeHit = true
                        // Programmatic requestFocus() (menu open) comes with direction Enter and must pass.
                        if (requestedFocusDirection != FocusDirection.Enter) cancelFocusChange()
                    }
                }
                .focusGroup()
                .onKeyEvent { event ->
                    // Left with nowhere to go inside the screen -> open the menu. Bubble phase, so a
                    // focused control that uses Left itself (e.g. the latency slider) handles it first.
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        leftEdgeHit = false
                        focusManager.moveFocus(FocusDirection.Left)
                        if (leftEdgeHit && !fullPlayer) {
                            menuOpenedByUser = true
                            menuFocus.requestFocus()
                        }
                        true
                    } else false
                },
        ) {
            // Inner group: coming back from the menu restores the last focused control (the first one if it is gone).
            // Kept apart from the outer group because both would set onExit and the outer one wins.
            // Fallback is the screen itself: with nothing to restore (e.g. → from the bottom "Ustawienia" menu
            // item, geometrically closest to the mini player) focus must land in the content, not the mini player.
            Column(Modifier.fillMaxSize()) {
                // The player bar is a fixed 56 dp strip at the top of every screen (drawn over the content when it unfolds).
                if (route != Routes.NOW_PLAYING) {
                    ShellDock(
                        barFocus = dockFocus,
                        onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true } },
                        onCollapse = { runCatching { screenFocus.requestFocus() } },
                    )
                }
                // Only the screen is restored; the bar sits outside so it is never the restore target.
                // Up with nowhere left to go inside the screen jumps to the player bar; Down never leaves the screen
                // (it would land on the bar's geometry or nothing). A separate group, because the restorer below sets its own onExit.
                Column(
                    Modifier.weight(1f)
                        .focusProperties {
                            onExit = {
                                when (requestedFocusDirection) {
                                    FocusDirection.Down -> cancelFocusChange()
                                    FocusDirection.Up -> { cancelFocusChange(); if (route != Routes.NOW_PLAYING && hasTrack) runCatching { dockFocus.requestFocus() } }
                                    else -> Unit
                                }
                            }
                        }
                        .focusGroup(),
                ) {
                    Column(Modifier.fillMaxSize().focusRequester(screenFocus).focusRestorer()) { content() }
                }
            }
        }
    }
    if (pickerOpen) {
        PlayerPicker(
            players = npState.players,
            onPick = { id -> pickerOpen = false; nowPlaying.onSelectPlayer(id); runCatching { screenFocus.requestFocus() } },
            onClose = { pickerOpen = false },
        )
    }
    }
    }
}

/** The list of MA players (opened from the pill at the top of the menu); picking one makes it the one this app controls. */
@Composable
private fun PlayerPicker(players: List<PlayerChoice>, onPick: (String) -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    BackHandler { onClose() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().zIndex(10f).background(SpinroomColors.Bg.copy(alpha = 0.78f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(420.dp).background(SpinroomColors.Surface, RoundedCornerShape(20.dp)).padding(24.dp).focusGroup(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.player_picker_title), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            val start = players.indexOfFirst { it.selected }.coerceAtLeast(0)
            players.forEachIndexed { i, p ->
                FocusSurface(
                    { onPick(p.id) }, Modifier.fillMaxWidth().then(if (i == start) Modifier.focusRequester(first) else Modifier),
                    containerColor = if (p.selected) SpinroomColors.Accent.copy(alpha = 0.16f) else SpinroomColors.SurfaceHigh,
                    focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1f,
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(if (p.available) SpinroomColors.Success else SpinroomColors.TextSecondary, CircleShape))
                        Text(p.name, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!p.available) Text(stringResource(R.string.player_picker_offline), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Menu / remote shortcut target: always the main view of [target], whatever is on top of it now.
 * Start pops back to the root; the others replace everything above Start, so Back from them returns to Start.
 */
internal fun NavHostController.openTopLevel(target: String) {
    if (target == Routes.HOME) {
        popBackStack(Routes.HOME, inclusive = false)
        return
    }
    navigate(target) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/** Pseudo-route of the rail entry that opens the full-screen player. */
private const val MENU_PLAYER = "player"
