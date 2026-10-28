package io.github.riberrr.spinroom.feature.details

import io.github.riberrr.spinroom.core.maapi.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun track(n: Int) = MediaItem(itemId = "$n", mediaType = "track", name = "T$n", uri = "library://track/$n", duration = 180, artists = listOf(MediaItem(name = "Art")))

private class FakeBackend : DetailsBackend {
    val player = MutableStateFlow("p1")
    var fail = false
    var failFavorite = false
    val items = mutableMapOf<String, MediaItem>(
        "library://album/1" to MediaItem(itemId = "1", mediaType = "album", name = "Horyzont", uri = "library://album/1", year = 2024, artists = listOf(MediaItem(name = "Kwartet"))),
        "library://artist/2" to MediaItem(itemId = "2", mediaType = "artist", name = "Kwartet", uri = "library://artist/2"),
        "library://playlist/3" to MediaItem(itemId = "3", mediaType = "playlist", name = "Mix", uri = "library://playlist/3"),
    )
    data class Play(val player: String, val uris: List<String>, val option: String, val shuffle: Boolean?, val start: String?)
    val plays = mutableListOf<Play>()
    val favorites = mutableListOf<Boolean>()

    override val serverAddress: Flow<String> = MutableStateFlow("10.0.1.50:8095")
    override val selectedPlayerId: Flow<String> = player
    override suspend fun item(uri: String) = if (fail) error("boom") else items.getValue(uri)
    override suspend fun tracks(item: MediaItem) = (1..3).map(::track)
    override suspend fun artistAlbums(item: MediaItem) = listOf(items.getValue("library://album/1"))
    override suspend fun play(playerId: String, uris: List<String>, option: String, shuffle: Boolean?, startItem: String?) {
        plays += Play(playerId, uris, option, shuffle, startItem)
    }
    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (failFavorite) error("nope")
        favorites += favorite
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val backend = FakeBackend()

    private fun vm(uri: String) = DetailsViewModel(backend).also { it.load(uri) }

    @Test fun album_loads_tracks_and_meta() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(DetailsStatus.Content, s.status)
        assertEquals(DetailsKind.Album, s.kind)
        assertEquals("Kwartet", s.byline)
        assertEquals(3, s.trackCount)
        assertEquals(9, s.totalMinutes)
        assertEquals(listOf(1, 2, 3), s.tracks.map { it.number })
        assertTrue(s.albums.isEmpty())
    }

    @Test fun artist_has_albums_and_top_tracks() = runTest {
        val vm = vm("library://artist/2")
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(DetailsKind.Artist, s.kind)
        assertEquals(1, s.albums.size)
        assertEquals(3, s.tracks.size)
        assertEquals("", s.byline)
    }

    @Test fun playlist_kind() = runTest {
        val vm = vm("library://playlist/3")
        advanceUntilIdle()
        assertEquals(DetailsKind.Playlist, vm.state.value.kind)
    }

    @Test fun load_error_then_retry() = runTest {
        backend.fail = true
        val vm = vm("library://album/1")
        advanceUntilIdle()
        assertEquals(DetailsStatus.Error, vm.state.value.status)
        backend.fail = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(DetailsStatus.Content, vm.state.value.status)
    }

    @Test fun play_album_sends_album_uri() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onPlay()
        advanceUntilIdle()
        assertEquals(FakeBackend.Play("p1", listOf("library://album/1"), "play", false, null), backend.plays.single())
        assertEquals(DetailsMessage.Playing, vm.state.value.message)
    }

    @Test fun shuffle_and_queue() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onPlay(shuffle = true)
        vm.onAddToQueue()
        advanceUntilIdle()
        assertEquals(true, backend.plays[0].shuffle)
        assertEquals("add", backend.plays[1].option)
        assertEquals(DetailsMessage.Queued, vm.state.value.message)
    }

    @Test fun play_track_starts_at_it_with_all_tracks() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onPlay(track = vm.state.value.tracks[1])
        advanceUntilIdle()
        val p = backend.plays.single()
        assertEquals(listOf("library://track/1", "library://track/2", "library://track/3"), p.uris)
        assertEquals("library://track/2", p.start)
    }

    @Test fun artist_plays_top_tracks() = runTest {
        val vm = vm("library://artist/2")
        advanceUntilIdle()
        vm.onPlay()
        advanceUntilIdle()
        assertEquals(3, backend.plays.single().uris.size)
    }

    @Test fun no_player_selected_gives_hint_and_does_not_play() = runTest {
        backend.player.value = ""
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onPlay()
        advanceUntilIdle()
        assertTrue(backend.plays.isEmpty())
        assertEquals(DetailsMessage.NoPlayer, vm.state.value.message)
        vm.onMessageShown()
        assertEquals(null, vm.state.value.message)
    }

    @Test fun favorite_toggles_and_rolls_back_on_failure() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onToggleFavorite()
        advanceUntilIdle()
        assertTrue(vm.state.value.favorite)
        assertEquals(listOf(true), backend.favorites)
        backend.failFavorite = true
        vm.onToggleFavorite()
        advanceUntilIdle()
        assertTrue(vm.state.value.favorite)
        assertEquals(DetailsMessage.Failed, vm.state.value.message)
    }

    @Test fun same_uri_load_is_noop() = runTest {
        val vm = vm("library://album/1")
        advanceUntilIdle()
        vm.onToggleFavorite()
        vm.load("library://album/1")
        advanceUntilIdle()
        assertEquals(DetailsStatus.Content, vm.state.value.status)
        assertFalse(vm.state.value.tracks.isEmpty())
    }

    @Test fun duration_format() {
        assertEquals("4:12", formatDuration(252_000))
        assertEquals("0:05", formatDuration(5_000))
    }
}
