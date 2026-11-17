package io.github.riberrr.spinroom.feature.search

import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.SearchResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private class FakeBackend : SearchBackend {
        val queries = mutableListOf<String>()
        val played = mutableListOf<String>()
        var canPlay = true
        var fail = false
        var timeout = false
        var results = SearchResults()

        override val serverAddress = flowOf("http://srv:8095")
        override suspend fun search(query: String): SearchResults {
            queries += query
            // Like MaClient.command without a connection: waits for Connected until the timeout fires.
            if (timeout) withTimeout(1_000) { awaitCancellation() }
            if (fail) error("offline")
            return results
        }
        val favCalls = mutableListOf<Pair<String, Boolean>>()
        var favOk = true
        override suspend fun setFavorite(uri: String, favorite: Boolean): Boolean {
            favCalls += uri to favorite
            return favOk
        }
        val remoteFavorites = mutableMapOf<String, Boolean>()
        override suspend fun isFavorite(uri: String): Boolean? = remoteFavorites[uri]
        override suspend fun play(uri: String): Boolean {
            if (canPlay) played += uri
            return canPlay
        }
    }

    private val backend = FakeBackend()

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(name: String, type: String, provider: String, artist: String = "", album: String = "") = MediaItem(
        itemId = name, provider = provider, mediaType = type, name = name, uri = "$provider://$type/$name",
        artists = if (artist.isEmpty()) emptyList() else listOf(MediaItem(name = artist)),
        album = if (album.isEmpty()) null else MediaItem(name = album),
    )

    private suspend fun TestScope.vmTest(block: suspend TestScope.(SearchViewModel) -> Unit) {
        val vm = SearchViewModel(backend, debounceMs = 400)
        runCurrent()
        try {
            block(vm)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun debounceSearchesOnlyOnceForFastTyping() = runTest {
        vmTest { vm ->
            "kwa".forEach { vm.onType(it.toString()); advanceTimeBy(100) }
            assertTrue(backend.queries.isEmpty())
            advanceTimeBy(500); runCurrent()
            assertEquals(listOf("kwa"), backend.queries)
            assertEquals("kwa", vm.state.value.query)
        }
    }

    @Test fun blankQueryDoesNotHitServerAndClearsResults() = runTest {
        backend.results = SearchResults(tracks = listOf(item("Port 14", "track", "ytmusic--a")))
        vmTest { vm ->
            vm.onType("p"); advanceTimeBy(500); runCurrent()
            assertEquals(1, vm.state.value.tracks.size)
            vm.onBackspace(); advanceUntilIdle()
            assertEquals(listOf("p"), backend.queries)
            assertTrue(vm.state.value.isIdle)
            assertTrue(vm.state.value.tracks.isEmpty())
        }
    }

    @Test fun resultsAreGroupedWithSourceAndBestResult() = runTest {
        backend.results = SearchResults(
            artists = listOf(item("Kwartet Północ", "artist", "filesystem_local--1")),
            tracks = listOf(
                item("Port 14", "track", "filesystem_local--1", "Kwartet Północ", "Horyzont"),
                item("Kwartet na żywo", "track", "ytmusic--2", "Kwartet Północ"),
            ),
        )
        vmTest { vm ->
            vm.onType("kwartet"); advanceTimeBy(500); runCurrent()
            val s = vm.state.value
            assertEquals("Pliki lokalne", s.tracks[0].source)
            assertEquals("YouTube Music", s.tracks[1].source)
            assertEquals("Kwartet Północ · Horyzont", s.tracks[0].subtitle)
            vm.onFilter(SearchFilter.TRACKS)
            assertEquals(2, vm.state.value.shownTracks.size)
            assertTrue(vm.state.value.shownArtists.isEmpty())
        }
    }

    @Test fun failureSetsErrorAndRetryRecovers() = runTest {
        backend.fail = true
        vmTest { vm ->
            vm.onType("a"); advanceTimeBy(500); runCurrent()
            assertTrue(vm.state.value.failed)
            backend.fail = false
            backend.results = SearchResults(tracks = listOf(item("A", "track", "x")))
            vm.onRetry(); runCurrent()
            assertTrue(!vm.state.value.failed)
            assertEquals(1, vm.state.value.tracks.size)
        }
    }

    @Test fun offlineTimeoutEndsInFailedStateNotSpinner() = runTest {
        backend.timeout = true
        vmTest { vm ->
            vm.onType("a"); advanceTimeBy(500); runCurrent()
            assertTrue(vm.state.value.loading)
            advanceUntilIdle()
            assertTrue(vm.state.value.failed)
            assertTrue(!vm.state.value.loading)
            // The flow keeps working after the failure: a later query still runs.
            backend.timeout = false
            backend.results = SearchResults(tracks = listOf(item("A", "track", "x")))
            vm.onType("b"); advanceTimeBy(500); runCurrent()
            assertEquals(listOf("a", "ab"), backend.queries)
            assertEquals(1, vm.state.value.tracks.size)
        }
    }

    @Test fun voiceResultSetsQueryAndPlayNeedsPlayer() = runTest {
        vmTest { vm ->
            vm.onVoiceResult("  Port 14 ")
            assertEquals("Port 14", vm.state.value.query)
            advanceTimeBy(500); runCurrent()
            assertEquals(listOf("Port 14"), backend.queries)

            val row = SearchRow(item("Port 14", "track", "x"), ResultKind.TRACK, "Port 14", "", "x", null)
            backend.canPlay = false
            vm.onPlay(row); runCurrent()
            assertEquals(SearchUiState.Notice.NO_PLAYER, vm.state.value.notice)
            vm.onNoticeShown()
            backend.canPlay = true
            vm.onPlay(row); runCurrent()
            assertEquals(listOf("x://track/Port 14"), backend.played)
            assertNull(vm.state.value.notice)
        }
    }

    @Test fun sourceLabels() {
        assertEquals("Pliki lokalne", SearchViewModel.sourceLabel("filesystem_local--ab12"))
        assertEquals("Biblioteka", SearchViewModel.sourceLabel("library"))
        assertEquals("Some thing", SearchViewModel.sourceLabel("some_thing--x"))
    }

    private fun favItem(name: String, type: String, provider: String = "x", fav: Boolean = false, year: Int? = null) =
        item(name, type, provider).copy(favorite = fav, year = year)

    @Test fun favoriteToggleShowsToastAndUndoReverts() = runTest {
        backend.results = SearchResults(radio = listOf(favItem("Radio Jazz", "radio")))
        vmTest { vm ->
            vm.onType("jazz"); advanceTimeBy(500); runCurrent()
            val row = vm.state.value.radio[0]
            assertTrue(!vm.state.value.isFavorite(row))
            vm.onToggleFavorite(row); runCurrent()
            assertTrue(vm.state.value.isFavorite(row))
            assertEquals(FavoriteToast(row.uri, "Radio Jazz", true), vm.state.value.toast)
            assertEquals(listOf(row.uri to true), backend.favCalls)
            vm.onUndo(); runCurrent()
            assertTrue(!vm.state.value.isFavorite(row))
            assertNull(vm.state.value.toast)
            assertEquals(listOf(row.uri to true, row.uri to false), backend.favCalls)
        }
    }

    @Test fun removingExistingFavoriteAndToastExpires() = runTest {
        backend.results = SearchResults(playlists = listOf(favItem("Jazz na wieczór", "playlist", fav = true)))
        vmTest { vm ->
            vm.onType("jazz"); advanceTimeBy(500); runCurrent()
            val row = vm.state.value.playlists[0]
            assertTrue(vm.state.value.isFavorite(row))
            vm.onToggleFavorite(row); runCurrent()
            assertTrue(!vm.state.value.isFavorite(row))
            assertEquals(false, vm.state.value.toast?.added)
            advanceTimeBy(SearchViewModel.TOAST_MS + 100); runCurrent()
            assertNull(vm.state.value.toast)
        }
    }

    @Test fun failedFavoriteIsRolledBack() = runTest {
        backend.results = SearchResults(radio = listOf(favItem("Radio Jazz", "radio")))
        backend.favOk = false
        vmTest { vm ->
            vm.onType("jazz"); advanceTimeBy(500); runCurrent()
            val row = vm.state.value.radio[0]
            vm.onToggleFavorite(row); runCurrent()
            assertTrue(!vm.state.value.isFavorite(row))
            assertNull(vm.state.value.toast)
            assertEquals(SearchUiState.Notice.FAVORITE_FAILED, vm.state.value.notice)
        }
    }

    @Test fun categoriesAndFiltersNarrowResults() = runTest {
        backend.results = SearchResults(
            radio = listOf(favItem("Radio Jazz", "radio", "tunein--1", fav = true)),
            playlists = listOf(favItem("Jazz na wieczór", "playlist", "spotify--1")),
            albums = listOf(favItem("Jazzowy Port", "album", "spotify--1", year = 1996), favItem("Blue", "album", "filesystem_local--1", year = 2011)),
        )
        vmTest { vm ->
            vm.onType("jazz"); advanceTimeBy(500); runCurrent()
            assertEquals(1, vm.state.value.shownRadio.size)
            vm.onFilter(SearchFilter.PLAYLISTS)
            assertEquals(1, vm.state.value.shownPlaylists.size)
            assertTrue(vm.state.value.shownAlbums.isEmpty() && vm.state.value.shownRadio.isEmpty())
            vm.onFilter(SearchFilter.ALL)
            vm.onToggleSource("spotify")
            assertEquals(listOf("Jazzowy Port"), vm.state.value.shownAlbums.map { it.title })
            vm.onToggleDecade(1990)
            assertEquals(2, vm.state.value.filters.activeCount)
            assertEquals(listOf("Jazzowy Port"), vm.state.value.shownAlbums.map { it.title })
            assertTrue(vm.state.value.shownPlaylists.isEmpty())
            vm.onClearFilters()
            vm.onToggleFavoritesOnly()
            assertEquals(listOf("Radio Jazz"), vm.state.value.shownRadio.map { it.title })
            assertTrue(vm.state.value.shownAlbums.isEmpty())
            assertEquals(listOf("spotify", "filesystem_local", "tunein"), vm.state.value.sources.map { it.domain })
        }
    }

    @Test fun physicalKeyboardMapping() {
        assertEquals(PhysicalInput.Text("a"), mapPhysicalKey(false, false, 'A'.code, false))
        assertEquals(PhysicalInput.Text("ł"), mapPhysicalKey(false, false, 'ł'.code, false))
        assertEquals(PhysicalInput.Text(" "), mapPhysicalKey(false, false, ' '.code, false))
        assertEquals(PhysicalInput.Text("7"), mapPhysicalKey(false, false, '7'.code, false))
        assertEquals(PhysicalInput.Backspace, mapPhysicalKey(true, false, 0, false))
        assertEquals(PhysicalInput.Submit, mapPhysicalKey(false, true, '\n'.code, false))
        assertNull(mapPhysicalKey(false, false, 0, false))        // D-pad
        assertNull(mapPhysicalKey(false, false, 'c'.code, true))  // Ctrl+C
        assertNull(mapPhysicalKey(false, false, 0x1B, false))     // control char
        assertNull(mapPhysicalKey(false, false, '~'.code, false)) // not offered on the on-screen keyboard
    }

    @Test fun physicalInputEditsQueryAndSubmitSearchesAtOnce() = runTest {
        vmTest { vm ->
            "Jaz".forEach { vm.onPhysicalInput(mapPhysicalKey(false, false, it.code, false)!!) }
            vm.onPhysicalInput(PhysicalInput.Backspace)
            vm.onPhysicalInput(PhysicalInput.Text("z"))
            assertEquals("jaz", vm.state.value.query)
            vm.onPhysicalInput(PhysicalInput.Submit); runCurrent()
            assertEquals(listOf("jaz"), backend.queries)
        }
    }

    @Test fun selectingRowLoadsRealFavoriteFlagButKeepsUserChange() = runTest {
        backend.results = SearchResults(radio = listOf(favItem("Radio Jazz", "radio"), favItem("Jazz FM", "radio")))
        backend.remoteFavorites["x://radio/Radio Jazz"] = true
        backend.remoteFavorites["x://radio/Jazz FM"] = true
        vmTest { vm ->
            vm.onType("jazz"); advanceTimeBy(500); runCurrent()
            val (a, b) = vm.state.value.radio
            vm.onToggleFavorite(b); runCurrent() // user adds b first (server said it was already a favorite)
            vm.onSelect(a); vm.onSelect(b); runCurrent()
            assertTrue(vm.state.value.isFavorite(a))
            assertTrue(vm.state.value.isFavorite(b))
            vm.onToggleFavorite(a); runCurrent()
            assertTrue(!vm.state.value.isFavorite(a))
        }
    }
}
