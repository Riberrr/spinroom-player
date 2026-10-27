package io.github.riberrr.spinroom.core.audio.output

import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat

/**
 * Estimates the local time (µs) at which output frame 0 left / leaves the device ("base"); frame N then
 * plays at `base + framesToUs(N)`. Pure logic, fed by [AudioTrackOutput].
 *
 * Two sources:
 * - `playbackHeadPosition`: always there, but updated in steps and blind to latency after the mixer.
 *   `now − head` is never earlier than the truth, so we keep its minimum and let it creep up slowly
 *   (follows clock drift in both directions).
 * - `getTimestamp`: precise and includes hardware latency, but some devices (the Android TV emulator)
 *   report it late or with a frame counter that is not the track's. It is used only when it agrees with the
 *   head-based base within [MIN_LATENCY_US]..[MAX_LATENCY_US].
 */
class OutputClockEstimator(private val format: PcmFormat) {
    private var headBase: Long? = null
    private var tsBase: Long? = null

    /** True when the last estimate came from a trusted AudioTrack timestamp. */
    var usingTimestamp = false
        private set

    /** Head position read *before* [nowUs] was taken. head = 0 means not playing yet. */
    fun onHead(head: Long, nowUs: Long) {
        if (head <= 0) return
        val raw = nowUs - format.framesToUs(head)
        val b = headBase
        headBase = if (b == null || raw < b) raw else b + ((raw - b) / CREEP_DIVISOR)
    }

    fun onTimestamp(framePosition: Long, nanoTime: Long) {
        if (framePosition <= 0) return
        tsBase = nanoTime / 1000 - format.framesToUs(framePosition)
    }

    /** Local time of frame 0, or null until the head has moved. */
    fun baseUs(): Long? {
        val hb = headBase ?: return null
        val tb = tsBase
        usingTimestamp = tb != null && tb - hb in MIN_LATENCY_US..MAX_LATENCY_US
        return if (usingTimestamp) tb else hb
    }

    companion object {
        /** Head jitter allowance below the head-based base. */
        const val MIN_LATENCY_US = -20_000L
        /** Larger "latency" than this means the timestamp counts frames from somewhere else. */
        const val MAX_LATENCY_US = 500_000L
        /** Upward creep per sample: 1/256 of the gap. */
        private const val CREEP_DIVISOR = 256
    }
}
