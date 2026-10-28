package io.github.riberrr.spinroom.core.audio

import io.github.riberrr.spinroom.core.sendspin.api.AudioChunk
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCommand
import io.github.riberrr.spinroom.core.sendspin.api.PlayerEvent
import io.github.riberrr.spinroom.core.sendspin.api.PlayerState
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

sealed interface AudioEngineState {
    /** No stream; output closed. */
    data object Idle : AudioEngineState

    /** Stream started, output open, waiting for due audio (start, underrun, after clear). */
    data class Buffering(val format: StreamFormat) : AudioEngineState

    /** Real audio is going out. */
    data class Playing(val format: StreamFormat) : AudioEngineState

    /** Decoder or output failed; the engine stays stopped until the next [AudioEngine.start]. */
    data class Error(val message: String) : AudioEngineState
}

/** Live numbers for the debug screen and latency calibration (S14c). */
data class AudioStats(
    val bufferedMs: Int = 0,
    /** How far ahead of "now" the write cursor is: device buffer + delay to the speaker. */
    val outputLatencyMs: Int = 0,
    /** Last sync error (target − cursor), µs. */
    val syncErrorUs: Long = 0,
    val droppedFrames: Long = 0,
    val correctedFrames: Long = 0,
    val resyncs: Int = 0,
    val underruns: Int = 0,
)

/**
 * Plays timestamped Sendspin audio. Thread-safe; calls return immediately (decoding runs on an internal
 * thread, output on another). Events must arrive in wire order — [handle] maps them 1:1.
 */
interface AudioEngine {
    val state: StateFlow<AudioEngineState>
    val stats: StateFlow<AudioStats>

    /** Player part of `client/state`; send it with `PlayerStateReporter.report` whenever it changes. */
    val playerState: StateFlow<PlayerState>

    /** `stream/start`. Mid-stream it only switches format; chunks keep their own format, no gap. */
    fun start(format: StreamFormat)

    /** One audio chunk, in arrival order. */
    fun submit(chunk: AudioChunk)

    /** `stream/clear`: drop buffered audio, keep the output open. */
    fun clear()

    /** `stream/end`: stop output and drop everything. */
    fun stop()

    /** 0–100; amplitude follows (v/100)^1.5. */
    fun setVolume(volume: Int)

    fun setMuted(muted: Boolean)

    /** Sendspin `output_delay_ms` (set by MA via `set_output_delay`, reported back), 0–5000 ms. */
    fun setOutputDelayMs(delayMs: Int)

    /**
     * Local calibration on top of [setOutputDelayMs], −500…+500 ms (S14c). Positive = audio leaves earlier
     * (slow soundbar), negative = later. Not reported to the server. Applies at once (realigns playback).
     */
    fun setCalibrationMs(offsetMs: Int)

    /** Stops and frees the threads; the engine cannot be used afterwards. */
    fun release()

    fun handle(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.StreamStarted -> start(event.format)
            is PlayerEvent.Chunk -> submit(event.chunk)
            PlayerEvent.StreamCleared -> clear()
            PlayerEvent.StreamEnded -> stop()
            is PlayerEvent.Command -> when (val c = event.command) {
                is PlayerCommand.SetVolume -> setVolume(c.volume)
                is PlayerCommand.SetMuted -> setMuted(c.muted)
                is PlayerCommand.SetOutputDelay -> setOutputDelayMs(c.delayMs)
            }
        }
    }
}

object Volume {
    /** Sendspin volume curve: amplitude = (volume/100)^1.5; muted is independent of volume. */
    fun amplitude(volume: Int, muted: Boolean): Float =
        if (muted) 0f else (volume.coerceIn(0, 100) / 100.0).pow(1.5).toFloat()
}
