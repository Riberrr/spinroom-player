package io.github.riberrr.spinroom.core.audio

import android.os.Process
import android.util.Log
import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import io.github.riberrr.spinroom.core.audio.buffer.PlaybackScheduler
import io.github.riberrr.spinroom.core.audio.buffer.SyncConfig
import io.github.riberrr.spinroom.core.audio.buffer.TimedPcmBuffer
import io.github.riberrr.spinroom.core.audio.decode.AudioDecoder
import io.github.riberrr.spinroom.core.audio.decode.AudioDecoderFactory
import io.github.riberrr.spinroom.core.audio.decode.DefaultAudioDecoderFactory
import io.github.riberrr.spinroom.core.audio.output.AudioOutput
import io.github.riberrr.spinroom.core.audio.output.AudioOutputFactory
import io.github.riberrr.spinroom.core.audio.output.AudioTrackOutput
import io.github.riberrr.spinroom.core.sendspin.api.AudioChunk
import io.github.riberrr.spinroom.core.sendspin.api.ClockSource
import io.github.riberrr.spinroom.core.sendspin.api.PlayerState
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Default [AudioEngine]. Two threads:
 * - `Spinroom-decode` runs every public call in order (start, decode, clear, stop), so clear/stop always
 *   cut exactly between the chunks sent before and after them;
 * - `Spinroom-audio` (urgent audio priority) asks [PlaybackScheduler] for 10 ms at a time and blocks on
 *   the output's write, which paces it.
 *
 * [clock] must use the System.nanoTime base for `localNowUs`/`serverToLocalUs`, because AudioTrack
 * timestamps do.
 */
class SpinroomAudioEngine(
    private val clock: ClockSource,
    private val decoderFactory: AudioDecoderFactory = DefaultAudioDecoderFactory,
    private val outputFactory: AudioOutputFactory = AudioTrackOutput.Factory,
    initialVolume: Int = 100,
    initialMuted: Boolean = false,
    syncConfig: SyncConfig = SyncConfig(),
) : AudioEngine {

    private val lock = Any()
    private val buffer = TimedPcmBuffer()
    private val scheduler = PlaybackScheduler(buffer, clock, syncConfig)
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "Spinroom-decode") }

    // Decode thread only.
    private var decoder: AudioDecoder? = null
    private var loop: OutputLoop? = null

    @Volatile private var outputFormat: PcmFormat? = null
    @Volatile private var calibrationMs = 0
    @Volatile private var outputLeadMs = DEFAULT_LEAD_TIME_MS
    @Volatile private var resetOutput = false

    private val _state = MutableStateFlow<AudioEngineState>(AudioEngineState.Idle)
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(AudioStats())
    override val stats: StateFlow<AudioStats> = _stats.asStateFlow()

    private val _playerState = MutableStateFlow(
        PlayerState(
            volume = initialVolume.coerceIn(0, 100),
            muted = initialMuted,
            outputDelayMs = 0,
            requiredLeadTimeMs = DEFAULT_LEAD_TIME_MS,
            minBufferMs = MIN_BUFFER_MS,
        ),
    )
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    override fun start(format: StreamFormat) = post {
        outputFormat = PcmFormat(format.sampleRate, format.channels)
        if (loop?.thread?.isAlive != true) {
            stopNow() // clears a loop that died on an output error
            synchronized(lock) {
                buffer.clear()
                scheduler.unlock()
                scheduler.resetStats()
            }
            _state.value = AudioEngineState.Buffering(format)
            loop = OutputLoop(format).also { it.thread.start() }
        }
        Log.i(TAG, "stream start ${format.codec} ${format.sampleRate}/${format.channels}/${format.bitDepth}")
    }

    override fun submit(chunk: AudioChunk) = post {
        if (loop == null) return@post // chunk without stream/start, or after stream/end
        val dec = decoder?.takeIf { it.format == chunk.format } ?: run {
            decoder?.release()
            decoderFactory.create(chunk.format).also { decoder = it }
        }
        dec.decode(chunk.data, chunk.timestampUs) { block ->
            synchronized(lock) { buffer.add(block) }
        }
    }

    override fun clear() = post {
        decoder?.flush()
        synchronized(lock) {
            buffer.clear()
            scheduler.unlock()
        }
        resetOutput = true
    }

    override fun stop() = post { stopNow() }

    override fun setVolume(volume: Int) {
        _playerState.update { it.copy(volume = volume.coerceIn(0, 100)) }
    }

    override fun setMuted(muted: Boolean) {
        _playerState.update { it.copy(muted = muted) }
    }

    override fun setOutputDelayMs(delayMs: Int) {
        val ms = delayMs.coerceIn(0, MAX_OUTPUT_DELAY_MS)
        _playerState.update { it.copy(outputDelayMs = ms) }
        applyDelay()
    }

    override fun setCalibrationMs(offsetMs: Int) {
        calibrationMs = offsetMs.coerceIn(-MAX_CALIBRATION_MS, MAX_CALIBRATION_MS)
        applyDelay()
    }

    /** Scheduler delay = protocol delay + local calibration; a change realigns at once instead of drifting. */
    private fun applyDelay() {
        val totalUs = (_playerState.value.outputDelayMs + calibrationMs) * 1000L
        synchronized(lock) {
            if (scheduler.outputDelayUs != totalUs) {
                scheduler.outputDelayUs = totalUs
                scheduler.unlock()
            }
        }
        updateLeadTime()
    }

    /** Playing earlier (positive calibration) needs that much more lead time from the server. */
    private fun updateLeadTime() {
        val lead = maxOf(DEFAULT_LEAD_TIME_MS, outputLeadMs) + maxOf(0, calibrationMs)
        _playerState.update { if (it.requiredLeadTimeMs == lead) it else it.copy(requiredLeadTimeMs = lead) }
    }

    override fun release() {
        post { stopNow() }
        worker.shutdown()
        worker.awaitTermination(2, TimeUnit.SECONDS)
    }

    private fun stopNow() {
        loop?.let {
            it.running = false
            it.thread.join(2000)
        }
        loop = null
        decoder?.release()
        decoder = null
        synchronized(lock) { buffer.clear() }
        if (_state.value !is AudioEngineState.Error) _state.value = AudioEngineState.Idle
        _stats.value = AudioStats()
    }

    private fun post(block: () -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "audio engine failure", e)
                _state.value = AudioEngineState.Error(e.message ?: e.javaClass.simpleName)
                stopNow()
            }
        }
    }

    private inner class OutputLoop(private val format: StreamFormat) : Runnable {
        @Volatile var running = true
        val thread = Thread(this, "Spinroom-audio")

        override fun run() {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
            var output: AudioOutput? = null
            var buf = ByteArray(0)
            var written = 0L
            var appliedAmp = -1f
            var lastAudioNs = 0L
            var lastStatsNs = 0L
            try {
                while (running) {
                    val want = synchronized(lock) { buffer.headFormat() } ?: output?.format ?: outputFormat ?: break
                    if (output == null || output.format != want || resetOutput) {
                        resetOutput = false
                        output?.release()
                        output = outputFactory.create(want)
                        written = 0
                        appliedAmp = -1f
                        synchronized(lock) { scheduler.unlock() }
                        outputLeadMs = ((output.bufferDurationUs + LEAD_MARGIN_US) / 1000).toInt()
                        updateLeadTime()
                    }
                    val ps = _playerState.value
                    val amp = Volume.amplitude(ps.volume, ps.muted)
                    if (amp != appliedAmp) {
                        output.setVolume(amp)
                        appliedAmp = amp
                    }

                    val frames = want.sampleRate * FILL_MS / 1000
                    if (buf.size < frames * want.bytesPerFrame) buf = ByteArray(frames * want.bytesPerFrame)
                    val cursor = output.frameLocalTimeUs(written)
                    val result = synchronized(lock) { scheduler.fill(buf, frames, want, cursor) }
                    output.write(buf, frames)
                    written += frames

                    val now = System.nanoTime()
                    if (result.audioFrames > 0) {
                        lastAudioNs = now
                        if (_state.value !is AudioEngineState.Playing) _state.value = AudioEngineState.Playing(format)
                    } else if (now - lastAudioNs > BUFFERING_AFTER_NS && _state.value is AudioEngineState.Playing) {
                        _state.value = AudioEngineState.Buffering(format)
                    }
                    if (now - lastStatsNs > STATS_INTERVAL_NS) {
                        lastStatsNs = now
                        publishStats(want, cursor)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "audio output failure", e)
                _state.value = AudioEngineState.Error(e.message ?: e.javaClass.simpleName)
            } finally {
                output?.release()
            }
        }

        private fun publishStats(format: PcmFormat, cursorUs: Long?) {
            val (s, bufferedFrames) = synchronized(lock) { scheduler.stats to buffer.bufferedFrames }
            _stats.value = AudioStats(
                bufferedMs = (format.framesToUs(bufferedFrames) / 1000).toInt(),
                outputLatencyMs = cursorUs?.let { ((it - clock.localNowUs()) / 1000).toInt() } ?: 0,
                syncErrorUs = s.lastErrorUs,
                droppedFrames = s.droppedFrames,
                correctedFrames = s.correctedFrames,
                resyncs = s.resyncs,
                underruns = s.underruns,
            )
        }
    }

    companion object {
        private const val TAG = "Spinroom"
        private const val FILL_MS = 10
        private const val DEFAULT_LEAD_TIME_MS = 300
        private const val MIN_BUFFER_MS = 200
        private const val MAX_OUTPUT_DELAY_MS = 5000
        const val MAX_CALIBRATION_MS = 500

        /** Decode time + startup until the first AudioTrack timestamp (silence is written meanwhile). */
        private const val LEAD_MARGIN_US = 250_000L
        private const val BUFFERING_AFTER_NS = 300_000_000L
        private const val STATS_INTERVAL_NS = 250_000_000L
    }
}
