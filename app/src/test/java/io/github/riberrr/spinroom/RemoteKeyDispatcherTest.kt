package io.github.riberrr.spinroom

import io.github.riberrr.spinroom.core.data.KeyAction
import io.github.riberrr.spinroom.core.data.KeyAssignment
import io.github.riberrr.spinroom.core.data.KeyAssignments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyDispatcherTest {
    private class Fake(override var radioPlaying: Boolean = false) : RemoteHandler {
        val calls = mutableListOf<String>()
        override fun openDrawer() { calls += "drawer" }
        override fun nextTrack() { calls += "nextTrack" }
        override fun previousTrack() { calls += "prevTrack" }
        override fun nextStation() { calls += "nextStation" }
        override fun previousStation() { calls += "prevStation" }
        override fun playPause() { calls += "playPause" }
        override fun toggleFavorite() { calls += "favorite" }
        override fun openRoute(route: String) { calls += "open:$route" }
        override fun playFavoriteStation(n: Int) { calls += "station$n" }
        var assignable = false
        override fun canAssignStationNumber() = assignable
        override fun assignStationNumber(n: Int): Boolean { if (assignable) calls += "assign$n"; return assignable }
    }

    private val playPause = DefaultKeyBindings.KEYCODE_MEDIA_PLAY_PAUSE
    private val chUp = DefaultKeyBindings.KEYCODE_CHANNEL_UP
    private val chDown = DefaultKeyBindings.KEYCODE_CHANNEL_DOWN

    @Test fun channelKeysSkipTracksForMusic() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertTrue(d.dispatch(KeyPress(chUp, true, 0)))
        assertTrue(d.dispatch(KeyPress(chDown, true, 0)))
        assertEquals(listOf("nextTrack", "prevTrack"), h.calls)
    }

    @Test fun channelKeysSwitchStationsForRadio() {
        val h = Fake(radioPlaying = true); val d = RemoteKeyDispatcher(h)
        d.dispatch(KeyPress(chUp, true, 0))
        d.dispatch(KeyPress(chDown, true, 0))
        assertEquals(listOf("nextStation", "prevStation"), h.calls)
    }

    @Test fun channelKeyHeldDoesNotRepeatTheAction() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        d.dispatch(KeyPress(chUp, true, 0))
        assertTrue(d.dispatch(KeyPress(chUp, true, 1)))
        assertTrue(d.dispatch(KeyPress(chUp, true, 2)))
        assertEquals(listOf("nextTrack"), h.calls)
    }

    @Test fun shortPlayPauseTogglesOnReleaseAndIsConsumed() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertTrue(d.dispatch(KeyPress(playPause, true, 0)))
        assertTrue(h.calls.isEmpty()) // nothing yet: could still turn into a hold
        assertTrue(d.dispatch(KeyPress(playPause, false, 0)))
        assertEquals(listOf("playPause"), h.calls)
    }

    @Test fun holdPlayPauseOpensDrawerWithoutTouchingPlayback() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertTrue(d.dispatch(KeyPress(playPause, true, 0))) // must not reach the system media session
        assertTrue(d.dispatch(KeyPress(playPause, true, 1)))
        assertTrue(d.dispatch(KeyPress(playPause, true, 2)))
        assertTrue(d.dispatch(KeyPress(playPause, false, 0)))
        assertEquals(listOf("drawer"), h.calls)
        // the next short press works again
        assertTrue(d.dispatch(KeyPress(playPause, true, 0)))
        assertTrue(d.dispatch(KeyPress(playPause, false, 0)))
        assertEquals(listOf("drawer", "playPause"), h.calls)
    }

    @Test fun playPauseWithoutAnyShortcutPassesToSystem() {
        val d = RemoteKeyDispatcher(Fake(), AssignedKeyBindings(KeyAssignments(emptyList())))
        assertFalse(d.dispatch(KeyPress(playPause, true, 0)))
        assertFalse(d.dispatch(KeyPress(playPause, false, 0)))
    }

    @Test fun unmappedKeysPass() {
        val d = RemoteKeyDispatcher(Fake())
        assertFalse(d.dispatch(KeyPress(19, true, 0)))
        assertFalse(d.dispatch(KeyPress(19, true, 1)))
    }

    @Test fun bindingsAreReplaceable() {
        val h = Fake()
        val d = RemoteKeyDispatcher(h, object : KeyBindings {
            override fun onHold(keyCode: Int) = null
            override fun onPress(keyCode: Int) = if (keyCode == 100) RemoteAction.NEXT else null
        })
        assertFalse(d.dispatch(KeyPress(chUp, true, 0)))
        assertTrue(d.dispatch(KeyPress(100, true, 0)))
        assertEquals(listOf("nextTrack"), h.calls)
    }

    @Test fun assignedBindingsDriveTheDispatcher() {
        val h = Fake()
        val list = KeyAssignments(listOf(
            KeyAssignment(184, false, KeyAction.OPEN_SEARCH),
            KeyAssignment(8, false, KeyAction.STATION, arg = 3),
        ))
        val d = RemoteKeyDispatcher(h, AssignedKeyBindings(list))
        assertTrue(d.dispatch(KeyPress(184, true, 0)))
        assertTrue(d.dispatch(KeyPress(8, true, 0)))
        assertFalse(d.dispatch(KeyPress(chUp, true, 0))) // defaults are gone once the user removed them
        assertEquals(listOf("open:search", "station3"), h.calls)
    }

    @Test fun keyWithShortAndHoldRunsShortOnReleaseOnly() {
        val h = Fake()
        val list = KeyAssignments(listOf(
            KeyAssignment(184, false, KeyAction.PLAY_PAUSE),
            KeyAssignment(184, true, KeyAction.FAVORITE),
        ))
        val d = RemoteKeyDispatcher(h, AssignedKeyBindings(list))
        assertTrue(d.dispatch(KeyPress(184, true, 0)))
        assertTrue(d.dispatch(KeyPress(184, false, 0)))
        assertTrue(d.dispatch(KeyPress(184, true, 0)))
        assertTrue(d.dispatch(KeyPress(184, true, 1)))
        assertTrue(d.dispatch(KeyPress(184, false, 0)))
        assertEquals(listOf("playPause", "favorite"), h.calls)
    }

    @Test fun holdOkIsBoundByDefaultToFavorite() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertFalse(d.dispatch(KeyPress(23, true, 0))) // short OK still reaches the UI
        assertTrue(d.dispatch(KeyPress(23, true, 1)))
        assertTrue(d.dispatch(KeyPress(23, false, 0)))
        assertEquals(listOf("favorite"), h.calls)
    }

    @Test fun digitHeldOnFocusedFavoriteAssignsNumberInsteadOfDialling() {
        val h = Fake().apply { assignable = true }; val d = RemoteKeyDispatcher(h, AssignedKeyBindings(KeyAssignments(listOf(KeyAssignment(10, false, KeyAction.STATION, arg = 3)))))
        val key3 = 10 // KEYCODE_3
        assertTrue(d.dispatch(KeyPress(key3, true, 0)))
        assertTrue(d.dispatch(KeyPress(key3, true, 1)))
        assertTrue(d.dispatch(KeyPress(key3, false, 0)))
        assertEquals(listOf("assign3"), h.calls)
    }

    @Test fun digitShortPressStillDialsOnReleaseWhenAssignable() {
        val h = Fake().apply { assignable = true }; val d = RemoteKeyDispatcher(h, AssignedKeyBindings(KeyAssignments(listOf(KeyAssignment(10, false, KeyAction.STATION, arg = 3)))))
        assertTrue(d.dispatch(KeyPress(10, true, 0)))
        assertTrue(d.dispatch(KeyPress(10, false, 0)))
        assertEquals(listOf("station3"), h.calls)
    }

    @Test fun redKeyOpensRadioByDefault() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertTrue(d.dispatch(KeyPress(183, true, 0)))
        assertEquals(listOf("open:radio"), h.calls)
    }

    @Test fun cycleWrapsAndHandlesNotFound() {
        assertEquals(1, cycleIndex(0, 3, true))
        assertEquals(0, cycleIndex(2, 3, true))
        assertEquals(2, cycleIndex(0, 3, false))
        assertEquals(0, cycleIndex(-1, 3, true))
        assertEquals(2, cycleIndex(-1, 3, false))
        assertEquals(-1, cycleIndex(0, 0, true))
    }
}

class ShellLogicTest {
    private fun fav(uri: String, name: String, station: Boolean = true) = Favorite(uri, name, null, station)

    @Test fun barShowsSongOverStationForRadioAndTitleOverArtistOtherwise() {
        assertEquals("Artysta – Piosenka" to "Jazz Jeden", barLines(true, "Jazz Jeden", "", "Artysta – Piosenka"))
        assertEquals("Jazz Jeden" to "", barLines(true, "Jazz Jeden", "", ""))
        assertEquals("Utwór" to "Wykonawca", barLines(false, "Utwór", "Wykonawca", ""))
    }

    @Test fun drawerKeyIsTheAssignedKeyOrNull() {
        assertEquals(85, drawerKeyCode(KeyAssignments()))
        val moved = KeyAssignments().without(KeyAssignments.DEFAULTS[2]).with(KeyAssignment(165, false, KeyAction.OPEN_DRAWER))
        assertEquals(165, drawerKeyCode(moved))
        assertEquals(null, drawerKeyCode(KeyAssignments(emptyList())))
    }

    @Test fun stationIndexMatchesUriThenName() {
        val s = listOf(fav("library://radio/1", "Jazz Jeden"), fav("library://radio/2", "Klasyka 24"))
        assertEquals(1, stationIndex(s, "library://radio/2", ""))
        assertEquals(1, stationIndex(s, "tunein://radio/99", "klasyka 24"))
        assertEquals(-1, stationIndex(s, "tunein://radio/99", "Inne"))
        assertEquals(-1, stationIndex(s, "", ""))
    }
}
