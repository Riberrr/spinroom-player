package io.github.riberrr.spinroom.core.audio.buffer

import io.github.riberrr.spinroom.core.sendspin.api.ClockSource
import kotlin.math.abs
import kotlin.math.roundToInt

/** Sync tuning (docs/SENDSPIN.md §6). */
data class SyncConfig(
    /** No correction while |error| is below this. */
    val deadbandUs: Long = 100,
    /** Above this the scheduler drops sync and realigns with silence / dropped frames. */
    val resyncThresholdUs: Long = 10_000,
    /** At most one frame drop/duplicate per this many frames (200 = 0.5 % speed). */
    val correctionIntervalFrames: Int = 200,
)

/** Counters since the scheduler was created or [PlaybackScheduler.resetStats] was called. */
data class SchedulerStats(
    val droppedFrames: Long = 0,
    val silenceFrames: Long = 0,
    val correctedFrames: Long = 0,
    val resyncs: Int = 0,
    val underruns: Int = 0,
    /** Last measured target − cursor in µs (positive: audio is early, we are ahead). */
    val lastErrorUs: Long = 0,
)

data class FillResult(
    /** Frames of real audio written (the rest is silence). */
    val audioFrames: Int,
    /** True when the next buffered block has a different [PcmFormat] than the output. */
    val formatChangePending: Boolean,
)

/**
 * Decides what goes into the output for a given output cursor. Pure logic: no Android, no threads.
 *
 * A frame with server time `ts` must reach the output at local time `clock.serverToLocalUs(ts) - outputDelayUs`.
 * The caller passes the local time at which the first frame it is about to write will leave the device
 * (`cursorLocalUs`, from AudioTrack timestamps). While unlocked (start, after a gap, after clear) the scheduler
 * aligns exactly: silence until the data is due, late frames dropped. Once locked it only nudges the error back
 * into the dead band by dropping or duplicating single frames, at most 0.5 % of the time.
 */
class PlaybackScheduler(
    private val buffer: TimedPcmBuffer,
    private val clock: ClockSource,
    private val config: SyncConfig = SyncConfig(),
) {
    /** Fixed device delay (HDMI/soundbar), µs. Audio is pushed out this much earlier. */
    var outputDelayUs: Long = 0

    var stats = SchedulerStats()
        private set

    private var locked = false
    private var framesSinceCorrection = 0

    val isLocked: Boolean get() = locked

    /** Forget alignment, e.g. after stream/clear or a new output. */
    fun unlock() {
        locked = false
        framesSinceCorrection = 0
    }

    fun resetStats() {
        stats = SchedulerStats()
    }

    /**
     * Fills [out] with exactly [frames] frames of [format] PCM16.
     * [cursorLocalUs] null = output timing unknown yet: only silence is written and nothing is consumed.
     */
    fun fill(out: ByteArray, frames: Int, format: PcmFormat, cursorLocalUs: Long?): FillResult {
        val bpf = format.bytesPerFrame
        require(out.size >= frames * bpf) { "output buffer too small" }
        var pos = 0
        var audio = 0
        var formatChange = false
        var s = stats

        fun silence(n: Int) {
            out.fill(0, pos * bpf, (pos + n) * bpf)
            pos += n
            s = s.copy(silenceFrames = s.silenceFrames + n)
        }

        while (pos < frames) {
            val remaining = frames - pos
            val headTs = buffer.headTimestampUs()
            if (cursorLocalUs == null || headTs == null) {
                if (headTs == null && locked) {
                    locked = false
                    s = s.copy(underruns = s.underruns + 1)
                }
                silence(remaining)
                break
            }
            if (buffer.headFormat() != format) {
                formatChange = true
                silence(remaining)
                break
            }

            val cursorUs = cursorLocalUs + format.framesToUs(pos.toLong())
            val targetUs = clock.serverToLocalUs(headTs) - outputDelayUs
            val err = targetUs - cursorUs
            s = s.copy(lastErrorUs = err)

            if (locked && abs(err) > config.resyncThresholdUs) {
                locked = false
                s = s.copy(resyncs = s.resyncs + 1)
            }

            if (!locked) {
                val errFrames = format.usToFrames(err)
                when {
                    errFrames > 0 -> {
                        val n = minOf(errFrames, remaining.toLong()).toInt()
                        silence(n)
                        if (n.toLong() == errFrames) lock()
                    }
                    errFrames < 0 -> {
                        val n = buffer.advanceHead(minOf(-errFrames, Int.MAX_VALUE.toLong()).toInt())
                        s = s.copy(droppedFrames = s.droppedFrames + n)
                    }
                    else -> lock()
                }
                continue
            }

            // Locked: soft correction, then copy a slice of the head block.
            if (abs(err) > config.deadbandUs && framesSinceCorrection >= config.correctionIntervalFrames) {
                val n = correctionFrames(format)
                framesSinceCorrection = 0
                if (err < 0) {
                    // Behind schedule: skip frames.
                    val dropped = buffer.advanceHead(n)
                    s = s.copy(correctedFrames = s.correctedFrames + dropped)
                    continue
                } else {
                    // Ahead of schedule: repeat the next frame.
                    val dup = minOf(n, remaining)
                    val head = buffer.headBlock()!!
                    val src = buffer.headByteOffset()
                    repeat(dup) { i -> System.arraycopy(head.data, src, out, (pos + i) * bpf, bpf) }
                    pos += dup
                    audio += dup
                    s = s.copy(correctedFrames = s.correctedFrames + dup)
                    continue
                }
            }

            val n = minOf(remaining, buffer.headRemainingFrames(), config.correctionIntervalFrames)
            val head = buffer.headBlock()!!
            System.arraycopy(head.data, buffer.headByteOffset(), out, pos * bpf, n * bpf)
            buffer.advanceHead(n)
            pos += n
            audio += n
            framesSinceCorrection += n
        }
        stats = s
        return FillResult(audioFrames = audio, formatChangePending = formatChange)
    }

    private fun lock() {
        locked = true
        framesSinceCorrection = 0
    }

    /** N = max(1, round(21 µs · fs)), per spec. */
    private fun correctionFrames(format: PcmFormat): Int = maxOf(1, (21e-6 * format.sampleRate).roundToInt())
}
