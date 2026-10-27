package io.github.riberrr.spinroom.core.audio.buffer

import io.github.riberrr.spinroom.core.sendspin.api.ClockSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Server clock = local clock + [offsetUs]; local time is driven by the test. */
class FakeClock(var offsetUs: Long = 0) : ClockSource {
    var nowUs = 0L
    override fun localNowUs() = nowUs
    override fun serverToLocalUs(serverUs: Long) = serverUs - offsetUs
    override fun localToServerUs(localUs: Long) = localUs + offsetUs
    override val converged: StateFlow<Boolean> = MutableStateFlow(true)
}

class PlaybackSchedulerTest {
    private val mono = PcmFormat(48_000, 1)
    private val clock = FakeClock(offsetUs = 5_000_000)
    private val buffer = TimedPcmBuffer()
    private val scheduler = PlaybackScheduler(buffer, clock)

    /** Mono block whose samples are firstValue, firstValue+1, ... so positions are visible in the output. */
    private fun block(serverUs: Long, frames: Int, firstValue: Int = 1, format: PcmFormat = mono): PcmBlock {
        val data = ByteArray(frames * format.bytesPerFrame)
        for (i in 0 until frames) for (c in 0 until format.channels) {
            val v = firstValue + i
            val o = (i * format.channels + c) * 2
            data[o] = v.toByte()
            data[o + 1] = (v shr 8).toByte()
        }
        return PcmBlock(serverUs, data, format)
    }

    private fun sample(out: ByteArray, frame: Int): Int =
        ((out[2 * frame + 1].toInt() shl 8) or (out[2 * frame].toInt() and 0xFF)).toShort().toInt()

    private val server = clock.offsetUs // server time of local 0

    @Test
    fun waitsWithSilenceUntilDataIsDue() {
        buffer.add(block(server + 5_000, 1000)) // due 5 ms = 240 frames after cursor
        val out = ByteArray(480 * 2)
        val r = scheduler.fill(out, 480, mono, cursorLocalUs = 0)
        assertEquals(0, sample(out, 239))
        assertEquals(1, sample(out, 240))
        assertEquals(240, sample(out, 479))
        assertEquals(240, r.audioFrames)
        assertTrue(scheduler.isLocked)
    }

    @Test
    fun dropsLateFramesAndStartsInTheMiddle() {
        buffer.add(block(server - 10_000, 1000)) // 10 ms late = 480 frames
        val out = ByteArray(100 * 2)
        scheduler.fill(out, 100, mono, cursorLocalUs = 0)
        assertEquals(481, sample(out, 0))
        assertEquals(480, scheduler.stats.droppedFrames)
    }

    @Test
    fun dropsWholeLateBlocks() {
        buffer.add(block(server - 100_000, 960))
        buffer.add(block(server - 80_000, 960, firstValue = 2000))
        buffer.add(block(server, 960, firstValue = 5000))
        val out = ByteArray(10 * 2)
        scheduler.fill(out, 10, mono, cursorLocalUs = 0)
        assertEquals(5000, sample(out, 0))
    }

    @Test
    fun unknownCursorWritesSilenceAndKeepsData() {
        buffer.add(block(server, 100))
        val out = ByteArray(50 * 2) { 9 }
        val r = scheduler.fill(out, 50, mono, cursorLocalUs = null)
        assertEquals(0, r.audioFrames)
        assertTrue(out.all { it == 0.toByte() })
        assertEquals(100L, buffer.bufferedFrames)
    }

    @Test
    fun outputDelayPlaysAudioEarlier() {
        scheduler.outputDelayUs = 5_000
        buffer.add(block(server + 5_000, 1000))
        val out = ByteArray(10 * 2)
        scheduler.fill(out, 10, mono, cursorLocalUs = 0)
        assertEquals(1, sample(out, 0))
    }

    @Test
    fun contiguousBlocksPlayGaplessAcrossFills() {
        buffer.add(block(server, 480, firstValue = 1))
        buffer.add(block(server + 10_000, 480, firstValue = 481))
        val out = ByteArray(960 * 2)
        scheduler.fill(out, 300, mono, cursorLocalUs = 0)
        val out2 = ByteArray(660 * 2)
        scheduler.fill(out2, 660, mono, cursorLocalUs = mono.framesToUs(300))
        assertEquals(300, sample(out, 299))
        for (i in 0 until 660) assertEquals(301 + i, sample(out2, i))
        assertEquals(0L, scheduler.stats.correctedFrames)
    }

    @Test
    fun holeInStreamIsFilledWithSilence() {
        buffer.add(block(server, 480))
        buffer.add(block(server + 60_000, 480, firstValue = 1000)) // 50 ms hole after the first block
        val out = ByteArray(4000 * 2)
        scheduler.fill(out, 4000, mono, cursorLocalUs = 0)
        assertEquals(480, sample(out, 479))
        assertEquals(0, sample(out, 480 + 2399))
        assertEquals(1000, sample(out, 2880))
        assertEquals(1, scheduler.stats.resyncs)
    }

    @Test
    fun smallDriftIsCorrectedSlowlyByDroppingFrames() {
        // Output runs 0.2 % fast against the server clock: it consumes 0.2 % more time per frame than
        // the audio covers, so the scheduler must drop about 1 frame per 500.
        buffer.add(block(server, 48_000 * 3))
        val out = ByteArray(480 * 2)
        var cursor = 0.0
        var maxAfterSettleUs = 0L
        repeat(250) { i ->
            scheduler.fill(out, 480, mono, cursor.toLong())
            cursor += 10_000 * 1.002
            if (i > 50) maxAfterSettleUs = maxOf(maxAfterSettleUs, abs(scheduler.stats.lastErrorUs))
        }
        assertEquals(0, scheduler.stats.resyncs)
        assertTrue("corrected ${scheduler.stats.correctedFrames}", scheduler.stats.correctedFrames in 200L..260L)
        assertTrue("error $maxAfterSettleUs", maxAfterSettleUs < 200)
    }

    @Test
    fun correctionNeverExceedsHalfAPercent() {
        // 5 ms early with a lock already held: dup at most 1 frame per 200.
        buffer.add(block(server, 48_000))
        val out = ByteArray(480 * 2)
        scheduler.fill(out, 480, mono, cursorLocalUs = 0) // lock at 0
        scheduler.fill(out, 480, mono, cursorLocalUs = 10_000 - 5_000) // cursor jumps 5 ms back
        assertTrue(scheduler.stats.correctedFrames <= 3)
        assertFalse(scheduler.stats.resyncs > 0)
    }

    @Test
    fun underrunUnlocksAndCounts() {
        buffer.add(block(server, 100))
        val out = ByteArray(200 * 2)
        scheduler.fill(out, 200, mono, cursorLocalUs = 0)
        assertEquals(1, scheduler.stats.underruns)
        assertFalse(scheduler.isLocked)
    }

    @Test
    fun formatChangeIsReported() {
        buffer.add(block(server, 100, format = PcmFormat(44_100, 2)))
        val r = scheduler.fill(ByteArray(100 * 2), 100, mono, cursorLocalUs = 0)
        assertTrue(r.formatChangePending)
        assertEquals(100L, buffer.bufferedFrames)
    }

    @Test
    fun stereoFramesStayIntact() {
        val st = PcmFormat(48_000, 2)
        buffer.add(block(server + 1_000, 100, format = st)) // 48 frames of silence first
        val out = ByteArray(100 * 4)
        scheduler.fill(out, 100, st, cursorLocalUs = 0)
        assertEquals(0, sample(out, 95)) // frame 47, right channel
        assertEquals(1, sample(out, 96)) // frame 48, left
        assertEquals(1, sample(out, 97)) // frame 48, right
    }
}
