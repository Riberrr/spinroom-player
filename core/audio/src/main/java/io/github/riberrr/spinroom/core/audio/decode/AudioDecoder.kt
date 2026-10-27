package io.github.riberrr.spinroom.core.audio.decode

import io.github.riberrr.spinroom.core.audio.buffer.PcmBlock
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat

/** Turns one chunk of encoded audio into timestamped PCM16 blocks. Used from a single thread. */
interface AudioDecoder {
    val format: StreamFormat

    /** Decodes [data] whose first sample has server time [timestampUs]; emits blocks to [sink] in order. */
    fun decode(data: ByteArray, timestampUs: Long, sink: (PcmBlock) -> Unit)

    /** Drops internal state (stream/clear). */
    fun flush()

    fun release()
}

fun interface AudioDecoderFactory {
    fun create(format: StreamFormat): AudioDecoder
}

/** Default factory: Kotlin for PCM, MediaCodec for FLAC and Opus. */
object DefaultAudioDecoderFactory : AudioDecoderFactory {
    override fun create(format: StreamFormat): AudioDecoder = when (format.codec) {
        Codec.PCM -> PcmDecoder(format)
        Codec.FLAC, Codec.OPUS -> MediaCodecDecoder(format)
    }
}
