package io.github.riberrr.spinroom.core.audio

import io.github.riberrr.spinroom.core.audio.buffer.FakeClock
import io.github.riberrr.spinroom.core.audio.buffer.PcmFormat
import io.github.riberrr.spinroom.core.audio.output.AudioOutput
import io.github.riberrr.spinroom.core.audio.output.AudioOutputFactory
import io.github.riberrr.spinroom.core.sendspin.api.AudioChunk
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCommand
import io.github.riberrr.spinroom.core.sendspin.api.PlayerEvent
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Output in virtual time: frame N leaves the device at framesToUs(N); written audio is recorded. */
private class FakeOutput(override val format: PcmFormat) : AudioOutput {
    val recorded = ByteArrayOutputStream()
    @Volatile var amp = -1f
    override val bufferDurationUs = 100_000L
    override fun frameLocalTimeUs(framePosition: Long) = format.framesToUs(framePosition)
    override fun write(data: ByteArray, frames: Int) {
        synchronized(recorded) { recorded.write(data, 0, frames * format.bytesPerFrame) }
        Thread.sleep(1)
    }
    override fun setVolume(amplitude: Float) { amp = amplitude }
    override fun release() = Unit
    fun frames(): Int = synchronized(recorded) { recorded.size() } / format.bytesPerFrame
    fun bytes(): ByteArray = synchronized(recorded) { recorded.toByteArray() }
}

class SpinroomAudioEngineTest {
    private val clock = FakeClock(offsetUs = 1_000_000)
    private val outputs = mutableListOf<FakeOutput>()
    private val engine = SpinroomAudioEngine(
        clock = clock,
        outputFactory = AudioOutputFactory { f -> FakeOutput(f).also { synchronized(outputs) { outputs += it } } },
    )
    private val pcm = StreamFormat(Codec.PCM, 48_000, 1, 16)

    @After
    fun tearDown() = engine.release()

    private fun chunk(serverUs: Long, frames: Int, first: Int) = AudioChunk(
        timestampUs = serverUs,
        sendAheadUs = 0,
        data = ByteArray(frames * 2).also { d ->
            for (i in 0 until frames) {
                d[2 * i] = (first + i).toByte()
                d[2 * i + 1] = ((first + i) shr 8).toByte()
            }
        },
        arrivalLocalUs = 0,
        format = pcm,
    )

    private fun waitFor(timeoutMs: Long = 5000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond()) {
            check(System.currentTimeMillis() < end) { "timeout" }
            Thread.sleep(5)
        }
    }

    @Test
    fun playsChunkAtItsTimestamp() {
        engine.handle(PlayerEvent.StreamStarted(pcm))
        // Due 500 ms (24 000 frames) after the output starts.
        engine.handle(PlayerEvent.Chunk(chunk(clock.offsetUs + 500_000, 4800, first = 1)))
        waitFor { outputs.firstOrNull()?.let { it.frames() > 30_000 } == true }
        val out = outputs.first().bytes()
        fun s(i: Int) = ((out[2 * i + 1].toInt() shl 8) or (out[2 * i].toInt() and 0xFF)).toShort().toInt()
        assertEquals(0, s(23_999))
        assertEquals(1, s(24_000))
        assertEquals(4800, s(24_000 + 4799))
        assertEquals(0, s(24_000 + 4800))
        waitFor { engine.state.value is AudioEngineState.Buffering }
    }

    @Test
    fun volumeAndCommandsReachOutputAndPlayerState() {
        engine.start(pcm)
        engine.handle(PlayerEvent.Command(PlayerCommand.SetVolume(50)))
        engine.handle(PlayerEvent.Command(PlayerCommand.SetOutputDelay(120)))
        waitFor { outputs.firstOrNull()?.amp?.let { kotlin.math.abs(it - 0.35355f) < 1e-4f } == true }
        assertEquals(50, engine.playerState.value.volume)
        assertEquals(120, engine.playerState.value.outputDelayMs)
        engine.setMuted(true)
        waitFor { outputs.first().amp == 0f }
    }

    @Test
    fun clearRecreatesOutputAndStopGoesIdle() {
        engine.start(pcm)
        engine.submit(chunk(clock.offsetUs + 2_000_000, 4800, first = 1))
        waitFor { outputs.size == 1 && outputs[0].frames() > 1000 }
        engine.clear()
        waitFor { outputs.size == 2 }
        engine.stop()
        waitFor { engine.state.value == AudioEngineState.Idle }
        // The cleared chunk never played.
        assertTrue(outputs.all { o -> o.bytes().all { it == 0.toByte() } })
    }
}
