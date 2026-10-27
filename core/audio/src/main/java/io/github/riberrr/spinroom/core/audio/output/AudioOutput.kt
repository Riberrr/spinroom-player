package io.github.riberrr.spinroom.core.audio.output

import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat

/** PCM16 sink with a way to tell when a written frame leaves the device. Driven by one thread. */
interface AudioOutput {
    val format: PcmFormat

    /** Device-side buffer size in µs (part of required lead time). */
    val bufferDurationUs: Long

    /**
     * Local monotonic µs (System.nanoTime base) at which frame number [framePosition], counted from the
     * first write, leaves the output. Null while the device has not reported a timestamp yet.
     */
    fun frameLocalTimeUs(framePosition: Long): Long?

    /** Blocking write of [frames] whole frames from [data]. */
    fun write(data: ByteArray, frames: Int)

    /** Linear amplitude 0..1. */
    fun setVolume(amplitude: Float)

    fun release()
}

fun interface AudioOutputFactory {
    fun create(format: PcmFormat): AudioOutput
}
