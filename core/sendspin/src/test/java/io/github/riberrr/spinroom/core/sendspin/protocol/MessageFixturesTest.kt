package io.github.riberrr.spinroom.core.sendspin.protocol

import io.github.riberrr.spinroom.core.sendspin.fixture
import io.github.riberrr.spinroom.core.sendspin.hexFixture
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.PskCategory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageFixturesTest {

    /** Decodes a fixture, checks its type, re-encodes it and compares JSON trees. */
    private fun <T> roundTrip(name: String, type: String, serializer: KSerializer<T>): T {
        val text = fixture(name)
        val env = MessageCodec.decode(text)
        assertEquals(type, env.type)
        val value = env.decode(serializer)
        val again = MessageCodec.encode(type, serializer, value)
        assertEquals(Json.parseToJsonElement(text), Json.parseToJsonElement(again))
        return value
    }

    @Test fun `client init`() {
        val m = roundTrip("client-init.json", MessageType.CLIENT_INIT, ClientInit.serializer())
        assertEquals("25519_ChaChaPoly_SHA256", m.suite)
        assertEquals(1, m.version)
    }

    @Test fun `server init`() {
        assertEquals(43, roundTrip("server-init.json", MessageType.SERVER_INIT, ServerInit.serializer()).serverId.length)
    }

    @Test fun `server error`() {
        assertEquals("unsupported_suite", roundTrip("server-error.json", MessageType.SERVER_ERROR, ServerError.serializer()).reason)
    }

    @Test fun `noise handshake envelope`() {
        roundTrip("noise-handshake.json", MessageType.NOISE_HANDSHAKE, NoiseHandshake.serializer())
    }

    @Test fun `message 1 inner payload`() {
        val p = MessageCodec.json.decodeFromString(HandshakeMessage1Payload.serializer(), fixture("noise-msg1-inner-payload.json"))
        assertEquals(Psk.id(Psk.SENTINEL), p.pskId)
        assertEquals(PskCategory.SENTINEL, PskCategory.fromWire(p.pskCategory))
    }

    @Test fun `server hello`() {
        assertEquals(listOf("pl", "en"), roundTrip("server-hello.json", MessageType.SERVER_HELLO, ServerHello.serializer()).languages)
    }

    @Test fun `client hello`() {
        val m = roundTrip("client-hello.json", MessageType.CLIENT_HELLO, ClientHello.serializer())
        assertEquals(4, m.playerSupport!!.supportedFormats.size)
        assertTrue(m.unpairedAccess.enabled)
        assertTrue("pairing_psk" in m.supportedPairMethods)
    }

    @Test fun `server activate`() {
        val playback = roundTrip("server-activate-playback.json", MessageType.SERVER_ACTIVATE, ServerActivate.serializer())
        assertEquals(listOf("playback"), playback.activities)
        assertNull(playback.pairing)
        val pairing = roundTrip("server-activate-pairing-psk.json", MessageType.SERVER_ACTIVATE, ServerActivate.serializer())
        assertEquals("pairing_psk", pairing.pairing!!.method)
        assertEquals(emptyList<String>(), pairing.activeRoles)
    }

    @Test fun `client state`() {
        val m = roundTrip("client-state-player.json", MessageType.CLIENT_STATE, ClientState.serializer())
        assertEquals(300, m.player!!.requiredLeadTimeMs)
        assertEquals(600, m.artwork!!.channels.single().width)
    }

    @Test fun `server command`() {
        assertEquals(55, roundTrip("server-command-volume.json", MessageType.SERVER_COMMAND, ServerCommand.serializer()).player!!.volume)
    }

    @Test fun `client command`() {
        assertEquals("pause", roundTrip("client-command-pause.json", MessageType.CLIENT_COMMAND, ClientCommand.serializer()).controller.command)
    }

    @Test fun `stream messages`() {
        val flac = roundTrip("stream-start-flac.json", MessageType.STREAM_START, StreamStart.serializer())
        assertEquals("flac", flac.player!!.codec)
        assertTrue(java.util.Base64.getDecoder().decode(flac.player.codecHeader).copyOf(4).contentEquals("fLaC".toByteArray()))
        assertNull(roundTrip("stream-start-opus.json", MessageType.STREAM_START, StreamStart.serializer()).player!!.codecHeader)
        assertEquals(listOf("player"), roundTrip("stream-clear.json", MessageType.STREAM_CLEAR, StreamClear.serializer()).roles)
        assertNull(roundTrip("stream-end.json", MessageType.STREAM_END, StreamEnd.serializer()).roles)
    }

    @Test fun `group update`() {
        assertEquals("Salon", roundTrip("group-update.json", MessageType.GROUP_UPDATE, GroupUpdate.serializer()).groupName)
    }

    @Test fun `server state`() {
        val m = roundTrip("server-state-metadata-controller.json", MessageType.SERVER_STATE, ServerState.serializer())
        assertEquals("Pyramid Song", m.metadata!!.title)
        assertEquals(289000L, m.metadata.progress!!.trackDuration)
        assertEquals(289000L, m.controller!!.seekMaxMs)
    }

    @Test fun `time messages`() {
        assertEquals(1_000_000L, roundTrip("client-time.json", MessageType.CLIENT_TIME, ClientTime.serializer()).clientTransmitted)
        assertEquals(5_000_950L, roundTrip("server-time.json", MessageType.SERVER_TIME, ServerTime.serializer()).serverTransmitted)
    }

    @Test fun `goodbye and pairing`() {
        assertEquals("shutdown", roundTrip("client-goodbye.json", MessageType.CLIENT_GOODBYE, ClientGoodbye.serializer()).reason)
        assertEquals(1, roundTrip("client-pair-init.json", MessageType.CLIENT_PAIR_INIT, ClientPairInit.serializer()).pairingIndex)
        assertEquals(43, roundTrip("client-pair-finalize.json", MessageType.CLIENT_PAIR_FINALIZE, ClientPairFinalize.serializer()).longTermPsk.length)
        roundTrip("server-pair-finalize.json", MessageType.SERVER_PAIR_FINALIZE, kotlinx.serialization.json.JsonObject.serializer())
        assertEquals("method_not_supported", roundTrip("pair-abort.json", MessageType.PAIR_ABORT, PairAbort.serializer()).reason)
    }

    @Test fun `unknown fields are ignored`() {
        val env = MessageCodec.decode("""{"type":"group/update","payload":{"playback_state":"stopped","x":1},"extra":true}""")
        assertEquals("stopped", env.decode(GroupUpdate.serializer()).playbackState)
    }

    @Test fun `malformed envelopes fail`() {
        assertThrows(ProtocolException::class.java) { MessageCodec.decode("not json") }
        assertThrows(ProtocolException::class.java) { MessageCodec.decode("""{"payload":{}}""") }
        assertThrows(ProtocolException::class.java) { MessageCodec.decode("""{"type":"x","payload":[]}""") }
        assertThrows(ProtocolException::class.java) { MessageCodec.decode("""{"type":"server/time","payload":{}}""").decode(ServerTime.serializer()) }
    }

    // ---- binary fixtures ----

    @Test fun `audio chunk`() {
        val f = BinaryFrames.parse(hexFixture("audio-chunk.hex")) as BinaryFrame.Audio
        assertEquals(5_100_000L, f.timestampUs)
        assertEquals(100_000L, f.sendAheadUs)
        assertArrayEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()), f.data)
    }

    @Test fun `audio shorter than 13 bytes is an error`() {
        assertThrows(ProtocolException::class.java) { BinaryFrames.parse(hexFixture("audio-chunk.hex").copyOf(12)) }
        assertEquals(0, (BinaryFrames.parse(hexFixture("audio-chunk.hex").copyOf(13)) as BinaryFrame.Audio).data.size)
    }

    @Test fun `artwork announce and part`() {
        val a = BinaryFrames.parse(hexFixture("artwork-announce.hex")) as BinaryFrame.ArtworkAnnounce
        assertEquals(0, a.channel)
        assertEquals(5_000_000L, a.timestampUs)
        assertEquals(3L, a.totalSize)
        val p = BinaryFrames.parse(hexFixture("artwork-part.hex")) as BinaryFrame.ArtworkPart
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), p.data)
        assertTrue(BinaryFrames.parse(byteArrayOf(9, 1)) is BinaryFrame.ArtworkCancel)
    }

    @Test fun `first fragment`() {
        val f = BinaryFrames.parse(hexFixture("fragment-first.hex")) as BinaryFrame.Fragment
        assertTrue(f.first)
        assertEquals(false, f.last)
        assertEquals(0, f.origType)
        assertEquals("{\"type\":\"", String(f.data))
    }

    @Test fun `unknown and empty binary`() {
        assertTrue(BinaryFrames.parse(byteArrayOf(16, 1, 2)) is BinaryFrame.Unknown)
        assertThrows(ProtocolException::class.java) { BinaryFrames.parse(ByteArray(0)) }
    }

    @Test fun `large json is fragmented and reassembled`() {
        val big = MessageCodec.encode("server/state", Json.parseToJsonElement("""{"_x":"${"a".repeat(150_000)}"}"""))
        val parts = BinaryFrames.encodeJson(big)
        assertEquals(3, parts.size)
        assertTrue(parts.all { it.size <= BinaryFrames.MAX_PLAINTEXT })
        val assembler = FragmentAssembler()
        val results = parts.map { assembler.accept(BinaryFrames.parse(it) as BinaryFrame.Fragment) }
        assertNull(results[0]); assertNull(results[1])
        val (type, body) = results[2]!!
        assertEquals(0, type)
        assertEquals(big, String(body))
    }

    @Test fun `bad fragment sequences fail`() {
        val cont = BinaryFrames.parse(byteArrayOf(1, 1, 65)) as BinaryFrame.Fragment
        assertThrows(ProtocolException::class.java) { FragmentAssembler().accept(cont) }
        val first = BinaryFrames.parse(byteArrayOf(1, 2, 0, 65)) as BinaryFrame.Fragment
        val a = FragmentAssembler()
        a.accept(first)
        assertThrows(ProtocolException::class.java) { a.accept(first) }
        assertThrows(ProtocolException::class.java) { BinaryFrames.parse(byteArrayOf(1, 6, 0)) }
    }
}
