package io.github.riberrr.spinroom.core.audio.decode

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import io.github.riberrr.spinroom.core.audio.buffer.PcmBlock
import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * FLAC / Opus via the platform decoder, used synchronously. Input presentation times carry the server
 * timestamp of each codec unit, so output blocks keep their exact server time even if the codec lags.
 * The platform FLAC decoder exists from API 27; see [io.github.riberrr.spinroom.core.audio.AudioCapabilities].
 */
class MediaCodecDecoder(override val format: StreamFormat) : AudioDecoder {
    private val codec: MediaCodec
    private var outFormat = PcmFormat(format.sampleRate, format.channels)
    private var outFloat = false
    private val info = MediaCodec.BufferInfo()
    private var nextFlacNumber: Long? = null
    private var unitsWithoutOutput = 0

    init {
        val mime = mimeFor(format.codec)
        val mf = MediaFormat.createAudioFormat(mime, format.sampleRate, format.channels)
        mf.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        when (format.codec) {
            Codec.FLAC -> {
                val header = requireNotNull(format.codecHeader) { "FLAC stream without codec_header" }
                mf.setByteBuffer("csd-0", ByteBuffer.wrap(header))
            }
            Codec.OPUS -> {
                mf.setByteBuffer("csd-0", ByteBuffer.wrap(OpusHeaders.opusHead(format.channels, format.sampleRate)))
                mf.setByteBuffer("csd-1", ByteBuffer.wrap(OpusHeaders.nanosLe(0)))
                mf.setByteBuffer("csd-2", ByteBuffer.wrap(OpusHeaders.nanosLe(0)))
            }
            Codec.PCM -> error("PCM is decoded by PcmDecoder")
        }
        codec = MediaCodec.createDecoderByType(mime)
        try {
            codec.configure(mf, null, null, 0)
            codec.start()
        } catch (e: Exception) {
            codec.release()
            throw e
        }
    }

    override fun decode(data: ByteArray, timestampUs: Long, sink: (PcmBlock) -> Unit) {
        if (format.codec == Codec.FLAC) {
            var samples = 0L
            for (frame in FlacFrames.split(data)) {
                val expected = nextFlacNumber
                nextFlacNumber = frame.number + if (frame.variableBlocking) frame.blockSize.toLong() else 1L
                if (expected != null && expected != frame.number) {
                    // The platform decoder goes silent after a frame-number jump (encoder restart, loop).
                    Log.i(TAG, "FLAC frame number jump $expected -> ${frame.number}, resetting decoder")
                    drain(sink, DRAIN_BEFORE_FLUSH_US)
                    codec.flush()
                    unitsWithoutOutput = 0
                }
                val ts = timestampUs + samples * 1_000_000L / format.sampleRate
                queue(data, frame.offset, frame.length, ts, sink)
                samples += frame.blockSize
            }
        } else {
            queue(data, 0, data.size, timestampUs, sink)
        }
        drain(sink, OUTPUT_TIMEOUT_US)
    }

    private fun queue(data: ByteArray, offset: Int, length: Int, ptsUs: Long, sink: (PcmBlock) -> Unit) {
        var index = codec.dequeueInputBuffer(INPUT_TIMEOUT_US)
        while (index < 0) {
            drain(sink, 0)
            index = codec.dequeueInputBuffer(INPUT_TIMEOUT_US)
        }
        val buf = codec.getInputBuffer(index)!!
        buf.clear()
        buf.put(data, offset, length)
        codec.queueInputBuffer(index, 0, length, ptsUs, 0)
        unitsWithoutOutput++
        drain(sink, 0)
        if (unitsWithoutOutput >= STALL_UNITS) {
            // Backstop for other silent decoder failures: lose a few units rather than the whole stream.
            Log.w(TAG, "${format.codec} decoder stalled for $unitsWithoutOutput units, resetting")
            codec.flush()
            unitsWithoutOutput = 0
        }
    }

    private fun drain(sink: (PcmBlock) -> Unit, firstTimeoutUs: Long) {
        var timeout = firstTimeoutUs
        while (true) {
            val index = codec.dequeueOutputBuffer(info, timeout)
            timeout = 0
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> readOutputFormat(codec.outputFormat)
                index >= 0 -> {
                    if (info.size > 0) {
                        emit(index, sink)
                        unitsWithoutOutput = 0
                    }
                    codec.releaseOutputBuffer(index, false)
                }
                else -> return // INFO_TRY_AGAIN_LATER (or deprecated buffers-changed)
            }
        }
    }

    private fun emit(index: Int, sink: (PcmBlock) -> Unit) {
        val buf = codec.getOutputBuffer(index)!!
        buf.position(info.offset).limit(info.offset + info.size)
        val pcm = if (outFloat) {
            val fb = buf.order(ByteOrder.nativeOrder()).asFloatBuffer()
            val arr = FloatArray(fb.remaining()).also { fb.get(it) }
            PcmConvert.floatToPcm16(arr, arr.size)
        } else {
            ByteArray(info.size).also { buf.get(it) }
        }
        val whole = pcm.size - pcm.size % outFormat.bytesPerFrame
        if (whole > 0) {
            sink(PcmBlock(info.presentationTimeUs, if (whole == pcm.size) pcm else pcm.copyOf(whole), outFormat))
        }
    }

    private fun readOutputFormat(mf: MediaFormat) {
        val rate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        outFloat = mf.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
            mf.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
        outFormat = PcmFormat(rate, ch)
        Log.d(TAG, "decoder output ${format.codec} $rate Hz, $ch ch, float=$outFloat")
    }

    override fun flush() {
        codec.flush()
        nextFlacNumber = null
        unitsWithoutOutput = 0
    }

    override fun release() {
        runCatching { codec.stop() }
        codec.release()
    }

    companion object {
        private const val TAG = "Spinroom"
        private const val INPUT_TIMEOUT_US = 10_000L
        private const val OUTPUT_TIMEOUT_US = 2_000L
        private const val DRAIN_BEFORE_FLUSH_US = 20_000L
        private const val STALL_UNITS = 6

        fun mimeFor(codec: Codec): String = when (codec) {
            Codec.FLAC -> MediaFormat.MIMETYPE_AUDIO_FLAC
            Codec.OPUS -> MediaFormat.MIMETYPE_AUDIO_OPUS
            Codec.PCM -> MediaFormat.MIMETYPE_AUDIO_RAW
        }
    }
}
