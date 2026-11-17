package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.data.KeyRules.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KeyAssignmentsTest {
    private val green = KeyAssignment(184, false, KeyAction.OPEN_SEARCH)

    @Test fun defaultsRoundTrip() {
        val d = KeyAssignments()
        assertEquals(d, KeyAssignments.decode(d.encode()))
    }

    @Test fun customListRoundTripKeepsArgAndScope() {
        val list = KeyAssignments(listOf(green, KeyAssignment(8, true, KeyAction.STATION, arg = 3, global = true)))
        assertEquals(list, KeyAssignments.decode(list.encode()))
    }

    @Test fun missingMeansDefaultsAndDashMeansEmpty() {
        assertEquals(KeyAssignments.DEFAULTS, KeyAssignments.decode(null).items)
        assertEquals(emptyList<KeyAssignment>(), KeyAssignments.decode(KeyAssignments.EMPTY).items)
    }

    @Test fun decodeDropsGarbageUnknownActionsBadStationAndBlockedKeys() {
        val raw = listOf("x", "184:S:NOPE:0:A", "9:S:STATION:12:A", "4:S:NEXT:0:A", "24:L:NEXT:0:A", "184:S:OPEN_SEARCH:0:A", "184:Q:NEXT:0:A").joinToString(";")
        assertEquals(listOf(green), KeyAssignments.decode(raw).items)
    }

    @Test fun sameGestureReplacesOldAssignment() {
        val base = KeyAssignments(listOf(green))
        val other = KeyAssignment(184, false, KeyAction.OPEN_RADIO)
        assertEquals(green, base.collidingWith(other))
        assertEquals(listOf(other), base.with(other).items)
    }

    @Test fun shortAndLongOnSameKeyDoNotCollide() {
        val base = KeyAssignments(listOf(green))
        val long = KeyAssignment(184, true, KeyAction.FAVORITE)
        assertNull(base.collidingWith(long))
        assertEquals(2, base.with(long).items.size)
    }

    @Test fun systemKeysAreBlocked() {
        listOf(3, 4, 24, 25, 164, 26).forEach {
            assertEquals("key $it", Verdict.Blocked, KeyRules.classify(it, false))
            assertEquals("key $it long", Verdict.Blocked, KeyRules.classify(it, true))
        }
        assertEquals(Verdict.Blocked, KeyRules.classify(19, true))
        assertEquals(Verdict.Blocked, KeyRules.classify(22, false))
    }

    @Test fun withIgnoresBlockedKeys() {
        val base = KeyAssignments(emptyList())
        assertEquals(emptyList<KeyAssignment>(), base.with(KeyAssignment(4, false, KeyAction.NEXT)).items)
    }

    @Test fun okOnlyAsHoldAndTvFunctionKeysWarn() {
        assertEquals(Verdict.Blocked, KeyRules.classify(23, false))
        assertEquals(Verdict.Ok, KeyRules.classify(23, true))
        assertEquals(Verdict.Warn, KeyRules.classify(82, false)) // Menu
        assertEquals(Verdict.Warn, KeyRules.classify(85, false)) // short Play/Pause
        assertEquals(Verdict.Ok, KeyRules.classify(85, true))
        assertEquals(Verdict.Ok, KeyRules.classify(184, false)) // green
    }

    @Test fun defaultsMatchTheSpec() {
        val d = KeyAssignments()
        assertEquals(KeyAction.NEXT, d.find(166, false)?.action)
        assertEquals(KeyAction.PREVIOUS, d.find(167, false)?.action)
        assertEquals(KeyAction.OPEN_DRAWER, d.find(85, true)?.action)
        assertEquals(KeyAction.FAVORITE, d.find(23, true)?.action)
        assertEquals(KeyAction.OPEN_RADIO, d.find(183, false)?.action)
        assertNotNull(d.find(166, false)?.takeIf { it.global })
    }
}

class KeyGestureDetectorTest {
    @Test fun tapReportsOnRelease() {
        val d = KeyGestureDetector()
        assertNull(d.onKey(184, true, 0))
        assertEquals(KeyGesture(184, false), d.onKey(184, false, 0))
    }

    @Test fun holdReportsOnceAtFirstRepeatAndNotOnRelease() {
        val d = KeyGestureDetector()
        assertNull(d.onKey(23, true, 0))
        assertEquals(KeyGesture(23, true), d.onKey(23, true, 1))
        assertNull(d.onKey(23, true, 2))
        assertNull(d.onKey(23, false, 0))
        // next tap works again
        d.onKey(23, true, 0)
        assertEquals(KeyGesture(23, false), d.onKey(23, false, 0))
    }
}
