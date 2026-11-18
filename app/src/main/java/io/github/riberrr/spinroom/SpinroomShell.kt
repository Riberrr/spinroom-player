package io.github.riberrr.spinroom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import io.github.riberrr.spinroom.core.design.SideMenu
import io.github.riberrr.spinroom.core.design.SideMenuItem
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.RAIL_WIDTH
import io.github.riberrr.spinroom.feature.nowplaying.activityNowPlayingViewModel
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.riberrr.spinroom.core.design.PinnedEntry

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
    val shell: ShellViewModel = hiltViewModel()
    val stations by shell.stations.collectAsState()
    val playlists by shell.playlists.collectAsState()
    val pinned = remember(stations, playlists) { pinnedOf(stations, playlists) }
    val radioLabel = stringResource(R.string.pinned_radio)
    val playlistLabel = stringResource(R.string.pinned_playlist)
    LaunchedEffect(Unit) { shell.openDrawer.collect { runCatching { dockFocus.requestFocus() } } }
    LaunchedEffect(Unit) {
        shell.navigate.collect { target ->
            navController.navigate(target) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    var menuHasFocus by remember { mutableStateOf(false) }
    var menuOpenedByUser by remember { mutableStateOf(false) }
    var leftEdgeHit by remember { mutableStateOf(false) }
    var menuItemId by remember { mutableStateOf("") }

    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val items = listOf(
        SideMenuItem(Routes.HOME, stringResource(R.string.menu_home), SpinroomIcons.Home),
        SideMenuItem(Routes.RADIO, stringResource(R.string.menu_radio), SpinroomIcons.Radio),
        SideMenuItem(Routes.PLAYLISTS, stringResource(R.string.menu_playlists), SpinroomIcons.Playlists),
        SideMenuItem(Routes.LIBRARY, stringResource(R.string.menu_library), SpinroomIcons.Library),
        SideMenuItem(Routes.SEARCH, stringResource(R.string.menu_search), SpinroomIcons.Search),
    )
    // The player entry exists only while something plays; it jumps focus to the bottom dock (no scrolling down a long list).
    val hasTrack by activityNowPlayingViewModel().state.collectAsState().let { s -> androidx.compose.runtime.derivedStateOf { s.value.hasTrack } }
    val fullPlayer = route == Routes.NOW_PLAYING
    val bottom = buildList {
        if (hasTrack && !fullPlayer) add(SideMenuItem(MENU_PLAYER, stringResource(R.string.menu_player), SpinroomIcons.Player))
        add(SideMenuItem(Routes.SETTINGS, stringResource(R.string.menu_settings), SpinroomIcons.Settings))
    }
    // Full-screen player: the menu slides behind the left edge and is not composed, so it cannot take focus.
    LaunchedEffect(fullPlayer) { if (fullPlayer) { menuHasFocus = false; menuOpenedByUser = false } }
    val slot by androidx.compose.animation.core.animateDpAsState(if (fullPlayer) 0.dp else RAIL_WIDTH, label = "menuSlot")

    // Composed only while the menu has focus: a handler added on entering the composition is the newest one, so it
    // runs before the NavHost's own (registered earlier), which would otherwise pop the screen.
    if (menuHasFocus) BackHandler { runCatching { screenFocus.requestFocus() } }
    // Back on Start leaves the app for real (finish), instead of parking the task (Android 12+) with the last screen restored.
    val activity = androidx.activity.compose.LocalActivity.current
    BackHandler(enabled = route == Routes.HOME && !menuHasFocus) { activity?.finish() }

    Row(Modifier.fillMaxSize().background(SpinroomColors.Bg)) {
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
            modifier = Modifier.padding(top = 44.dp).onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight && menuHasFocus) {
                    if (menuItemId == MENU_PLAYER) { shell.requestDrawer(); return@onPreviewKeyEvent true }
                    runCatching { screenFocus.requestFocus() }.isSuccess
                } else false
            },
            items = items,
            selectedId = route.orEmpty(),
            onSelect = { target ->
                if (target == MENU_PLAYER) { shell.requestDrawer(); return@SideMenu }
                navController.navigate(target) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            bottomItems = bottom,
            pinned = pinned.take(MAX_PINNED).map { PinnedEntry(it.uri, it.name, if (it.isStation) radioLabel else playlistLabel, it.imageUrl, it.isStation) },
            pinnedTitle = stringResource(R.string.menu_pinned),
            onSelectPinned = { entry ->
                val fav = pinned.firstOrNull { it.uri == entry.id } ?: return@SideMenu
                runCatching { screenFocus.requestFocus() }
                if (fav.isStation) shell.playStation(fav.uri) else navController.navigate(Routes.details(fav.uri)) {
                    // Opening another item from the menu replaces the details on screen, so Back returns to where we came from.
                    if (route == Routes.DETAILS) popUpTo(Routes.DETAILS) { inclusive = true }
                }
            },
            onItemFocus = { menuItemId = it },
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
            MenuLogo(Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
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
                // Only the screen is restored; the mini player sits outside so it is never the restore target.
                Column(Modifier.weight(1f).focusRequester(screenFocus).focusRestorer()) { content() }
                if (route != Routes.NOW_PLAYING) {
                    ShellDock(
                        barFocus = dockFocus,
                        onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true } },
                        onCollapse = { runCatching { screenFocus.requestFocus() } },
                    )
                }
            }
        }
    }
}

/** App mark above the menu rail (mockup 03B): accent square with a note; no text, so the rail's width does not matter. */
@Composable
private fun MenuLogo(modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).background(SpinroomColors.Accent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text("♪", color = SpinroomColors.Bg, style = MaterialTheme.typography.titleLarge)
    }
}

/** The logo takes a row of the rail: with more pinned entries the expanded rail would push "Ustawienia" off screen. */
private const val MAX_PINNED = 4

/** Pseudo-route of the rail entry that moves focus to the bottom player. */
private const val MENU_PLAYER = "player"
