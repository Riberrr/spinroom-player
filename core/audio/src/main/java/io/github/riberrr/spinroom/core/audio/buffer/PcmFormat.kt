package io.github.riberrr.spinroom.core.audio.buffer

/** Decoded audio format. The engine always outputs signed 16-bit little-endian interleaved PCM. */
data class PcmFormat(val sampleRate: Int, val channels: Int) {
    val bytesPerFrame: Int get() = channels * 2

    fun framesToUs(frames: Long): Long = frames * 1_000_000L / sampleRate

    /** Rounds to the nearest frame; negative input gives negative frames. */
    fun usToFrames(us: Long): Long = Math.floorDiv(us * sampleRate + 500_000L, 1_000_000L)
}

/**
 * Decoded PCM16 block whose first frame must leave the speaker at server time [timestampUs].
 * [data] holds whole frames only.
 */
class PcmBlock(val timestampUs: Long, val data: ByteArray, val format: PcmFormat) {
    init {
        require(data.size % format.bytesPerFrame == 0) { "partial frame in PCM block" }
    }

    val frames: Int get() = data.size / format.bytesPerFrame
}
