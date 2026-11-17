package io.github.riberrr.spinroom

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

    @Test fun shortPlayPauseIsNotConsumed() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertFalse(d.dispatch(KeyPress(playPause, true, 0)))
        assertFalse(d.dispatch(KeyPress(playPause, false, 0)))
        assertTrue(h.calls.isEmpty())
    }

    @Test fun holdPlayPauseOpensDrawerOnceAndSwallowsRelease() {
        val h = Fake(); val d = RemoteKeyDispatcher(h)
        assertFalse(d.dispatch(KeyPress(playPause, true, 0)))
        assertTrue(d.dispatch(KeyPress(playPause, true, 1)))
        assertTrue(d.dispatch(KeyPress(playPause, true, 2)))
        assertTrue(d.dispatch(KeyPress(playPause, false, 0)))
        assertEquals(listOf("drawer"), h.calls)
        // the next short press passes through again
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

    @Test fun pinnedIsStationsThenPlaylistsCappedAtFive() {
        val s = listOf(fav("r1", "A"), fav("r2", "B"), fav("r3", "C"))
        val p = listOf(fav("p1", "X", false), fav("p2", "Y", false), fav("p3", "Z", false))
        assertEquals(listOf("r1", "r2", "r3", "p1", "p2"), pinnedOf(s, p).map { it.uri })
    }

    @Test fun stationIndexMatchesUriThenName() {
        val s = listOf(fav("library://radio/1", "Jazz Jeden"), fav("library://radio/2", "Klasyka 24"))
        assertEquals(1, stationIndex(s, "library://radio/2", ""))
        assertEquals(1, stationIndex(s, "tunein://radio/99", "klasyka 24"))
        assertEquals(-1, stationIndex(s, "tunein://radio/99", "Inne"))
        assertEquals(-1, stationIndex(s, "", ""))
    }
}
