package io.github.riberrr.spinroom.feature.settings.latency

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Calibration signal as a WAV file (48 kHz, mono, PCM16), generated on the fly by byte offset:
 * a short beep every second, a higher one every 4th second so both speakers can be compared beat by beat.
 * Pure, so the HTTP server can serve any byte range without holding the file in memory.
 */
object TestSignal {
    const val SAMPLE_RATE = 48_000
    const val DURATION_S = 180
    const val BEEP_MS = 30
    const val LOW_HZ = 880.0
    const val HIGH_HZ = 1760.0
    private const val FADE_SAMPLES = SAMPLE_RATE * 3 / 1000
    private const val AMPLITUDE = 0.6
    const val HEADER_BYTES = 44

    val dataBytes: Long = SAMPLE_RATE.toLong() * DURATION_S * 2
    val totalBytes: Long = HEADER_BYTES + dataBytes
    private val HEADER = header()

    fun header(): ByteArray {
        val b = java.nio.ByteBuffer.allocate(HEADER_BYTES).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt((totalBytes - 8).toInt()).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1) // PCM, mono
        b.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(dataBytes.toInt())
        return b.array()
    }

    /** Sample [i] (0-based from the start of the data chunk). */
    fun sample(i: Long): Short {
        val second = i / SAMPLE_RATE
        val inBeep = (i % SAMPLE_RATE).toInt()
        val beepSamples = SAMPLE_RATE * BEEP_MS / 1000
        if (inBeep >= beepSamples) return 0
        val hz = if (second % 4 == 0L) HIGH_HZ else LOW_HZ
        val fade = minOf(1.0, inBeep / FADE_SAMPLES.toDouble(), (beepSamples - inBeep) / FADE_SAMPLES.toDouble())
        val v = AMPLITUDE * fade * sin(2 * PI * hz * inBeep / SAMPLE_RATE)
        return (v * Short.MAX_VALUE).roundToInt().toShort()
    }

    /** Fills [out] with the file's bytes starting at [offset]; returns how many were written (0 at the end). */
    fun read(offset: Long, out: ByteArray): Int {
        if (offset >= totalBytes) return 0
        val n = minOf(out.size.toLong(), totalBytes - offset).toInt()
        val header = HEADER
        for (k in 0 until n) {
            val pos = offset + k
            out[k] = if (pos < HEADER_BYTES) {
                header[pos.toInt()]
            } else {
                val d = pos - HEADER_BYTES
                val s = sample(d / 2).toInt()
                (if (d % 2 == 0L) s else s shr 8).toByte()
            }
        }
        return n
    }
}
