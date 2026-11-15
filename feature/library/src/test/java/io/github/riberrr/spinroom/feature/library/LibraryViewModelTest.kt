package io.github.riberrr.spinroom.feature.library

import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.riberrr.spinroom.core.maapi.ImageUrls
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import io.github.riberrr.spinroom.core.maapi.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

private fun album(name: String, artist: String = "Odcienie", image: String? = null) = MediaItem(
    itemId = name, provider = "library", mediaType = "album", name = name, uri = "library://album/$name",
    artists = listOf(MediaItem(name = artist)),
    image = image?.let { MediaItemImage(type = "thumb", path = it, provider = "library") },
)

private val READY = MaConnectionState.Connected(ServerInfo(), authenticated = true)

private class FakeBackend(val names: List<String>, initial: MaConnectionState = READY) : LibraryBackend {
    val address = MutableStateFlow("10.0.1.50:8095")
    val connectionState = MutableStateFlow(initial)
    val itemCalls = mutableListOf<Int>()
    val pagerTabs = mutableListOf<LibraryTab>()
    override val serverAddress: Flow<String> = address
    override val connection: Flow<MaConnectionState> = connectionState
    override fun pager(tab: LibraryTab): Flow<PagingData<MediaItem>> { pagerTabs += tab; return emptyFlow() }
    override suspend fun count(tab: LibraryTab) = if (tab == LibraryTab.Albums) names.size else 7
    override suspend fun itemAt(tab: LibraryTab, offset: Int): MediaItem? {
        itemCalls += offset
        return names.getOrNull(offset)?.let { album(it) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val names = listOf("1999", "Akwarele", "Bez pośpiechu", "Ciepły beton", "Dalekie brzegi", "Echo", "Horyzont", "Iskry", "Zorza")

    @Test fun `focus is remembered and a tab change resets it to the chips`() = runTest {
        val vm = LibraryViewModel(FakeBackend(names))
        runCurrent()
        assertEquals(LibraryFocus.Tab, vm.state.value.focus)
        vm.onFocused(LibraryFocus.Item(4))
        runCurrent()
        assertEquals(LibraryFocus.Item(4), vm.state.value.focus)
        vm.onSelectTab(LibraryTab.Artists)
        runCurrent()
        assertEquals(LibraryFocus.Tab, vm.state.value.focus)
    }

    @Test fun `count label prefers the paged size over a stray zero`() {
        assertEquals(412, libraryCountLabel(0, 412, true))
        assertEquals(0, libraryCountLabel(0, 0, true))
        assertEquals(7, libraryCountLabel(7, 0, false))
        assertNull(libraryCountLabel(null, 0, false))
    }

    @Test fun `album count is loaded and tab switch loads the other count`() = runTest {
        val vm = LibraryViewModel(FakeBackend(names))
        runCurrent()
        assertEquals(LibraryTab.Albums, vm.state.value.tab)
        assertEquals(9, vm.state.value.count)
        vm.onSelectTab(LibraryTab.Artists)
        runCurrent()
        assertEquals(LibraryTab.Artists, vm.state.value.tab)
        assertEquals(7, vm.state.value.count)
    }

    @Test fun `jump to letter finds first item at or after it with few lookups`() = runTest {
        val backend = FakeBackend(names)
        val vm = LibraryViewModel(backend)
        runCurrent()
        val target = async { vm.scrollTo.first() }
        vm.onJumpToLetter("D")
        runCurrent()
        assertEquals(4, target.await())
        assertTrue(backend.itemCalls.size <= 4)
    }

    @Test fun `jump to hash goes to the top and letter past the end clamps to last`() = runTest {
        val vm = LibraryViewModel(FakeBackend(names))
        runCurrent()
        val first = async { vm.scrollTo.first() }
        vm.onJumpToLetter("#")
        runCurrent()
        assertEquals(0, first.await())
        val last = async { vm.scrollTo.first() }
        vm.onJumpToLetter("Z")
        runCurrent()
        assertEquals(8, last.await())
    }

    @Test fun `firstIndexOf on empty list is zero`() = runTest {
        assertEquals(0, firstIndexOf('A', 0) { null })
    }

    @Test fun `row mapping builds uri, artist subtitle and thumbnail url`() {
        val row = album("Horyzont", "Marta Las", image = "covers/h.jpg").toRow("10.0.1.50:8095")
        assertEquals("library://album/Horyzont", row.uri)
        assertEquals("Marta Las", row.subtitle)
        assertTrue(row.coverUrl!!.contains("size=${ImageUrls.proxySize(THUMB_PX)}"))
        assertNull(album("X").toRow("10.0.1.50:8095").coverUrl)
        assertNull(album("X", image = "a").toRow("").coverUrl)
    }

    @Test fun `offline - no grid is loaded and no counts are shown until the server is ready`() = runTest {
        val backend = FakeBackend(names, initial = MaConnectionState.Disconnected)
        val vm = LibraryViewModel(backend)
        backgroundScope.launch { vm.items.collect {} }
        runCurrent()
        assertEquals(MaConnectionState.Disconnected, vm.state.value.connection)
        assertNull(vm.state.value.count)
        assertTrue(backend.pagerTabs.isEmpty())

        backend.connectionState.value = READY
        runCurrent()
        assertEquals(9, vm.state.value.count)
        assertEquals(7, vm.state.value.counts[LibraryTab.Artists])
        assertEquals(listOf(LibraryTab.Albums), backend.pagerTabs)
    }

    @Test fun `unauthenticated connection counts as not ready`() = runTest {
        val backend = FakeBackend(names, initial = MaConnectionState.Connected(ServerInfo(), authenticated = false))
        val vm = LibraryViewModel(backend)
        backgroundScope.launch { vm.items.collect {} }
        runCurrent()
        assertNull(vm.state.value.count)
        assertTrue(backend.pagerTabs.isEmpty())
    }

    @Test fun `empty tab reports count zero and the empty status`() = runTest {
        val vm = LibraryViewModel(FakeBackend(emptyList()))
        runCurrent()
        assertEquals(0, vm.state.value.count)
        assertEquals(
            LibraryStatus.Empty,
            libraryStatus(vm.state.value.connection, LoadState.NotLoading(endOfPaginationReached = true), itemCount = 0),
        )
    }

    @Test fun `status is chosen by connection first, then by list state`() {
        val ok = READY
        val reconnecting = MaConnectionState.Reconnecting(1, null)
        assertEquals(LibraryStatus.NoServer, libraryStatus(MaConnectionState.Disconnected, LoadState.Loading, 0))
        assertEquals(LibraryStatus.NoServer, libraryStatus(MaConnectionState.Connected(ServerInfo(), false), LoadState.Loading, 0))
        assertEquals(LibraryStatus.Connecting, libraryStatus(MaConnectionState.Connecting, LoadState.Loading, 0))
        assertEquals(LibraryStatus.Connecting, libraryStatus(reconnecting, LoadState.NotLoading(false), 0))
        assertEquals(LibraryStatus.Content, libraryStatus(reconnecting, LoadState.NotLoading(false), 40))
        assertEquals(LibraryStatus.Error, libraryStatus(MaConnectionState.Failed(23, "token"), LoadState.NotLoading(false), 40))
        assertEquals(LibraryStatus.Loading, libraryStatus(ok, LoadState.Loading, 0))
        assertEquals(LibraryStatus.Error, libraryStatus(ok, LoadState.Error(Exception()), 0))
        assertEquals(LibraryStatus.Content, libraryStatus(ok, LoadState.Loading, 40))
    }

    @Test fun `counted source reports placeholders around the loaded page`() = runTest {
        val source = CountedLibrarySource(total = 1000, pageSize = 50) { limit, offset ->
            List(limit.coerceAtMost(1000 - offset)) { album("a${offset + it}") }
        }
        val page = source.load(PagingSource.LoadParams.Refresh(key = 200, loadSize = 100, placeholdersEnabled = true))
            as PagingSource.LoadResult.Page
        assertEquals(200, page.itemsBefore)
        assertEquals(700, page.itemsAfter)
        assertEquals(300, page.nextKey)
        val tail = source.load(PagingSource.LoadParams.Append(key = 950, loadSize = 100, placeholdersEnabled = true))
            as PagingSource.LoadResult.Page
        assertNull(tail.nextKey)
        assertEquals(0, tail.itemsAfter)
    }
}
