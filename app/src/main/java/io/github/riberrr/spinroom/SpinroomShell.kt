package io.github.riberrr.spinroom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.riberrr.spinroom.core.design.SideMenu
import io.github.riberrr.spinroom.core.design.SideMenuItem
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.feature.nowplaying.NowPlayingMiniPlayer

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
    var menuHasFocus by remember { mutableStateOf(false) }
    var menuOpenedByUser by remember { mutableStateOf(false) }
    var leftEdgeHit by remember { mutableStateOf(false) }

    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val items = listOf(
        SideMenuItem(Routes.HOME, stringResource(R.string.menu_home), SpinroomIcons.Home),
        SideMenuItem(Routes.LIBRARY, stringResource(R.string.menu_library), SpinroomIcons.Library),
        SideMenuItem(Routes.SOURCES, stringResource(R.string.menu_sources), SpinroomIcons.Sources),
        SideMenuItem(Routes.SEARCH, stringResource(R.string.menu_search), SpinroomIcons.Search),
    )
    val bottom = listOf(SideMenuItem(Routes.SETTINGS, stringResource(R.string.menu_settings), SpinroomIcons.Settings))

    BackHandler(enabled = menuHasFocus) { focusManager.moveFocus(FocusDirection.Right) }

    Row(Modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        SideMenu(
            items = items,
            selectedId = route.orEmpty(),
            onSelect = { target ->
                navController.navigate(target) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            bottomItems = bottom,
            enterFocusRequester = menuFocus,
            onFocusChange = {
                menuHasFocus = it
                if (it && !menuOpenedByUser) {
                    // Focus fell into the menu because a focused control vanished: keep it in the content.
                    runCatching { contentFocus.requestFocus() }
                }
                if (!it) menuOpenedByUser = false
            },
        )
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
                        if (leftEdgeHit) {
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
            Column(Modifier.fillMaxSize().focusRestorer(screenFocus)) {
                Column(Modifier.weight(1f).focusRequester(screenFocus)) { content() }
                if (route != Routes.NOW_PLAYING) {
                    NowPlayingMiniPlayer(onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true } })
                }
            }
        }
    }
}
