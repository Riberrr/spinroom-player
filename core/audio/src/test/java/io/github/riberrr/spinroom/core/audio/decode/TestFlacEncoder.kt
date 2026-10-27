package io.github.riberrr.spinroom.core.audio.decode

import java.io.ByteArrayOutputStream

/**
 * Minimal FLAC encoder for tests and the debug asset: 16-bit, independent channels, FIXED order-2
 * predictor, one Rice partition. Not efficient, but valid per RFC 9639.
 */
class TestFlacEncoder(private val sampleRate: Int, private val channels: Int, private val blockSize: Int) {

    /** `fLaC` + STREAMINFO (last metadata block) — exactly what Sendspin puts in `codec_header`. */
    fun header(totalSamples: Long): ByteArray {
        val w = BitWriter()
        w.bytes("fLaC".toByteArray())
        w.bits(1, 1) // last block
        w.bits(0, 7) // STREAMINFO
        w.bits(34, 24)
        w.bits(blockSize.toLong(), 16)
        w.bits(blockSize.toLong(), 16)
        w.bits(0, 24) // min frame size unknown
        w.bits(0, 24) // max frame size unknown
        w.bits(sampleRate.toLong(), 20)
        w.bits((channels - 1).toLong(), 3)
        w.bits(15, 5) // 16 bits per sample
        w.bits(totalSamples, 36)
        repeat(16) { w.bits(0, 8) } // MD5 unknown
        return w.toByteArray()
    }

    /** Encodes one frame. [samples] is per channel, all of equal length ≤ blockSize. */
    fun frame(frameNumber: Int, samples: Array<IntArray>): ByteArray {
        val n = samples[0].size
        val w = BitWriter()
        w.bits(0xFFF8, 16) // sync + fixed blocksize
        val bsCode = if (n == blockSize) blockSizeCode(n) else 7
        w.bits(bsCode.toLong(), 4)
        w.bits(sampleRateCode().toLong(), 4)
        w.bits((channels - 1).toLong(), 4) // independent channels
        w.bits(4, 3) // 16 bit
        w.bits(0, 1)
        w.bytes(utf8(frameNumber.toLong()))
        if (bsCode == 7) w.bits((n - 1).toLong(), 16)
        w.bits(FlacFrames.crc8(w.toByteArray(), 0, w.byteLength()).toLong(), 8)
        for (ch in samples) subframe(w, ch)
        w.alignToByte()
        val body = w.toByteArray()
        val crc = FlacFrames.crc16(body, 0, body.size)
        return body + byteArrayOf((crc shr 8).toByte(), crc.toByte())
    }

    private fun subframe(w: BitWriter, x: IntArray) {
        if (x.size < 3) { // VERBATIM for tiny blocks
            w.bits(0, 1); w.bits(1, 6); w.bits(0, 1)
            x.forEach { w.bits(it.toLong() and 0xFFFF, 16) }
            return
        }
        w.bits(0, 1); w.bits(0b001010, 6); w.bits(0, 1) // FIXED order 2
        w.bits(x[0].toLong() and 0xFFFF, 16)
        w.bits(x[1].toLong() and 0xFFFF, 16)
        val res = IntArray(x.size - 2) { i -> x[i + 2] - 2 * x[i + 1] + x[i] }
        val u = LongArray(res.size) { i -> ((res[i] shl 1) xor (res[i] shr 31)).toLong() and 0xFFFFFFFFL }
        val k = (0..14).minBy { k -> u.sumOf { it shr k } + u.size.toLong() * (k + 1) }
        w.bits(0, 2) // Rice, 4-bit parameter
        w.bits(0, 4) // partition order 0
        w.bits(k.toLong(), 4)
        for (v in u) {
            repeat((v shr k).toInt()) { w.bits(0, 1) }
            w.bits(1, 1)
            if (k > 0) w.bits(v and ((1L shl k) - 1), k)
        }
    }

    private fun blockSizeCode(n: Int): Int = when (n) {
        192 -> 1
        576, 1152, 2304, 4608 -> 2 + Integer.numberOfTrailingZeros(n / 576)
        256, 512, 1024, 2048, 4096, 8192, 16384, 32768 -> 8 + Integer.numberOfTrailingZeros(n / 256)
        else -> 7
    }

    private fun sampleRateCode(): Int = when (sampleRate) {
        44_100 -> 9
        48_000 -> 10
        96_000 -> 11
        else -> 0 // from STREAMINFO
    }

    private fun utf8(v: Long): ByteArray = when {
        v < 0x80 -> byteArrayOf(v.toByte())
        v < 0x800 -> byteArrayOf((0xC0 or (v shr 6).toInt()).toByte(), (0x80 or (v and 0x3F).toInt()).toByte())
        v < 0x10000 -> byteArrayOf(
            (0xE0 or (v shr 12).toInt()).toByte(),
            (0x80 or ((v shr 6) and 0x3F).toInt()).toByte(),
            (0x80 or (v and 0x3F).toInt()).toByte(),
        )
        else -> error("frame number too large for test encoder")
    }

    private class BitWriter {
        private val out = ByteArrayOutputStream()
        private var acc = 0
        private var nBits = 0

        fun bits(value: Long, count: Int) {
            for (i in count - 1 downTo 0) {
                acc = (acc shl 1) or ((value shr i) and 1).toInt()
                if (++nBits == 8) {
                    out.write(acc)
                    acc = 0
                    nBits = 0
                }
            }
        }

        fun bytes(b: ByteArray) = b.forEach { bits(it.toLong() and 0xFF, 8) }

        fun alignToByte() {
            while (nBits != 0) bits(0, 1)
        }

        fun byteLength(): Int = out.size()

        fun toByteArray(): ByteArray = out.toByteArray()
    }
}
