package io.github.riberrr.spinroom.core.audio.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class FlacFramesTest {
    private val enc = TestFlacEncoder(48_000, 2, 2048)

    private fun sine(n: Int, start: Int, hz: Double) =
        IntArray(n) { i -> (12000 * sin(2 * PI * hz * (start + i) / 48_000)).toInt() }

    private fun noise(n: Int, rnd: Random) = IntArray(n) { rnd.nextInt(-32768, 32768) }

    @Test
    fun splitsWholeFramesWithBlockSizes() {
        val f0 = enc.frame(0, arrayOf(sine(2048, 0, 440.0), sine(2048, 0, 660.0)))
        val f1 = enc.frame(1, arrayOf(sine(2048, 2048, 440.0), sine(2048, 2048, 660.0)))
        val f2 = enc.frame(2, arrayOf(sine(1000, 4096, 440.0), sine(1000, 4096, 660.0)))
        val frames = FlacFrames.split(f0 + f1 + f2)
        assertEquals(
            listOf(
                FlacFrame(0, f0.size, 2048, 0),
                FlacFrame(f0.size, f1.size, 2048, 1),
                FlacFrame(f0.size + f1.size, f2.size, 1000, 2),
            ),
            frames,
        )
    }

    @Test
    fun noiseFramesWithFakeSyncCodesSplitCorrectly() {
        // White noise is stored almost verbatim, so 0xFF 0xF8 patterns show up inside frames.
        val rnd = Random(7)
        repeat(20) { round ->
            val parts = List(3) { i -> enc.frame(round * 3 + i, arrayOf(noise(2048, rnd), noise(2048, rnd))) }
            val frames = FlacFrames.split(parts.reduce(ByteArray::plus))
            assertEquals(parts.map { it.size }, frames.map { it.length })
        }
    }

    @Test
    fun readsMultiByteFrameNumbers() {
        val a = enc.frame(127, arrayOf(sine(2048, 0, 440.0), sine(2048, 0, 440.0)))
        val b = enc.frame(300, arrayOf(sine(2048, 0, 440.0), sine(2048, 0, 440.0)))
        val c = enc.frame(5000, arrayOf(sine(2048, 0, 440.0), sine(2048, 0, 440.0)))
        assertEquals(listOf(127L, 300L, 5000L), FlacFrames.split(a + b + c).map { it.number })
    }

    @Test
    fun parseHeaderRejectsGarbage() {
        assertNull(FlacFrames.parseHeader(byteArrayOf(-1, -8, 0, 0, 0, 0, 0), 0))
        val f = enc.frame(5, arrayOf(sine(2048, 0, 440.0), sine(2048, 0, 440.0)))
        assertNotNull(FlacFrames.parseHeader(f, 0))
        f[4] = (f[4] + 1).toByte() // frame number changes -> CRC-8 fails
        assertNull(FlacFrames.parseHeader(f, 0))
    }

    @Test
    fun emptyAndHeaderlessInput() {
        assertEquals(emptyList<FlacFrame>(), FlacFrames.split(ByteArray(0)))
        assertEquals(emptyList<FlacFrame>(), FlacFrames.split(ByteArray(100) { 1 }))
    }

    /**
     * Writes the debug-screen sample (6 s, 48 kHz stereo: 440 Hz left, 554 Hz right, 20 ms fades) to
     * build/test-asset/. Copy it to src/debug/assets/ after changing the signal.
     */
    @Test
    fun writeDebugAsset() {
        val rate = 48_000
        val total = rate * 6
        val fade = rate / 50
        fun tone(hz: Double, i: Int): Int {
            val env = minOf(1.0, i / fade.toDouble(), (total - 1 - i) / fade.toDouble())
            return (10000 * env * sin(2 * PI * hz * i / rate)).toInt()
        }
        val out = java.io.ByteArrayOutputStream()
        out.write(enc.header(total.toLong()))
        var frame = 0
        var pos = 0
        while (pos < total) {
            val n = minOf(2048, total - pos)
            out.write(enc.frame(frame++, arrayOf(IntArray(n) { tone(440.0, pos + it) }, IntArray(n) { tone(554.37, pos + it) })))
            pos += n
        }
        val file = File("build/test-asset/spinroom-test-tone.flac")
        file.parentFile.mkdirs()
        file.writeBytes(out.toByteArray())
        val body = out.toByteArray().copyOfRange(42, out.size())
        assertEquals((total + 2047) / 2048, FlacFrames.split(body).size)
    }
}
