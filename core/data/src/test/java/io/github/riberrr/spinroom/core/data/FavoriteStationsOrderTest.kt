package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.RadioFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteStationsOrderTest {
    private class Prefs : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    @Test fun newStationGoesLast() =
        assertEquals(listOf("b", "a", "c"), StationOrder.merge(listOf("b", "a"), listOf("a", "b", "c")))

    @Test fun removedStationDisappearsFromSavedOrder() =
        assertEquals(listOf("c", "a"), StationOrder.merge(listOf("c", "b", "a"), listOf("a", "c")))

    @Test fun withoutSavedOrderKeepsServerOrder() =
        assertEquals(listOf("x", "y"), StationOrder.merge(emptyList(), listOf("x", "y")))

    @Test fun moveForwardAndBack() {
        val o = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), StationOrder.move(o, "a", 1))
        assertEquals(listOf("a", "c", "b"), StationOrder.move(o, "c", -1))
    }

    @Test fun moveIsClampedAndUnknownKeyIgnored() {
        val o = listOf("a", "b")
        assertEquals(o, StationOrder.move(o, "a", -1))
        assertEquals(o, StationOrder.move(o, "b", 1))
        assertEquals(o, StationOrder.move(o, "zzz", 1))
    }

    @Test fun removeAndAdd() {
        assertEquals(listOf("a", "c"), StationOrder.remove(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("a", "b"), StationOrder.add(listOf("a"), "b"))
        assertEquals(listOf("a"), StationOrder.add(listOf("a"), "a"))
    }

    @Test fun orderSurvivesRoundTrip() = runBlocking {
        val store = FavoriteStationsOrder(Prefs())
        store.save(listOf("library://radio/3", "library://radio/1"))
        assertEquals(listOf("library://radio/3", "library://radio/1"), store.current())
    }

    @Test fun emptyStoreGivesEmptyOrder() = runBlocking {
        assertEquals(emptyList<String>(), FavoriteStationsOrder(Prefs()).current())
    }

    private val a = Station("library://radio/1", "A", null, "uuid-a", "1")
    private val b = Station("library://radio/2", "B", null, "uuid-b", "2")

    @Test fun stepWrapsAroundBothWays() {
        val list = listOf(a, b)
        assertEquals(b, FavoriteStationsRepository.stepIn(list, a, true))
        assertEquals(a, FavoriteStationsRepository.stepIn(list, b, true))
        assertEquals(b, FavoriteStationsRepository.stepIn(list, a, false))
        assertEquals(a, FavoriteStationsRepository.stepIn(list, null, true))
        assertNull(FavoriteStationsRepository.stepIn(emptyList(), a, true))
    }

    @Test fun browseRowFindsItsFavoriteBySourceId() {
        val row = MediaItem(itemId = "uuid-b", provider = "radiobrowser", uri = "radiobrowser://radio/uuid-b", name = "B")
        assertEquals(b, FavoriteStationsRepository.favoriteIn(listOf(a, b), row))
        assertNull(FavoriteStationsRepository.favoriteIn(listOf(a), row))
    }

    @Test fun savedFilterDefaultsToPolishAndRoundTrips() {
        assertEquals(RadioFilter.Language("polish"), FavoriteStationsRepository.filterFrom(null, null))
        assertEquals(RadioFilter.Country("pl"), FavoriteStationsRepository.filterFrom("country", "pl"))
        assertEquals(RadioFilter.Popular, FavoriteStationsRepository.filterFrom("popular", ""))
        assertEquals(RadioFilter.Language("polish"), FavoriteStationsRepository.filterFrom("tag", ""))
        val f = RadioFilter.Tag("jazz")
        assertEquals(f, FavoriteStationsRepository.filterFrom(FavoriteStationsRepository.filterKind(f), FavoriteStationsRepository.filterKey(f)))
    }
}
