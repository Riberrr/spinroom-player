package io.github.riberrr.spinroom.core.audio.decode

/** Converts Sendspin PCM (little-endian signed, interleaved) to PCM16 little-endian. */
object PcmConvert {
    fun toPcm16(data: ByteArray, bitDepth: Int): ByteArray = when (bitDepth) {
        16 -> data
        24 -> ByteArray(data.size / 3 * 2).also { out ->
            var o = 0
            var i = 0
            while (i + 2 < data.size) {
                // Keep the two most significant bytes (truncate the low byte).
                out[o] = data[i + 1]
                out[o + 1] = data[i + 2]
                i += 3
                o += 2
            }
        }
        32 -> ByteArray(data.size / 2).also { out ->
            var o = 0
            var i = 0
            while (i + 3 < data.size) {
                out[o] = data[i + 2]
                out[o + 1] = data[i + 3]
                i += 4
                o += 2
            }
        }
        else -> throw IllegalArgumentException("unsupported PCM bit depth $bitDepth")
    }

    /** Float PCM (some Android decoders output it) to PCM16 little-endian. */
    fun floatToPcm16(samples: FloatArray, count: Int): ByteArray {
        val out = ByteArray(count * 2)
        for (i in 0 until count) {
            val v = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt()
            out[2 * i] = v.toByte()
            out[2 * i + 1] = (v shr 8).toByte()
        }
        return out
    }
}
