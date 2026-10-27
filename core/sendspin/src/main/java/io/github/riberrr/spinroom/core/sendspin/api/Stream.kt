package io.github.riberrr.spinroom.core.sendspin.api

enum class Codec { PCM, FLAC, OPUS }

/** Format of the audio carried by [AudioChunk]s (from `stream/start` player object). */
data class StreamFormat(
    val codec: Codec,
    val sampleRate: Int,
    val channels: Int,
    /** Ignored for [Codec.OPUS]. */
    val bitDepth: Int,
    /** Decoded from Base64; FLAC: `fLaC` marker + STREAMINFO. Null for PCM and Opus. */
    val codecHeader: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is StreamFormat && codec == other.codec && sampleRate == other.sampleRate &&
            channels == other.channels && bitDepth == other.bitDepth &&
            codecHeader.contentEquals(other.codecHeader)

    override fun hashCode(): Int =
        listOf(codec, sampleRate, channels, bitDepth, codecHeader?.contentHashCode()).hashCode()
}

/** One binary audio message (type 4), already decrypted. */
class AudioChunk(
    /** Server clock µs at which the first sample must leave the speaker (before output delay). */
    val timestampUs: Long,
    /** Lead the server had when sending; only for jitter measurement, never for scheduling. */
    val sendAheadUs: Long,
    /** One codec unit sequence: PCM frames, FLAC frames, or exactly one Opus packet. */
    val data: ByteArray,
    /** Local monotonic µs when the last byte arrived (for jitter / min_buffer_ms). */
    val arrivalLocalUs: Long,
    /** Format in effect when the chunk was received. */
    val format: StreamFormat,
)
