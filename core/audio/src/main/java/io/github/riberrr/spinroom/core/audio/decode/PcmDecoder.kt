package io.github.riberrr.spinroom.core.audio.decode

import io.github.riberrr.spinroom.core.audio.buffer.PcmBlock
import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat

/** PCM needs no codec, only bit-depth conversion, so it skips MediaCodec. */
class PcmDecoder(override val format: StreamFormat) : AudioDecoder {
    private val pcmFormat = PcmFormat(format.sampleRate, format.channels)
    private val inFrameBytes = format.channels * format.bitDepth / 8

    override fun decode(data: ByteArray, timestampUs: Long, sink: (PcmBlock) -> Unit) {
        val whole = data.size - data.size % inFrameBytes
        if (whole == 0) return
        val src = if (whole == data.size) data else data.copyOf(whole)
        sink(PcmBlock(timestampUs, PcmConvert.toPcm16(src, format.bitDepth), pcmFormat))
    }

    override fun flush() = Unit
    override fun release() = Unit
}
