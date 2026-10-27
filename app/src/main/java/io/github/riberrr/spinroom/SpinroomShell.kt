package io.github.riberrr.spinroom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
 * D-pad left on the content's left edge opens the menu; Back inside the menu returns focus to the content.
 * The mini player shows the shared now-playing state and hides itself when nothing is playing.
 */
@Composable
fun SpinroomShell(navController: NavHostController, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val menuFocus = remember { FocusRequester() }
    var menuHasFocus by remember { mutableStateOf(false) }

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
            firstItemFocusRequester = menuFocus,
            onFocusChange = { menuHasFocus = it },
        )
        Column(
            Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    // Left with nowhere to go inside the screen -> open the menu.
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        if (focusManager.moveFocus(FocusDirection.Left)) true else {
                            menuFocus.requestFocus()
                            true
                        }
                    } else false
                },
        ) {
            Column(Modifier.weight(1f)) { content() }
            if (route != Routes.NOW_PLAYING) {
                NowPlayingMiniPlayer(onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true } })
            }
        }
    }
}
