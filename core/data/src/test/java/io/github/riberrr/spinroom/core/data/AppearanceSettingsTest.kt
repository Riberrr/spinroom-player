package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    private class Prefs : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    @Test fun defaultsGiveCurrentLook() = runBlocking {
        val a = AppearanceSettings(Prefs()).appearance.first()
        assertEquals("grafit", a.theme)
        assertEquals("amber", a.accent)
        assertFalse(a.solidBackground)
        assertFalse(a.screensaver)
    }

    @Test fun savesAndIgnoresUnknownIds() = runBlocking {
        val s = AppearanceSettings(Prefs())
        s.setTheme("oled"); s.setAccent("mint"); s.setSolidBackground(true); s.setScreensaver(true)
        s.setTheme("neon"); s.setAccent("pink")
        val a = s.appearance.first()
        assertEquals("oled", a.theme)
        assertEquals("mint", a.accent)
        assertTrue(a.solidBackground)
        assertTrue(a.screensaver)
    }
}
