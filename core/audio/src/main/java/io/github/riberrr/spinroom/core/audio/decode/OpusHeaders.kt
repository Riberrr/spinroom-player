package io.github.riberrr.spinroom.core.audio.decode

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Codec-specific data for Android's Opus decoder. Sendspin sends bare packets with no header, so we build
 * `OpusHead` (RFC 7845 §5.1) ourselves; csd-1/csd-2 are codec delay and seek pre-roll in ns (both 0: the
 * server timestamps already point at the first audible sample and a flush must not swallow audio).
 */
object OpusHeaders {
    fun opusHead(channels: Int, inputSampleRate: Int): ByteArray {
        require(channels in 1..2) { "only mono/stereo Opus (mapping family 0)" }
        return ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("OpusHead".toByteArray(Charsets.US_ASCII))
            put(1) // version
            put(channels.toByte())
            putShort(0) // pre-skip
            putInt(inputSampleRate)
            putShort(0) // output gain
            put(0) // channel mapping family
        }.array()
    }

    fun nanosLe(value: Long): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
}
