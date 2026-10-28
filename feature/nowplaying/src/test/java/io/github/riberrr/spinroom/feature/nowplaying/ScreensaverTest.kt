package io.github.riberrr.spinroom.feature.nowplaying

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreensaverTest {
    @Test fun startsAtCentre() {
        val (x, y) = driftOffset(0, 160f, 60f)
        assertEquals(0f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test fun staysWithinRangeAndActuallyMoves() {
        var maxX = 0f
        for (t in 0L..600_000L step 1_000L) {
            val (x, y) = driftOffset(t, 160f, 60f)
            assertTrue(abs(x) <= 160.01f && abs(y) <= 60.01f)
            maxX = maxOf(maxX, abs(x))
        }
        assertTrue(maxX > 150f)
    }

    @Test fun movesSlowly() {
        val (x0, _) = driftOffset(10_000, 160f, 60f)
        val (x1, _) = driftOffset(11_000, 160f, 60f)
        assertTrue(abs(x1 - x0) < 10f) // under ~10 px per second
    }
}
