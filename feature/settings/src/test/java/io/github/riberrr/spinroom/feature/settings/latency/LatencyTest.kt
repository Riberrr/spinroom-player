package io.github.riberrr.spinroom.feature.settings.latency

import io.github.riberrr.spinroom.core.maapi.Player
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LatencyTest {
    @Test fun wavHeaderDescribesPcm16Mono48k() {
        val h = ByteBuffer.wrap(TestSignal.header()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(TestSignal.header(), 0, 4))
        assertEquals(TestSignal.totalBytes - 8, h.getInt(4).toLong())
        assertEquals(1, h.getShort(22).toInt()) // channels
        assertEquals(48_000, h.getInt(24))
        assertEquals(16, h.getShort(34).toInt())
        assertEquals(TestSignal.dataBytes, h.getInt(40).toLong())
    }

    @Test fun beepsStartOnEverySecondAndAreSilentOtherwise() {
        val sr = TestSignal.SAMPLE_RATE.toLong()
        assertEquals(0, TestSignal.sample(0).toInt()) // starts at a zero crossing, fade-in
        assertTrue((1..200L).any { TestSignal.sample(it).toInt() != 0 })
        assertTrue((sr / 10 until sr).all { TestSignal.sample(it).toInt() == 0 })
        assertTrue((sr + 1..sr + 200).any { TestSignal.sample(it).toInt() != 0 })
    }

    @Test fun readMatchesHeaderAndSamplesAtAnyOffset() {
        val all = ByteArray(200)
        TestSignal.read(0, all)
        val part = ByteArray(100)
        TestSignal.read(37, part)
        assertArrayEquals(all.copyOfRange(37, 137), part)
        assertEquals(0, TestSignal.read(TestSignal.totalBytes, part))
    }

    @Test fun serverServesFileAndRanges() {
        TestSignalServer(InetAddress.getLoopbackAddress()).use { s ->
            val url = URL("http://127.0.0.1:${s.port}${TestSignalServer.PATH}")
            (url.openConnection() as HttpURLConnection).run {
                assertEquals(200, responseCode)
                assertEquals(TestSignal.totalBytes, contentLengthLong)
                assertEquals("audio/wav", contentType)
                val first = inputStream.use { it.readNBytes(44) }
                assertArrayEquals(TestSignal.header(), first)
                disconnect()
            }
            (url.openConnection() as HttpURLConnection).run {
                setRequestProperty("Range", "bytes=1000-")
                assertEquals(206, responseCode)
                assertEquals(TestSignal.totalBytes - 1000, contentLengthLong)
                val expect = ByteArray(64).also { TestSignal.read(1000, it) }
                assertArrayEquals(expect, inputStream.use { it.readNBytes(64) })
                disconnect()
            }
            val missing = URL("http://127.0.0.1:${s.port}/other").openConnection() as HttpURLConnection
            assertEquals(404, missing.responseCode)
        }
        assertEquals(5L, TestSignalServer.parseRangeStart(" bytes=5-"))
        assertNull(TestSignalServer.parseRangeStart("bytes=-5"))
    }

    private fun p(id: String, name: String = id.uppercase(), syncedTo: String? = null, members: List<String> = emptyList()) =
        Player(playerId = id, name = name, syncedTo = syncedTo, groupMembers = members)

    @Test fun groupWhenTvIsSyncedToLeader() {
        val players = listOf(p("tv", syncedTo = "kitchen"), p("kitchen", members = listOf("kitchen", "tv")), p("bath"))
        assertEquals(CalibrationGroup("kitchen", listOf("KITCHEN")), CalibrationGroups.find(players, "tv"))
    }

    @Test fun groupPlayerIsVirtual() {
        val players = listOf(p("tv"), p("all", members = listOf("tv", "a", "b")), p("a"), p("b"))
        assertEquals(CalibrationGroup("all", listOf("A", "B")), CalibrationGroups.find(players, "tv"))
    }

    @Test fun tvLeadingOrAlone() {
        assertEquals(CalibrationGroup("tv", listOf("A")), CalibrationGroups.find(listOf(p("tv", members = listOf("tv", "a")), p("a")), "tv"))
        assertNull(CalibrationGroups.find(listOf(p("tv"), p("a")), "tv"))
        assertNull(CalibrationGroups.find(listOf(p("tv", members = listOf("tv"))), "tv"))
        assertNull(CalibrationGroups.find(listOf(p("a")), ""))
    }
}
