package io.github.riberrr.spinroom

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.SpinroomColors
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
    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { WithStubNav(navController) { HomeScreen() } }
        composable(Routes.NOW_PLAYING) { WithStubNav(navController) { NowPlayingScreen() } }
        composable(Routes.LIBRARY) { WithStubNav(navController) { LibraryScreen() } }
        composable(Routes.SOURCES) { WithStubNav(navController) { SourcesScreen() } }
        composable(Routes.SEARCH) { WithStubNav(navController) { SearchScreen() } }
        composable(Routes.SETTINGS) { WithStubNav(navController) { SettingsScreen() } }
        composable(
            Routes.DETAILS,
            arguments = listOf(navArgument(Routes.DETAILS_ARG) { type = NavType.StringType }),
        ) { entry ->
            val itemUri = entry.arguments?.getString(Routes.DETAILS_ARG).orEmpty()
            WithStubNav(navController) { DetailsScreen(itemUri = itemUri) }
        }
    }
}

/** Temporary D-pad route switcher so the stubs can be walked through; each real screen replaces it with its own navigation. */
@Composable
private fun WithStubNav(navController: NavHostController, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StubNavButton("Start") { navController.navigate(Routes.HOME) }
            StubNavButton("Teraz gra") { navController.navigate(Routes.NOW_PLAYING) }
            StubNavButton("Biblioteka") { navController.navigate(Routes.LIBRARY) }
            StubNavButton("Źródła") { navController.navigate(Routes.SOURCES) }
            StubNavButton("Szukaj") { navController.navigate(Routes.SEARCH) }
            StubNavButton("Ustawienia") { navController.navigate(Routes.SETTINGS) }
            StubNavButton("Szczegóły") { navController.navigate(Routes.details("library://album/1")) }
        }
        content()
    }
}

@Composable
private fun StubNavButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(label) }
}
