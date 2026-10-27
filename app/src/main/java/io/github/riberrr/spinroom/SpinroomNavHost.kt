package io.github.riberrr.spinroom

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.riberrr.spinroom.feature.details.DetailsScreen
import io.github.riberrr.spinroom.feature.home.HomeScreen
import io.github.riberrr.spinroom.feature.library.LibraryScreen
import io.github.riberrr.spinroom.feature.nowplaying.NowPlayingScreen
import io.github.riberrr.spinroom.feature.search.SearchScreen
import io.github.riberrr.spinroom.feature.settings.SettingsScreen
import io.github.riberrr.spinroom.feature.sources.SourcesScreen

object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "nowplaying"
    const val LIBRARY = "library"
    const val SOURCES = "sources"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val DETAILS_ARG = "itemUri"
    const val DETAILS = "details/{$DETAILS_ARG}"

    /** [itemUri] is a Music Assistant URI such as `library://album/12`; it is URL-encoded for the route. */
    fun details(itemUri: String) = "details/${Uri.encode(itemUri)}"
}

/** App-level navigation graph. Start is the home screen. */
@Composable
fun SpinroomNavHost(navController: NavHostController = rememberNavController()) {
    SpinroomShell(navController) {
        NavHost(navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    onOpenSources = { navController.navigate(Routes.SOURCES) },
                    onOpenDetails = { uri -> navController.navigate(Routes.details(uri)) },
                )
            }
            composable(Routes.NOW_PLAYING) { NowPlayingScreen() }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.SOURCES) { SourcesScreen() }
            composable(Routes.SEARCH) { SearchScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                Routes.DETAILS,
                arguments = listOf(navArgument(Routes.DETAILS_ARG) { type = NavType.StringType }),
            ) { entry ->
                val itemUri = entry.arguments?.getString(Routes.DETAILS_ARG).orEmpty()
                DetailsScreen(itemUri = itemUri)
            }
        }
    }
}
