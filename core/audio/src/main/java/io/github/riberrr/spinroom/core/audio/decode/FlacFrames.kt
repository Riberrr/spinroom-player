package io.github.riberrr.spinroom.core.audio.decode

/** One FLAC frame inside a chunk: byte range, sample count, frame number (first-sample number if [variableBlocking]). */
data class FlacFrame(
    val offset: Int,
    val length: Int,
    val blockSize: Int,
    val number: Long = 0,
    val variableBlocking: Boolean = false,
)

/**
 * Splits a Sendspin FLAC chunk (several whole frames back to back) into single frames.
 * Android's FLAC decoder decodes one frame per input buffer, so frames must be queued one by one.
 * A boundary is accepted only when the next header passes CRC-8 and the frame before it passes CRC-16.
 */
object FlacFrames {

    fun split(data: ByteArray): List<FlacFrame> {
        val frames = ArrayList<FlacFrame>(4)
        var start = findHeader(data, 0) ?: return frames
        while (true) {
            val h = parseHeader(data, start)!!
            var next = findHeader(data, start + MIN_FRAME_BYTES)
            while (next != null && !crc16Ok(data, start, next)) next = findHeader(data, next + 1)
            val end = next ?: data.size
            frames += FlacFrame(start, end - start, h.blockSize, h.number, h.variableBlocking)
            start = next ?: break
        }
        return frames
    }

    /** Header fields needed for splitting. */
    data class Header(val blockSize: Int, val headerLength: Int, val number: Long, val variableBlocking: Boolean)

    /** Parses and CRC-8-checks a frame header at [pos]; null if there is none. */
    fun parseHeader(d: ByteArray, pos: Int): Header? {
        if (pos + 6 > d.size) return null
        if (d[pos].toInt() and 0xFF != 0xFF || (d[pos + 1].toInt() and 0xFE) != 0xF8) return null
        val b2 = d[pos + 2].toInt() and 0xFF
        val b3 = d[pos + 3].toInt() and 0xFF
        val bsCode = b2 shr 4
        val srCode = b2 and 0x0F
        if (bsCode == 0 || srCode == 0x0F) return null
        if ((b3 shr 4) > 10 || (b3 shr 1 and 7) == 3 || (b3 and 1) != 0) return null
        var p = pos + 4
        // UTF-8-like coded frame/sample number.
        val first = d[p].toInt() and 0xFF
        val extra = when {
            first and 0x80 == 0 -> 0
            first and 0xE0 == 0xC0 -> 1
            first and 0xF0 == 0xE0 -> 2
            first and 0xF8 == 0xF0 -> 3
            first and 0xFC == 0xF8 -> 4
            first and 0xFE == 0xFC -> 5
            first == 0xFE -> 6
            else -> return null
        }
        p += 1 + extra
        if (p > d.size) return null
        var number = (first and (0x7F shr (if (extra == 0) 0 else extra + 1))).toLong()
        for (i in p - extra until p) {
            val b = d[i].toInt() and 0xFF
            if (b and 0xC0 != 0x80) return null
            number = (number shl 6) or (b and 0x3F).toLong()
        }
        val blockSize = when (bsCode) {
            1 -> 192
            in 2..5 -> 576 shl (bsCode - 2)
            6 -> { if (p + 1 > d.size) return null; (d[p].toInt() and 0xFF) + 1 }
            7 -> { if (p + 2 > d.size) return null; ((d[p].toInt() and 0xFF) shl 8 or (d[p + 1].toInt() and 0xFF)) + 1 }
            else -> 256 shl (bsCode - 8)
        }
        p += when (bsCode) { 6 -> 1; 7 -> 2; else -> 0 }
        p += when (srCode) { 12 -> 1; 13, 14 -> 2; else -> 0 }
        if (p + 1 > d.size) return null
        if (crc8(d, pos, p) != (d[p].toInt() and 0xFF)) return null
        return Header(blockSize, p + 1 - pos, number, d[pos + 1].toInt() and 1 == 1)
    }

    private fun findHeader(d: ByteArray, from: Int): Int? {
        var i = from
        while (i < d.size - 1) {
            if (d[i].toInt() and 0xFF == 0xFF && parseHeader(d, i) != null) return i
            i++
        }
        return null
    }

    private fun crc16Ok(d: ByteArray, start: Int, end: Int): Boolean {
        if (end - start < MIN_FRAME_BYTES) return false
        val stored = (d[end - 2].toInt() and 0xFF) shl 8 or (d[end - 1].toInt() and 0xFF)
        return crc16(d, start, end - 2) == stored
    }

    fun crc8(d: ByteArray, from: Int, to: Int): Int {
        var crc = 0
        for (i in from until to) {
            crc = crc xor (d[i].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 0x80 != 0) (crc shl 1) xor 0x07 else crc shl 1 }
            crc = crc and 0xFF
        }
        return crc
    }

    fun crc16(d: ByteArray, from: Int, to: Int): Int {
        var crc = 0
        for (i in from until to) {
            crc = crc xor ((d[i].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x8005 else crc shl 1 }
            crc = crc and 0xFFFF
        }
        return crc
    }

    /** Smallest possible frame: header (6) + one subframe byte + CRC-16. */
    private const val MIN_FRAME_BYTES = 9
}
