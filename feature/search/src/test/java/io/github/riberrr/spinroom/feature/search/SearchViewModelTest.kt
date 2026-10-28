package io.github.riberrr.spinroom.feature.search

import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.SearchResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
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
        var results = SearchResults()

        override val serverAddress = flowOf("http://srv:8095")
        override suspend fun search(query: String): SearchResults {
            queries += query
            if (fail) error("offline")
            return results
        }
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
            assertEquals("Kwartet Północ", s.best?.title)
            assertEquals("Pliki lokalne", s.tracks[0].source)
            assertEquals("YouTube Music", s.tracks[1].source)
            assertEquals("Kwartet Północ · Horyzont", s.tracks[0].subtitle)
            vm.onFilter(SearchFilter.TRACKS)
            assertNull(vm.state.value.best)
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
}
