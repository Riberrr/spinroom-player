package io.github.riberrr.spinroom.feature.nowplaying

import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.QueueItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Scriptable [PlayerBackend]: tests push queue/player data and read back the commands that were sent. */
private class FakeBackend : PlayerBackend {
    val connectionState = MutableStateFlow<MaConnectionState>(MaConnectionState.Disconnected)
    val selected = MutableStateFlow("")
    val address = MutableStateFlow("10.0.1.50:8095")
    val queueState = MutableStateFlow<PlayerQueue?>(null)
    val itemsState = MutableStateFlow<List<QueueItem>>(emptyList())
    val playersState = MutableStateFlow<List<Player>>(emptyList())
    val commands = mutableListOf<String>()
    var failFavorite = false

    override val connection = connectionState
    override val selectedPlayerId = selected
    override val serverAddress = address
    override fun queue(queueId: String) = queueState.map { it ?: PlayerQueue(queueId = queueId) }
    override fun queueItems(queueId: String) = itemsState
    override fun players() = playersState

    override suspend fun selectPlayer(playerId: String) { commands += "select:$playerId"; selected.value = playerId }
    override suspend fun playPause(queueId: String) { commands += "playPause:$queueId" }
    override suspend fun next(queueId: String) { commands += "next:$queueId" }
    override suspend fun previous(queueId: String) { commands += "previous:$queueId" }
    override suspend fun setShuffle(queueId: String, enabled: Boolean) { commands += "shuffle:$enabled" }
    override suspend fun setRepeat(queueId: String, mode: String) { commands += "repeat:$mode" }
    override suspend fun playIndex(queueId: String, index: Int) { commands += "playIndex:$index" }
    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (failFavorite) error("offline")
        commands += "favorite:${item.itemId}:$favorite"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var backend: FakeBackend

    @Before fun setUp() { Dispatchers.setMain(dispatcher); backend = FakeBackend() }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun track(i: Int, favorite: Boolean = false) = QueueItem(
        queueId = "tv", queueItemId = "item$i", name = "Utwór $i", duration = 200 + i, index = i,
        mediaItem = MediaItem(
            itemId = "t$i", provider = "library", mediaType = "track", name = "Utwór $i", uri = "library://track/$i", favorite = favorite,
            duration = 200 + i, artists = listOf(MediaItem(name = "Lena Wiatr")), album = MediaItem(name = "Nocne światła"),
            image = MediaItemImage(type = "thumb", path = "cover$i.jpg", provider = "library"),
        ),
    )

    private fun queue(index: Int = 0, state: String = "playing", elapsed: Double = 84.0, shuffle: Boolean = false, repeat: String = "off", favorite: Boolean = false) =
        PlayerQueue(
            queueId = "tv", state = state, elapsedTime = elapsed, currentIndex = index, shuffleEnabled = shuffle, repeatMode = repeat,
            items = 5, currentItem = track(index, favorite),
        )

    private val created = mutableListOf<NowPlayingViewModel>()

    /** The progress ticker never goes idle, so cancel every ViewModel scope before runTest drains the scheduler. */
    private fun vmTest(body: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { body() } finally { created.forEach { it.viewModelScope.cancel() } }
    }

    private fun TestScope.viewModel(): NowPlayingViewModel =
        NowPlayingViewModel(backend, { testScheduler.currentTime }, 1_000).also { created += it }

    private fun TestScope.playing(): NowPlayingViewModel {
        backend.selected.value = "tv"
        backend.itemsState.value = List(5) { track(it) }
        backend.queueState.value = queue()
        return viewModel().also { runCurrent() }
    }

    @Test fun withoutPlayerNothingIsShown() = vmTest {
        val vm = viewModel()
        runCurrent()
        assertFalse(vm.state.value.hasPlayer)
        assertFalse(vm.state.value.hasTrack)
    }

    @Test fun mapsQueueToUiState() = vmTest {
        backend.playersState.value = listOf(Player(playerId = "tv", name = "Salon TV", volumeLevel = 65))
        val vm = playing()
        val s = vm.state.value
        assertTrue(s.hasPlayer && s.hasTrack)
        assertEquals("Salon TV", s.playerName)
        assertEquals(65, s.volume)
        assertEquals("Utwór 0", s.title)
        assertEquals("Lena Wiatr", s.artist)
        assertEquals("Nocne światła", s.album)
        assertEquals(200_000L, s.durationMs)
        assertEquals(84_000L, s.positionMs)
        assertTrue(s.isPlaying)
        assertTrue(s.coverUrl!!.contains("/imageproxy?path=cover0.jpg"))
        assertEquals(listOf(1, 2, 3), s.upcoming.map { it.index })
        assertEquals(5, s.queue.size)
        assertTrue(s.queue[0].isCurrent)
        assertEquals(201_000L, s.queue[1].durationMs)
    }

    @Test fun progressAdvancesLocallyWhilePlayingAndStopsOnPause() = vmTest {
        val vm = playing()
        advanceTimeBy(3_000); runCurrent()
        assertEquals(87_000L, vm.state.value.positionMs)

        backend.queueState.value = queue(state = "paused", elapsed = 90.0)
        runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(90_000L, vm.state.value.positionMs)
        assertFalse(vm.state.value.isPlaying)
    }

    @Test fun newEventResyncsProgress() = vmTest {
        val vm = playing()
        advanceTimeBy(10_000); runCurrent()
        backend.queueState.value = queue(elapsed = 20.0)
        runCurrent()
        assertEquals(20_000L, vm.state.value.positionMs)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(22_000L, vm.state.value.positionMs)
    }

    @Test fun progressNeverExceedsDuration() = vmTest {
        val vm = playing()
        backend.queueState.value = queue(elapsed = 199.0)
        runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(200_000L, vm.state.value.positionMs)
    }

    @Test fun playPauseIsOptimisticAndSendsCommand() = vmTest {
        val vm = playing()
        advanceTimeBy(2_000); runCurrent()
        vm.onPlayPause()
        assertFalse(vm.state.value.isPlaying)
        assertEquals(86_000L, vm.state.value.positionMs)
        runCurrent()
        assertEquals(listOf("playPause:tv"), backend.commands)
    }

    @Test fun transportCommandsTargetTheSelectedQueue() = vmTest {
        val vm = playing()
        vm.onNext(); vm.onPrevious(); vm.onPlayIndex(3)
        runCurrent()
        assertEquals(listOf("next:tv", "previous:tv", "playIndex:3"), backend.commands)
    }

    @Test fun shuffleAndRepeatToggle() = vmTest {
        val vm = playing()
        vm.onShuffle()
        vm.onRepeat(); vm.onRepeat(); vm.onRepeat()
        runCurrent()
        assertTrue(vm.state.value.shuffle)
        assertEquals(RepeatMode.OFF, vm.state.value.repeat)
        assertEquals(listOf("shuffle:true", "repeat:all", "repeat:one", "repeat:off"), backend.commands)
    }

    @Test fun repeatModeComesFromServer() = vmTest {
        val vm = playing()
        backend.queueState.value = queue(repeat = "one", shuffle = true)
        runCurrent()
        assertEquals(RepeatMode.ONE, vm.state.value.repeat)
        assertTrue(vm.state.value.shuffle)
    }

    @Test fun favoriteIsOptimisticAndKeptUntilTrackChanges() = vmTest {
        val vm = playing()
        vm.onFavorite()
        runCurrent()
        assertTrue(vm.state.value.favorite)
        assertEquals(listOf("favorite:t0:true"), backend.commands)

        // Server event for the same track that has not caught up yet must not flip it back.
        backend.queueState.value = queue(elapsed = 95.0)
        runCurrent()
        assertTrue(vm.state.value.favorite)

        backend.queueState.value = queue(index = 1)
        runCurrent()
        assertFalse(vm.state.value.favorite)
    }

    @Test fun favoriteRollsBackWhenServerFails() = vmTest {
        val vm = playing()
        backend.failFavorite = true
        vm.onFavorite()
        runCurrent()
        assertFalse(vm.state.value.favorite)
    }

    @Test fun queueExpandAndCollapse() = vmTest {
        val vm = playing()
        vm.onToggleQueue()
        assertTrue(vm.state.value.queueExpanded)
        vm.onCollapseQueue()
        assertFalse(vm.state.value.queueExpanded)
    }

    @Test fun selectingAnotherPlayerResetsPlaybackAndPersists() = vmTest {
        backend.playersState.value = listOf(Player(playerId = "tv", name = "TV"), Player(playerId = "kitchen", name = "Kuchnia"))
        val vm = playing()
        vm.onOpenPicker()
        assertTrue(vm.state.value.pickerOpen)
        backend.queueState.value = null
        vm.onSelectPlayer("kitchen")
        runCurrent()
        val s = vm.state.value
        assertFalse(s.pickerOpen)
        assertEquals("kitchen", s.playerId)
        assertEquals("Kuchnia", s.playerName)
        assertEquals("kitchen", s.players.single { it.selected }.id)
        assertFalse(s.hasTrack)
        assertNull(s.coverUrl)
        assertEquals(listOf("select:kitchen"), backend.commands)
    }

    @Test fun commandsAreIgnoredWithoutPlayer() = vmTest {
        val vm = viewModel()
        runCurrent()
        vm.onNext(); vm.onPlayPause()
        runCurrent()
        assertTrue(backend.commands.isEmpty())
    }
}
