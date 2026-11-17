package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsExtrasTest {
    private class Prefs : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    @Test fun nameValidation() {
        assertEquals(PlayerName.Result.Empty, PlayerName.validate(""))
        assertEquals(PlayerName.Result.Empty, PlayerName.validate("   \t "))
        assertEquals(PlayerName.Result.TooLong, PlayerName.validate("a".repeat(41)))
        assertEquals(PlayerName.Result.Ok("Salon"), PlayerName.validate("  Salon "))
        assertEquals(PlayerName.Result.Ok("a".repeat(40)), PlayerName.validate("a".repeat(40)))
    }

    @Test fun nameStoreRoundTrip() = runBlocking {
        val s = PlayerNameStore(Prefs())
        assertNull(s.name.first())
        s.set("Kuchnia")
        assertEquals("Kuchnia", s.name.first())
    }

    @Test fun homeLayoutDefaultsAndPersist() = runBlocking {
        val s = HomeLayoutSettings(Prefs())
        assertEquals(HomeRows.ALL, s.layout.first().visible)
        s.update { it.moved(HomeRows.RECENT, -2).toggled(HomeRows.PLAYLISTS) }
        val l = s.layout.first()
        assertEquals(listOf("recent", "stations", "playlists"), l.order)
        assertEquals(listOf("recent", "stations"), l.visible)
    }

    @Test fun homeLayoutTolerantDecodeAndBounds() {
        val l = HomeLayout.decode("bogus,playlists,playlists|bogus,recent")
        assertEquals(listOf("playlists", "stations", "recent"), l.order)
        assertEquals(setOf("recent"), l.hidden)
        assertEquals(l, l.moved("playlists", -1))
    }
}
