package io.github.riberrr.spinroom.feature.home

import androidx.lifecycle.viewModelScope
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.QueueItem
import io.github.riberrr.spinroom.core.maapi.RecommendationFolder
import io.github.riberrr.spinroom.core.maapi.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeBackend : HomeBackend {
    val connectionState = MutableStateFlow<MaConnectionState>(MaConnectionState.Disconnected)
    val player = MutableStateFlow("")
    val queueState = MutableStateFlow<PlayerQueue?>(null)
    var recent: List<MediaItem> = emptyList()
    var playlistItems: List<MediaItem> = emptyList()
    var radios: List<MediaItem> = emptyList()
    var recs: List<RecommendationFolder> = emptyList()
    var failAll = false
    val played = mutableListOf<Pair<String, String>>()

    override val connection: Flow<MaConnectionState> = connectionState
    override val playerId: Flow<String> = player
    override val serverAddress: Flow<String> = MutableStateFlow("10.0.1.50:8095")
    override fun queue(queueId: String): Flow<PlayerQueue> = queueState.filterNotNull()

    private fun <T> guard(v: T): T = if (failAll) error("offline") else v
    override suspend fun recentlyPlayed() = guard(recent)
    override suspend fun playlists() = guard(playlistItems)
    override suspend fun favoriteRadios() = guard(radios)
    override suspend fun recommendations() = guard(recs)
    override suspend fun play(queueId: String, uri: String) { played += queueId to uri }
}

private fun album(id: String, name: String, artist: String) = MediaItem(
    itemId = id, provider = "library", mediaType = "album", name = name, uri = "library://album/$id",
    artists = listOf(MediaItem(name = artist)), image = MediaItemImage(path = "p$id", provider = "library"),
)

private fun connected() = MaConnectionState.Connected(ServerInfo(), authenticated = true)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun HomeViewModel.viewModelScopeCancel() = viewModelScope.cancel()

    @Test fun `rows fill once connected`() = runTest(dispatcher) {
        val b = FakeBackend().apply {
            recent = listOf(album("1", "Nocne światła", "Lena Wiatr"))
            playlistItems = listOf(MediaItem(itemId = "9", provider = "library", mediaType = "playlist", name = "Spokojny poranek", uri = "library://playlist/9"))
            radios = listOf(MediaItem(itemId = "5", provider = "radiobrowser", mediaType = "radio", name = "Jazz Jeden", uri = "library://radio/5"))
        }
        val vm = HomeViewModel(b)
        runCurrent()
        assertTrue(vm.state.value.loading)
        b.connectionState.value = connected()
        runCurrent()
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals("Nocne światła", s.recent.single().title)
        assertEquals("Lena Wiatr", s.recent.single().subtitle)
        assertNotNull(s.recent.single().coverUrl)
        assertTrue(s.playlists.single().isPlaylist)
        assertEquals("JJ", s.stations.single().initials)
        assertFalse(s.showYoutube)
        vm.viewModelScopeCancel()
    }

    @Test fun `youtube row only from ytmusic provider`() = runTest(dispatcher) {
        val mix = MediaItem(itemId = "m", provider = "ytmusic", mediaType = "playlist", name = "Mix dla Ciebie", uri = "ytmusic://playlist/m")
        val b = FakeBackend().apply {
            recs = listOf(
                RecommendationFolder(itemId = "a", provider = "library", name = "Random", items = listOf(album("2", "X", "Y"))),
                RecommendationFolder(itemId = "b", provider = "ytmusic", name = "Mixes", items = listOf(mix)),
            )
        }
        val vm = HomeViewModel(b)
        b.connectionState.value = connected()
        runCurrent()
        assertEquals(listOf("Mix dla Ciebie"), vm.state.value.recommended.map { it.title })
        assertTrue(vm.state.value.showYoutube)
        vm.viewModelScopeCancel()
    }

    @Test fun `all calls failing shows failure and retry reloads`() = runTest(dispatcher) {
        val b = FakeBackend().apply { failAll = true }
        val vm = HomeViewModel(b)
        b.connectionState.value = connected()
        runCurrent()
        assertTrue(vm.state.value.failed)
        b.failAll = false
        b.recent = listOf(album("1", "A", "B"))
        vm.onRetry()
        runCurrent()
        assertFalse(vm.state.value.failed)
        assertEquals(1, vm.state.value.recent.size)
        vm.viewModelScopeCancel()
    }

    @Test fun `continue card follows player queue`() = runTest(dispatcher) {
        val b = FakeBackend()
        val vm = HomeViewModel(b)
        b.player.value = "tv"
        b.queueState.value = PlayerQueue(
            queueId = "tv", items = 11, currentIndex = 3, elapsedTime = 60.0,
            currentItem = QueueItem(name = "Nocne światła", duration = 240, mediaItem = album("1", "Nocne światła", "Lena Wiatr").copy(duration = 240)),
        )
        runCurrent()
        val card = vm.state.value.continueCard
        assertNotNull(card)
        assertEquals(4, card!!.position)
        assertEquals(11, card.total)
        assertEquals(0.25f, card.progress, 0.001f)
        assertEquals("Lena Wiatr", card.artist)
        b.queueState.value = PlayerQueue(queueId = "tv")
        runCurrent()
        assertNull(vm.state.value.continueCard)
        vm.viewModelScopeCancel()
    }

    @Test fun `station plays on chosen player, nothing without player`() = runTest(dispatcher) {
        val b = FakeBackend()
        val vm = HomeViewModel(b)
        val station = StationTileState("k", "Jazz Jeden", "radiobrowser", "J1", "library://radio/5")
        vm.onPlayStation(station)
        runCurrent()
        assertTrue(b.played.isEmpty())
        b.player.value = "tv"
        runCurrent()
        vm.onPlayStation(station)
        runCurrent()
        assertEquals(listOf("tv" to "library://radio/5"), b.played)
        vm.viewModelScopeCancel()
    }
}
