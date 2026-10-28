package io.github.riberrr.spinroom.feature.sources

import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

private fun folder(name: String, path: String) = MediaItem(itemId = name, provider = "p", mediaType = "folder", name = name, path = path, uri = path)
private fun radio(name: String) = MediaItem(itemId = name, provider = "p", mediaType = "radio", name = name, uri = "p://radio/$name", isPlayable = true)

private class FakeBackend : SourcesBackend {
    val conn = MutableStateFlow<MaConnectionState>(MaConnectionState.Connected(ServerInfo(), true))
    val player = MutableStateFlow("tv")
    val tree = mutableMapOf<String?, List<MediaItem>>()
    val failPaths = mutableSetOf<String?>()
    val commands = mutableListOf<String>()
    var failFavorite = false

    override val connection = conn
    override val playerId = player
    override val serverAddress = MutableStateFlow("10.0.1.50:8095")
    override suspend fun browse(path: String?): List<MediaItem> {
        commands += "browse:$path"
        if (path in failPaths) error("offline")
        return tree[path].orEmpty()
    }
    override suspend fun play(queueId: String, uri: String) { commands += "play:$queueId:$uri" }
    override suspend fun setFavorite(item: MediaItem, favorite: Boolean) {
        if (failFavorite) error("offline")
        commands += "fav:${item.name}:$favorite"
    }
}

class SourcesViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun backend() = FakeBackend().apply {
        tree[null] = listOf(folder("Radio Browser", "rb://"), folder("YouTube Music", "yt://"))
        tree["rb://"] = listOf(folder("Według kraju", "rb://country"), radio("Jazz Jeden"))
        tree["rb://country"] = listOf(radio("Klasyka 24"))
        tree["yt://"] = listOf(radio("Mix"))
    }

    private fun TestScope.vm(b: FakeBackend) = SourcesViewModel(b).also { runCurrent() }

    @Test fun `lists providers and opens the first one`() = runTest {
        val s = vm(backend()).state.value
        assertEquals(listOf("Radio Browser", "YouTube Music"), s.providers.map { it.name })
        assertEquals(listOf("Radio Browser"), s.trail.map { it.name })
        assertEquals(listOf("Według kraju", "Jazz Jeden"), s.items.map { it.name })
        assertTrue(s.items[0].isFolder)
        assertFalse(s.items[1].isFolder)
    }

    @Test fun `folder click extends breadcrumbs and back returns`() = runTest {
        val v = vm(backend())
        v.onItemClick(v.state.value.items[0]); runCurrent()
        assertEquals(listOf("Radio Browser", "Według kraju"), v.state.value.trail.map { it.name })
        assertEquals(listOf("Klasyka 24"), v.state.value.items.map { it.name })
        assertTrue(v.onBack()); runCurrent()
        assertEquals(1, v.state.value.trail.size)
        assertEquals(2, v.state.value.items.size)
        assertFalse(v.onBack())
    }

    @Test fun `crumb jumps up several levels`() = runTest {
        val b = backend().apply { tree["rb://country"] = listOf(folder("Polska", "rb://pl")); tree["rb://pl"] = listOf(radio("X")) }
        val v = vm(b)
        v.onItemClick(v.state.value.items[0]); runCurrent()
        v.onItemClick(v.state.value.items[0]); runCurrent()
        assertEquals(3, v.state.value.trail.size)
        v.onCrumb(0); runCurrent()
        assertEquals(listOf("Radio Browser"), v.state.value.trail.map { it.name })
    }

    @Test fun `selecting another provider switches tree`() = runTest {
        val v = vm(backend())
        v.onSelectProvider(1); runCurrent()
        assertEquals(listOf("Mix"), v.state.value.items.map { it.name })
        assertEquals("YouTube Music", v.state.value.trail.single().name)
    }

    @Test fun `playable item is sent to the selected player`() = runTest {
        val b = backend()
        val v = vm(b)
        v.onItemClick(v.state.value.items[1]); runCurrent()
        assertTrue("play:tv:p://radio/Jazz Jeden" in b.commands)
        assertEquals("Jazz Jeden", v.state.value.playedName)
    }

    @Test fun `no player chosen does not play`() = runTest {
        val b = backend().apply { player.value = "" }
        val v = vm(b)
        v.onItemClick(v.state.value.items[1]); runCurrent()
        assertTrue(v.state.value.noPlayer)
        assertTrue(b.commands.none { it.startsWith("play") })
    }

    @Test fun `failed folder shows error and retry reloads`() = runTest {
        val b = backend().apply { failPaths += "rb://" }
        val v = vm(b)
        assertTrue(v.state.value.itemsFailed)
        b.failPaths.clear()
        v.onRetry(); runCurrent()
        assertFalse(v.state.value.itemsFailed)
    }

    @Test fun `favorite is optimistic and rolled back on failure`() = runTest {
        val b = backend().apply { failFavorite = true }
        val v = vm(b)
        val row = v.state.value.items[1]
        v.onToggleFavorite(row)
        assertTrue(v.state.value.items[1].favorite)
        runCurrent()
        assertFalse(v.state.value.items[1].favorite)
        assertNull(v.state.value.playedName)
    }
}
