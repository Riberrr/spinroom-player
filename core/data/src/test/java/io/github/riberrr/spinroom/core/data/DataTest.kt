package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.datastore.preferences.core.emptyPreferences
import androidx.paging.PagingSource
import io.github.riberrr.spinroom.core.maapi.MediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTest {
    @Test fun settingsStorePersistsLoginAndClearsToken() = runBlocking {
        val s = SettingsStore(InMemoryPrefs())
        assertFalse(s.settings.first().isConfigured)
        s.saveLogin(" 10.0.1.50 ", "tok")
        val saved = s.settings.first()
        assertEquals("10.0.1.50", saved.serverAddress)
        assertTrue(saved.isConfigured)
        s.setPlayerId("tv1")
        s.clearToken()
        val after = s.settings.first()
        assertFalse(after.isConfigured)
        assertEquals("10.0.1.50", after.serverAddress)
        assertEquals("tv1", after.playerId)
    }

    @Test fun latencyStoreClampsAndDefaultsToZero() = runBlocking {
        val s = LatencyStore(InMemoryPrefs())
        assertEquals(0, s.calibrationMs.first())
        s.setCalibrationMs(-120)
        assertEquals(-120, s.calibrationMs.first())
        s.setCalibrationMs(900)
        assertEquals(500, s.calibrationMs.first())
    }

    private fun items(n: Int, from: Int) = List(n) { MediaItem(itemId = "${from + it}", name = "Album ${from + it}") }

    @Test fun pagingSourceStopsOnShortPage() = runBlocking {
        val calls = mutableListOf<Pair<Int, Int>>()
        val src = LibraryPagingSource(50) { limit, offset ->
            calls += limit to offset
            if (offset == 0) items(limit, 0) else items(7, offset)
        }
        val first = src.load(PagingSource.LoadParams.Refresh(null, 50, false)) as PagingSource.LoadResult.Page
        assertEquals(50, first.data.size)
        assertEquals(50, first.nextKey)
        val second = src.load(PagingSource.LoadParams.Append(first.nextKey!!, 50, false)) as PagingSource.LoadResult.Page
        assertEquals(7, second.data.size)
        assertNull(second.nextKey)
        assertEquals(listOf(50 to 0, 50 to 50), calls)
    }

    @Test fun pagingSourceReportsErrors() = runBlocking {
        val src = LibraryPagingSource(50) { _, _ -> error("boom") }
        assertTrue(src.load(PagingSource.LoadParams.Refresh(null, 50, false)) is PagingSource.LoadResult.Error)
    }
}

/** In-memory stand-in: the real file-backed DataStore fails its atomic rename on Windows JVMs. */
private class InMemoryPrefs : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}
