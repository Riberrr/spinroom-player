package io.github.riberrr.spinroom.core.data

import androidx.paging.PagingSource
import io.github.riberrr.spinroom.core.maapi.MediaItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun item(name: String, year: Int?) = MediaItem(itemId = name, name = name, year = year)

class LibraryFilterTest {
    @Test fun emptyFilterAcceptsEverything() {
        val f = LibraryFilter()
        assertTrue(f.isEmpty)
        assertEquals(0, f.activeCount)
        assertTrue(f.matches(item("x", null)))
    }

    @Test fun decadeMatchesYearRangeOnly() {
        val f = LibraryFilter(decade = 1990)
        assertTrue(f.matches(item("a", 1990)))
        assertTrue(f.matches(item("b", 1999)))
        assertFalse(f.matches(item("c", 2000)))
        assertFalse(f.matches(item("d", 1989)))
        assertFalse(f.matches(item("e", null)))
        assertFalse(f.serverSideOnly)
    }

    @Test fun activeCountCountsCriteria() {
        val f = LibraryFilter(genreId = "3", decade = 2010, provider = "spotify", favoriteOnly = true)
        assertEquals(4, f.activeCount)
        assertFalse(f.isEmpty)
        assertTrue(LibraryFilter(genreId = "3").serverSideOnly)
    }

    @Test fun playlistSortOrderBy() {
        assertEquals("last_played_desc", PlaylistSort.Recent.orderBy)
        assertEquals("name", PlaylistSort.Alphabetical.orderBy)
    }

    @Test fun filteringSourceGathersAcrossServerPages() = runTest {
        val all = (0 until 25).map { item("i$it", if (it % 5 == 0) 1995 else 2005) } // 5 matches of 25
        val src = FilteringPagingSource(10, LibraryFilter(decade = 1990)::matches) { limit, offset -> all.drop(offset).take(limit) }
        val page = src.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
        assertEquals(5, page.data.size)
        assertNull(page.nextKey) // server exhausted while looking for 20
    }

    @Test fun filteringSourceStopsWhenPageIsFullAndKeepsOffset() = runTest {
        val all = (0 until 100).map { item("i$it", 1995) }
        val src = FilteringPagingSource(10, { true }) { limit, offset -> all.drop(offset).take(limit) }
        val page = src.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
        assertEquals(20, page.data.size)
        assertEquals(20, page.nextKey)
    }
}
