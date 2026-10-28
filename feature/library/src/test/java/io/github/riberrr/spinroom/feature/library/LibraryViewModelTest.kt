package io.github.riberrr.spinroom.feature.library

import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
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

private class FakeBackend(val names: List<String>) : LibraryBackend {
    val address = MutableStateFlow("10.0.1.50:8095")
    val itemCalls = mutableListOf<Int>()
    val pagerTabs = mutableListOf<LibraryTab>()
    override val serverAddress: Flow<String> = address
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
        assertTrue(row.coverUrl!!.contains("size=$THUMB_PX"))
        assertNull(album("X").toRow("10.0.1.50:8095").coverUrl)
        assertNull(album("X", image = "a").toRow("").coverUrl)
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
