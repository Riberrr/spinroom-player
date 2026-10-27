package io.github.riberrr.spinroom.core.audio.output

import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OutputClockEstimatorTest {
    private val f = PcmFormat(48_000, 2)

    @Test
    fun nullUntilHeadMoves() {
        val e = OutputClockEstimator(f)
        e.onHead(0, 1_000)
        assertNull(e.baseUs())
    }

    @Test
    fun headBaseTracksMinimumDespiteSteppedHead() {
        // True base 1 s; head advances in 10 ms (480-frame) steps, sampled every 3 ms.
        val e = OutputClockEstimator(f)
        for (t in 0 until 3000 step 3) {
            val nowUs = 1_000_000L + t * 1000L
            val head = ((t / 10) * 480).toLong()
            e.onHead(head, nowUs)
        }
        assertTrue(abs(e.baseUs()!! - 1_000_000L) < 1_000)
        assertFalse(e.usingTimestamp)
    }

    @Test
    fun trustsConsistentTimestamp() {
        val e = OutputClockEstimator(f)
        e.onHead(48_000, 2_000_000) // head base = 1 s
        e.onTimestamp(48_000, 2_040_000_000) // 40 ms hardware latency
        assertEquals(1_040_000L, e.baseUs())
        assertTrue(e.usingTimestamp)
    }

    @Test
    fun ignoresTimestampFromAnotherCounter() {
        // Emulator: timestamp frame counter starts ~7 s late.
        val e = OutputClockEstimator(f)
        e.onHead(340_000, 8_000_000)
        e.onTimestamp(1_241, 8_000_000_000)
        assertFalse(e.baseUs() == null)
        assertFalse(e.usingTimestamp)
    }
}
