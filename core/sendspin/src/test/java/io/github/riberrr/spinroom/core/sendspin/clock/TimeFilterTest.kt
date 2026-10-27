package io.github.riberrr.spinroom.core.sendspin.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class TimeFilterTest {

    @Test
    fun `first samples follow the spec bootstrap`() {
        val f = TimeFilter()
        assertFalse(f.update(100, 0, 10)) // max_error <= 0
        assertTrue(f.update(1_000, 500, 1_000_000))
        assertEquals(1_000.0, f.offset, 0.0)
        assertEquals(1, f.count)
        assertFalse(f.update(1_000, 500, 1_000_000)) // not newer
        assertTrue(f.update(2_000, 500, 2_000_000))
        assertEquals(2_000.0, f.offset, 0.0)
        assertEquals(1_000.0 / 1_000_000, f.drift, 1e-12)
        assertEquals(2, f.count)
    }

    @Test
    fun `exchange arithmetic uses integer division`() {
        val f = TimeFilter()
        // measurement = (4000900 + 3999949) / 2 = 4000424 (truncated), max_error = (1001 - 50) / 2 = 475
        f.addExchange(1_000_000, 5_000_900, 5_000_950, 1_001_001)
        assertEquals(4_000_424.0, f.offset, 0.0)
    }

    @Test
    fun `conversions are inverse`() {
        val f = TimeFilter()
        f.update(5_000_000, 300, 1_000_000)
        f.update(5_000_100, 300, 2_000_000)
        repeat(20) { f.update(5_000_100 + (it + 1) * 100L, 300, 2_000_000 + (it + 1) * 1_000_000L) }
        for (t in listOf(0L, 12_345_678L, 30_000_000L)) {
            assertTrue(abs(f.clientTime(f.serverTime(t)) - t) <= 1)
        }
    }

    /** Simulated LAN/WiFi: offset, 40 ppm drift, asymmetric random delays, bursts of 8, best-of-burst. */
    @Test
    fun `tracks offset and drift within 1 ms under jitter`() {
        val rnd = Random(42)
        val offset = 8_765_432_100L
        val driftPpm = 40.0
        fun server(local: Long) = local + offset + (local * driftPpm / 1e6).toLong()

        val clock = SendspinClock(localNow = { 0 })
        var local = 1_000_000L
        val errors = ArrayList<Long>()
        repeat(240) { burstIndex ->
            val burst = TimeBurst(8)
            repeat(8) {
                val up = 500 + rnd.nextLong(0, 15_000) // 0.5..15.5 ms
                val down = 500 + rnd.nextLong(0, 15_000)
                val t1 = local
                val t2 = server(t1 + up)
                val t3 = t2 + 40
                val t4 = t1 + up + 40 + down
                burst.onSent()
                burst.onReply(t1, t2, t3, t4)
                local = t4 + 1_000
            }
            val b = burst.best()!!
            clock.addExchange(b[0], b[1], b[2], b[3])
            if (burstIndex > 20) {
                val probe = local + 2_000_000 // predict 2 s ahead
                errors += abs(clock.localToServerUs(probe) - server(probe))
                errors += abs(clock.serverToLocalUs(server(probe)) - probe)
            }
            local += 5_000_000 // next burst in 5 s
        }
        assertTrue(clock.converged.value)
        val worst = errors.max()
        assertTrue("worst error $worst µs", worst < 1_000)
    }

    @Test
    fun `burst keeps the smallest max_error`() {
        val b = TimeBurst(3)
        b.onSent(); b.onReply(0, 100, 110, 1_000) // max_error = (1000-10)/2 = 495
        b.onSent(); b.onReply(2_000, 2_100, 2_110, 2_300) // 145
        b.onSent(); b.onReply(5_000, 5_100, 5_110, 5_900) // 395
        assertTrue(b.complete)
        assertEquals(2_000L, b.best()!![0])
    }

    @Test
    fun `convergence needs samples and low error`() {
        val c = SendspinClock(localNow = { 0 })
        assertFalse(c.converged.value)
        c.addExchange(0, 1_000_000, 1_000_000, 1_000)
        c.addExchange(1_000_000, 2_000_000, 2_000_000, 1_001_000)
        assertFalse(c.converged.value)
        c.addExchange(2_000_000, 3_000_000, 3_000_000, 2_001_000)
        assertTrue(c.converged.value)
        c.reset()
        assertFalse(c.converged.value)
        assertEquals(123L, c.serverToLocalUs(123)) // identity before samples
    }
}
