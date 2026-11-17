package io.github.riberrr.spinroom

import io.github.riberrr.spinroom.core.data.KeyAction
import io.github.riberrr.spinroom.core.data.KeyAssignment
import io.github.riberrr.spinroom.core.data.KeyAssignments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalKeyPolicyTest {
    private val up = KeyAssignments.KEYCODE_CHANNEL_UP
    private val down = KeyAssignments.KEYCODE_CHANNEL_DOWN
    private val defaults = KeyAssignments()

    private fun decide(
        key: Int = up,
        a: KeyAssignments = defaults,
        enabled: Boolean = true,
        playing: Boolean = true,
        foreground: Boolean = false,
    ) = GlobalKeyPolicy.interceptedAction(key, a, enabled, playing, foreground)

    @Test fun grabsChannelKeysWhenEverythingAllows() {
        assertEquals(RemoteAction.NEXT, decide(up))
        assertEquals(RemoteAction.PREVIOUS, decide(down))
    }

    @Test fun passesWhenSwitchIsOff() = assertNull(decide(enabled = false))
    @Test fun passesWhenNothingPlays() = assertNull(decide(playing = false))
    @Test fun passesWhenAppIsInFront() = assertNull(decide(foreground = true))

    @Test fun passesUnassignedKeys() {
        assertNull(decide(key = 29)) // A
        assertNull(decide(key = 4)) // Back is never touched
        assertNull(decide(key = 3)) // Home
    }

    @Test fun passesAppOnlyShortcuts() {
        val a = KeyAssignments(listOf(KeyAssignment(up, false, KeyAction.NEXT, global = false)))
        assertNull(decide(a = a))
    }

    @Test fun passesKeyThatAlsoHasHoldShortcut() {
        val a = defaults.with(KeyAssignment(up, true, KeyAction.FAVORITE))
        assertNull(decide(a = a))
    }

    @Test fun passesActionsThatNeedTheApp() {
        val a = KeyAssignments(listOf(KeyAssignment(183, false, KeyAction.OPEN_RADIO, global = true)))
        assertNull(decide(key = 183, a = a))
    }

    @Test fun grabsStationAndPlayPauseShortcuts() {
        val a = KeyAssignments(
            listOf(
                KeyAssignment(8, false, KeyAction.STATION, 1, global = true),
                KeyAssignment(183, false, KeyAction.PLAY_PAUSE, global = true),
            ),
        )
        assertEquals(RemoteAction.STATION_1, decide(key = 8, a = a))
        assertEquals(RemoteAction.PLAY_PAUSE, decide(key = 183, a = a))
    }

    @Test fun filterSwallowsRepeatsAndUpOfGrabbedKey() {
        val f = GlobalKeyFilter { if (it == up) RemoteAction.NEXT else null }
        assertEquals(GlobalKeyFilter.Result(true, RemoteAction.NEXT), f.onKey(up, true, 0))
        assertEquals(GlobalKeyFilter.Result(true), f.onKey(up, true, 1))
        assertEquals(GlobalKeyFilter.Result(true), f.onKey(up, false, 0))
        assertFalse(f.onKey(up, false, 0).consume) // nothing left to swallow
    }

    @Test fun filterLeavesOtherKeysAlone() {
        val f = GlobalKeyFilter { if (it == up) RemoteAction.NEXT else null }
        assertFalse(f.onKey(29, true, 0).consume)
        assertFalse(f.onKey(29, true, 2).consume)
        assertFalse(f.onKey(29, false, 0).consume)
        assertTrue(f.onKey(up, true, 0).consume)
    }
}
