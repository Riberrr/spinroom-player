package io.github.riberrr.spinroom.core.audio.output

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat

/**
 * [AudioOutput] on a streaming PCM16 [AudioTrack]. Timing: [OutputClockEstimator] fed with the playback head
 * and with [AudioTrack.getTimestamp] (polled every 50 ms until trusted, then once a second, per the docs).
 */
class AudioTrackOutput(override val format: PcmFormat) : AudioOutput {
    private val track: AudioTrack
    override val bufferDurationUs: Long

    private val ts = AudioTimestamp()
    private var lastPollNanos = 0L
    private val clock = OutputClockEstimator(format)

    init {
        val channelMask = if (format.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        require(format.channels in 1..2) { "only mono/stereo output" }
        val minBytes = AudioTrack.getMinBufferSize(format.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val targetBytes = (format.sampleRate * TARGET_BUFFER_MS / 1000) * format.bytesPerFrame
        val bytes = maxOf(minBytes * 2, targetBytes)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(format.sampleRate)
                    .setChannelMask(channelMask)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bytes)
            .build()
        val frames = track.bufferSizeInFrames.takeIf { it > 0 } ?: (bytes / format.bytesPerFrame)
        bufferDurationUs = format.framesToUs(frames.toLong())
        track.play()
    }

    override fun frameLocalTimeUs(framePosition: Long): Long? {
        val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        val now = System.nanoTime()
        clock.onHead(head, now / 1000)
        val interval = if (clock.usingTimestamp) POLL_STEADY_NS else POLL_STARTUP_NS
        if (now - lastPollNanos >= interval) {
            lastPollNanos = now
            if (track.getTimestamp(ts)) clock.onTimestamp(ts.framePosition, ts.nanoTime)
        }
        val base = clock.baseUs() ?: return null
        return base + format.framesToUs(framePosition)
    }

    override fun write(data: ByteArray, frames: Int) {
        var off = 0
        val bytes = frames * format.bytesPerFrame
        while (off < bytes) {
            val n = track.write(data, off, bytes - off, AudioTrack.WRITE_BLOCKING)
            if (n < 0) throw IllegalStateException("AudioTrack.write failed: $n")
            off += n
        }
    }

    override fun setVolume(amplitude: Float) {
        track.setVolume(amplitude.coerceIn(0f, 1f))
    }

    override fun release() {
        runCatching { track.pause() }
        runCatching { track.flush() }
        track.release()
    }

    companion object {
        private const val TARGET_BUFFER_MS = 150
        private const val POLL_STARTUP_NS = 50_000_000L
        private const val POLL_STEADY_NS = 1_000_000_000L

        val Factory = AudioOutputFactory { AudioTrackOutput(it) }
    }
}
