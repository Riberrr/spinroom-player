package io.github.riberrr.spinroom.feature.home

import androidx.lifecycle.viewModelScope
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import io.github.riberrr.spinroom.core.maapi.ProviderMapping
import io.github.riberrr.spinroom.core.maapi.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    var recent: List<MediaItem> = emptyList()
    var playlistItems: List<MediaItem> = emptyList()
    var radios: List<MediaItem> = emptyList()
    var failAll = false
    val played = mutableListOf<Pair<String, String>>()

    override val connection: Flow<MaConnectionState> = connectionState
    override val playerId: Flow<String> = player
    val address = MutableStateFlow("10.0.1.50:8095")
    val changes = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val serverAddress: Flow<String> = address
    override val libraryChanges: Flow<Unit> = changes

    private fun <T> guard(v: T): T = if (failAll) error("offline") else v
    override suspend fun recentlyPlayed() = guard(recent)
    override suspend fun playlists() = guard(playlistItems)
    override suspend fun favoriteRadios() = guard(radios)
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

    @Test fun `no server configured stops loading`() = runTest(dispatcher) {
        val b = FakeBackend().apply { address.value = "" }
        val vm = HomeViewModel(b)
        runCurrent()
        assertTrue(vm.state.value.notConfigured)
        assertFalse(vm.state.value.loading)
        vm.viewModelScopeCancel()
    }

    @Test fun `connecting keeps loading, failed connection shows failure`() = runTest(dispatcher) {
        val b = FakeBackend()
        val vm = HomeViewModel(b)
        b.connectionState.value = MaConnectionState.Connecting
        runCurrent()
        assertTrue(vm.state.value.loading)
        assertFalse(vm.state.value.notConfigured)
        b.connectionState.value = MaConnectionState.Failed(1, "nope")
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.failed)
        vm.viewModelScopeCancel()
    }

    @Test fun `library change and resume refresh rows quietly`() = runTest(dispatcher) {
        val b = FakeBackend().apply { radios = listOf(radio("1", "A")) }
        val vm = HomeViewModel(b)
        b.connectionState.value = connected()
        runCurrent()
        assertEquals(1, vm.state.value.stations.size)
        b.radios = emptyList()
        b.changes.tryEmit(Unit)
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(vm.state.value.stations.isEmpty())
        b.radios = listOf(radio("2", "B"), radio("3", "C"))
        vm.onResume()
        runCurrent()
        assertEquals(2, vm.state.value.stations.size)
        // a failing silent refresh keeps the old rows
        b.failAll = true
        vm.onResume()
        runCurrent()
        assertEquals(2, vm.state.value.stations.size)
        assertFalse(vm.state.value.loading)
        vm.viewModelScopeCancel()
    }

    @Test fun `station shows provider name from mappings and image, never library`() = runTest(dispatcher) {
        val b = FakeBackend().apply {
            radios = listOf(
                MediaItem(
                    itemId = "5", provider = "library", mediaType = "radio", name = "Jazz Jeden", uri = "library://radio/5",
                    providerMappings = listOf(ProviderMapping(itemId = "x", providerDomain = "radiobrowser", providerInstance = "radiobrowser--1")),
                    image = MediaItemImage(path = "p5", provider = "library"),
                ),
                MediaItem(itemId = "6", provider = "library", mediaType = "radio", name = "Bez mapy", uri = "library://radio/6"),
            )
        }
        val vm = HomeViewModel(b)
        b.connectionState.value = connected()
        runCurrent()
        val (c, a) = vm.state.value.stations // sorted by name: "Bez mapy" first
        assertEquals("Radio Browser", a.provider)
        assertNotNull(a.coverUrl)
        assertEquals("", c.provider)
        assertNull(c.coverUrl)
        vm.viewModelScopeCancel()
    }
}

private fun radio(id: String, name: String) = MediaItem(itemId = id, provider = "radiobrowser", mediaType = "radio", name = name, uri = "library://radio/$id")
private fun playlist(id: String, name: String) = MediaItem(itemId = id, provider = "library", mediaType = "playlist", name = name, uri = "library://playlist/$id")

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRowsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `stations and playlists are sorted by name ignoring case`() = runTest(dispatcher) {
        val b = FakeBackend().apply {
            radios = listOf(radio("1", "radio 357"), radio("2", "Jazz Jeden"), radio("3", "Klasyka 24"))
            playlistItems = listOf(playlist("1", "Impreza"), playlist("2", "do samochodu"), playlist("3", "Niedziela"))
        }
        val vm = HomeViewModel(b)
        b.connectionState.value = MaConnectionState.Connected(ServerInfo(), authenticated = true)
        runCurrent()
        assertEquals(listOf("Jazz Jeden", "Klasyka 24", "radio 357"), vm.state.value.stations.map { it.name })
        assertEquals(listOf("do samochodu", "Impreza", "Niedziela"), vm.state.value.playlists.map { it.title })
        vm.viewModelScope.cancel()
    }

    @Test fun `empty favorites still show stations and playlists rows, recent only with items`() = runTest(dispatcher) {
        val b = FakeBackend()
        val vm = HomeViewModel(b)
        b.connectionState.value = MaConnectionState.Connected(ServerInfo(), authenticated = true)
        runCurrent()
        val s = vm.state.value
        assertTrue(s.loaded)
        assertTrue(s.stations.isEmpty() && s.playlists.isEmpty())
        assertEquals(listOf(HomeRow.Stations, HomeRow.Playlists), s.visibleRows)
        b.recent = listOf(album("1", "A", "B"))
        vm.onResume()
        runCurrent()
        assertEquals(DEFAULT_ROW_ORDER, vm.state.value.visibleRows)
        vm.viewModelScope.cancel()
    }

    @Test fun `row order follows rowOrder`() {
        val s = HomeUiState(rowOrder = listOf(HomeRow.Recent, HomeRow.Playlists, HomeRow.Stations, HomeRow.Playlists), recent = listOf(HomeTile("k", "t", "", null, null)))
        assertEquals(listOf(HomeRow.Recent, HomeRow.Playlists, HomeRow.Stations), s.visibleRows)
    }

    @Test fun `failed load without any success is not loaded`() = runTest(dispatcher) {
        val b = FakeBackend().apply { failAll = true }
        val vm = HomeViewModel(b)
        b.connectionState.value = MaConnectionState.Connected(ServerInfo(), authenticated = true)
        runCurrent()
        assertFalse(vm.state.value.loaded)
        assertTrue(vm.state.value.failed)
        vm.viewModelScope.cancel()
    }
}
