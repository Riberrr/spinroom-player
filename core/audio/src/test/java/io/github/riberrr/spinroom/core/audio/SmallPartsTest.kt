package io.github.riberrr.spinroom.core.audio

import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import io.github.riberrr.spinroom.core.audio.buffer.TimedPcmBuffer
import io.github.riberrr.spinroom.core.audio.buffer.PcmBlock
import io.github.riberrr.spinroom.core.audio.decode.OpusHeaders
import io.github.riberrr.spinroom.core.audio.decode.PcmConvert
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SmallPartsTest {
    @Test
    fun volumeCurve() {
        assertEquals(1f, Volume.amplitude(100, false), 1e-6f)
        assertEquals(0.353553f, Volume.amplitude(50, false), 1e-5f)
        assertEquals(0f, Volume.amplitude(0, false), 0f)
        assertEquals(0f, Volume.amplitude(100, true), 0f)
        assertEquals(1f, Volume.amplitude(150, false), 1e-6f)
    }

    @Test
    fun pcm24To16KeepsHighBytes() {
        val in24 = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66)
        assertArrayEquals(byteArrayOf(0x22, 0x33, 0x55, 0x66), PcmConvert.toPcm16(in24, 24))
    }

    @Test
    fun floatToPcm16() {
        val out = PcmConvert.floatToPcm16(floatArrayOf(1f, -1f, 0f, 2f), 4)
        assertArrayEquals(byteArrayOf(-1, 0x7F, 1, -128, 0, 0, -1, 0x7F), out)
    }

    @Test
    fun opusHeadLayout() {
        val h = OpusHeaders.opusHead(2, 48_000)
        assertEquals("OpusHead", String(h, 0, 8, Charsets.US_ASCII))
        assertEquals(19, h.size)
        assertEquals(1, h[8].toInt())
        assertEquals(2, h[9].toInt())
        assertArrayEquals(byteArrayOf(-128, -69, 0, 0), h.copyOfRange(12, 16)) // 48000 LE
        assertEquals(0, h[18].toInt())
    }

    @Test
    fun capabilitiesOrderAndFallback() {
        val full = AudioCapabilities.build(hasFlac = true, hasOpus = true).supportedFormats.map { it.codec }
        assertEquals(listOf(Codec.FLAC, Codec.FLAC, Codec.OPUS, Codec.PCM, Codec.PCM), full)
        val api26 = AudioCapabilities.build(hasFlac = false, hasOpus = true).supportedFormats.map { it.codec }
        assertEquals(listOf(Codec.OPUS, Codec.PCM, Codec.PCM), api26)
    }

    @Test
    fun bufferTracksHeadTimeAndDropsOldBlocks() {
        val f = PcmFormat(48_000, 1)
        val b = TimedPcmBuffer()
        b.add(PcmBlock(1_000_000, ByteArray(960), f)) // 480 frames = 10 ms
        b.add(PcmBlock(1_010_000, ByteArray(960), f))
        b.advanceHead(240)
        assertEquals(1_005_000L, b.headTimestampUs())
        assertEquals(720L, b.bufferedFrames)
        assertEquals(240L, b.dropBlocksEndingBefore(1_010_000))
        assertEquals(1_010_000L, b.headTimestampUs())
        assertEquals(1_020_000L, b.endTimestampUs())
    }
}
