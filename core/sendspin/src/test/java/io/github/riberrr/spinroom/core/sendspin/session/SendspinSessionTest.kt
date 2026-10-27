package io.github.riberrr.spinroom.core.sendspin.session

import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCapabilities
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCommand
import io.github.riberrr.spinroom.core.sendspin.api.PlayerEvent
import io.github.riberrr.spinroom.core.sendspin.api.PlayerState
import io.github.riberrr.spinroom.core.sendspin.api.RepeatMode
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import io.github.riberrr.spinroom.core.sendspin.fixture
import io.github.riberrr.spinroom.core.sendspin.noise.Base64Url
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.PskCategory
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientCommand
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientGoodbye
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientHello
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientPairFinalize
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientPairInit
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientState
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageCodec
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageType
import io.github.riberrr.spinroom.core.sendspin.protocol.PairAbort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class SendspinSessionTest {

    private val formats = listOf(
        StreamFormat(Codec.FLAC, 48_000, 2, 16),
        StreamFormat(Codec.OPUS, 48_000, 2, 16),
        StreamFormat(Codec.PCM, 48_000, 2, 16),
    )
    private val baseConfig = SessionConfig(
        name = "Salon TV",
        deviceInfo = DeviceInfo("Android TV", "Spinroom", "0.1.0"),
        player = PlayerCapabilities(formats, 4 * 1024 * 1024),
    )

    private inner class Harness(
        val scope: TestScope,
        config: SessionConfig = baseConfig,
        val store: InMemoryCredentialStore = InMemoryCredentialStore(),
    ) {
        val inbox = ArrayDeque<OutgoingFrame>()
        val events = ArrayList<PlayerEvent>()
        val notices = ArrayList<SessionNotice>()
        val session = SendspinSession(config, store, scope.backgroundScope, { scope.testScheduler.currentTime * 1000 + 1_000_000 })

        init {
            val unconfined = UnconfinedTestDispatcher(scope.testScheduler)
            scope.backgroundScope.launch(unconfined) { session.outgoing.collect { inbox.addLast(it) } }
            scope.backgroundScope.launch(unconfined) { session.events.collect { events += it } }
            scope.backgroundScope.launch(unconfined) { session.notices.collect { notices += it } }
        }

        fun server(keys: io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair = io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair.generate()) =
            FakeServer(session, inbox, keys)

        /** Lets clock bursts run until the filter converged. */
        fun converge(server: FakeServer) {
            repeat(8) {
                scope.advanceTimeBy(150)
                scope.runCurrent()
                server.pump()
            }
            assertTrue("clock converged", session.clock.converged.value)
        }

        fun nowUs() = scope.testScheduler.currentTime * 1000 + 1_000_000

        fun lastState(server: FakeServer) = server.lastOf(MessageType.CLIENT_STATE).decode(ClientState.serializer())
    }

    // ---------------- handshake & hello ----------------

    @Test
    fun `handshake, hello and activation`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()

        val init = server.received.first()
        assertEquals(MessageType.CLIENT_INIT, init.type)
        assertArrayEquals(h.store.identity.publicKey, server.clientKey)

        val hello = server.lastOf(MessageType.CLIENT_HELLO).decode(ClientHello.serializer())
        assertEquals("Salon TV", hello.name)
        assertEquals(listOf("player@v1", "metadata@v1", "artwork@v1", "controller@v1"), hello.supportedRoles)
        assertEquals("flac", hello.playerSupport!!.supportedFormats.first().codec)
        assertTrue(hello.unpairedAccess.enabled)
        assertTrue("pairing_psk" in hello.supportedPairMethods)

        val state = h.session.state.value as ConnectionState.Connected
        assertEquals(server.serverId, state.serverId)
        assertEquals("Music Assistant", state.serverName)
        assertEquals(setOf(Activity.PLAYBACK), state.activities)
        assertFalse(state.paired)

        // Before the clock converged the player is not available.
        val cs = h.lastState(server)
        assertFalse(cs.available)
        assertEquals(50, cs.player!!.volume)
        assertEquals(600, cs.artwork!!.channels.single().width)
        assertTrue(server.receivedOf(MessageType.CLIENT_TIME).isNotEmpty())
    }

    @Test
    fun `clock converges and player becomes available`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        h.converge(server)
        assertTrue(h.lastState(server).available)

        val local = h.nowUs() + 3_000_000
        val mapped = h.session.clock.serverToLocalUs(server.serverTime(local))
        assertTrue("error ${mapped - local}", abs(mapped - local) < 100)
        // Bursts are at least 8 messages.
        assertTrue(server.receivedOf(MessageType.CLIENT_TIME).size >= 3 * 8)

        h.session.setAvailable(false)
        server.pump()
        assertFalse(h.lastState(server).available)
    }

    // ---------------- player role ----------------

    @Test
    fun `stream lifecycle and audio chunks`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()

        server.sendAudio(1, byteArrayOf(1)) // no stream yet: ignored
        server.sendJson(MessageType.STREAM_START, MessageCodec.decode(fixture("stream-start-flac.json")).payload.toString())
        server.sendAudio(5_100_000, byteArrayOf(0xA, 0xB))
        server.sendJson(MessageType.STREAM_CLEAR, """{"server_transmitted":1,"roles":["player"]}""")
        server.sendJson(MessageType.STREAM_START, MessageCodec.decode(fixture("stream-start-opus.json")).payload.toString())
        server.sendAudio(5_200_000, byteArrayOf(0xC))
        server.sendJson(MessageType.STREAM_END)
        server.sendAudio(5_300_000, byteArrayOf(0xD)) // after end: ignored

        val e = h.events
        assertEquals(6, e.size)
        val flac = (e[0] as PlayerEvent.StreamStarted).format
        assertEquals(Codec.FLAC, flac.codec)
        assertEquals(44_100, flac.sampleRate)
        assertEquals("fLaC", String(flac.codecHeader!!.copyOf(4)))
        val chunk = (e[1] as PlayerEvent.Chunk).chunk
        assertEquals(5_100_000L, chunk.timestampUs)
        assertEquals(100_000L, chunk.sendAheadUs)
        assertEquals(flac, chunk.format)
        assertArrayEquals(byteArrayOf(0xA, 0xB), chunk.data)
        assertEquals(PlayerEvent.StreamCleared, e[2])
        assertEquals(Codec.OPUS, (e[3] as PlayerEvent.StreamStarted).format.codec)
        assertEquals(Codec.OPUS, (e[4] as PlayerEvent.Chunk).chunk.format.codec)
        assertEquals(PlayerEvent.StreamEnded, e[5])
    }

    @Test
    fun `server commands reach the player and state reports go out`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()

        server.sendJson(MessageType.SERVER_COMMAND, """{"player":{"command":"volume","volume":55}}""")
        server.sendJson(MessageType.SERVER_COMMAND, """{"player":{"command":"mute","mute":true}}""")
        server.sendJson(MessageType.SERVER_COMMAND, """{"player":{"command":"dance"}}""")
        assertEquals(
            listOf(PlayerEvent.Command(PlayerCommand.SetVolume(55)), PlayerEvent.Command(PlayerCommand.SetMuted(true))),
            h.events,
        )

        val before = server.receivedOf(MessageType.CLIENT_STATE).size
        val st = PlayerState(volume = 55, muted = true, requiredLeadTimeMs = 300, minBufferMs = 400)
        h.session.report(st)
        h.session.report(st) // unchanged: not re-sent
        server.pump()
        assertEquals(before + 1, server.receivedOf(MessageType.CLIENT_STATE).size)
        val p = h.lastState(server).player!!
        assertEquals(55, p.volume)
        assertTrue(p.muted)
        assertEquals(300, p.requiredLeadTimeMs)
        assertEquals(listOf("volume", "mute", "set_output_delay"), p.supportedCommands)
    }

    // ---------------- metadata, controller, artwork ----------------

    @Test
    fun `metadata, group and controller commands`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()

        server.sendJson(MessageType.SERVER_STATE, MessageCodec.decode(fixture("server-state-metadata-controller.json")).payload.toString())
        server.sendJson(MessageType.GROUP_UPDATE, MessageCodec.decode(fixture("group-update.json")).payload.toString())

        val meta = h.session.metadata.value!!
        assertEquals("Pyramid Song", meta.title)
        assertEquals(12_000L, meta.progress!!.positionMs)
        val group = h.session.group.value!!
        assertEquals("Salon", group.groupName)
        assertTrue(group.playing)
        assertEquals(40, group.volume)
        assertEquals(RepeatMode.OFF, group.repeat)

        assertTrue(h.session.sendCommand(ControllerCommand.Pause))
        assertTrue(h.session.sendCommand(ControllerCommand.Seek(999_999)))
        h.session.send(ControllerCommand.SetGroupVolume(70))
        server.pump()
        val cmds = server.receivedOf(MessageType.CLIENT_COMMAND).map { it.decode(ClientCommand.serializer()).controller }
        assertEquals(listOf("pause", "seek", "volume"), cmds.map { it.command })
        assertEquals(289_000L, cmds[1].positionMs) // clamped to seek_max_ms
        assertEquals(70, cmds[2].volume)

        // Commands outside supported_commands are not sent.
        server.sendJson(MessageType.SERVER_STATE, """{"controller":{"supported_commands":["play"],"volume":10,"muted":false,"repeat":"all","shuffle":true}}""")
        assertFalse(h.session.sendCommand(ControllerCommand.Next))
        assertEquals(RepeatMode.ALL, h.session.group.value!!.repeat)
        assertEquals("Pyramid Song", h.session.metadata.value!!.title) // omitted role object keeps state
    }

    @Test
    fun `future metadata is applied at its timestamp`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        h.converge(server)

        val now = server.serverTime(h.nowUs())
        server.sendJson(MessageType.SERVER_STATE, """{"metadata":{"timestamp":$now,"title":"A"}}""")
        server.sendJson(MessageType.SERVER_STATE, """{"metadata":{"timestamp":${now + 2_000_000},"title":"B"}}""")
        assertEquals("A", h.session.metadata.value!!.title)
        advanceTimeBy(1_900); runCurrent()
        assertEquals("A", h.session.metadata.value!!.title)
        advanceTimeBy(200); runCurrent()
        assertEquals("B", h.session.metadata.value!!.title)
    }

    @Test
    fun `artwork is assembled from parts`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()

        server.sendPlain(io.github.riberrr.spinroom.core.sendspin.hexFixture("artwork-announce.hex").let {
            it.copyOf().also { b -> b[b.size - 1] = 5 } // total_size = 5
        })
        server.sendPlain(io.github.riberrr.spinroom.core.sendspin.hexFixture("artwork-part.hex"))
        assertNull(h.session.artwork.value)
        server.sendPlain(byteArrayOf(8, 0, 0xE0.toByte(), 0x00))
        val img = h.session.artwork.value!!
        assertEquals(5_000_000L, img.timestampUs)
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0), img.bytes)

        // total_size 0 clears the image.
        server.sendPlain(byteArrayOf(8, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0))
        assertNull(h.session.artwork.value!!.bytes)
    }

    @Test
    fun `fragmented server json is reassembled`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        val title = "x".repeat(100_000)
        server.sendJson(MessageType.SERVER_STATE, """{"metadata":{"timestamp":0,"title":"$title"}}""")
        assertEquals(title, h.session.metadata.value!!.title)
    }

    // ---------------- failures, disconnect & reconnect ----------------

    @Test
    fun `disconnect ends the stream and a new connection works`() = runTest {
        val h = Harness(this)
        val first = h.server()
        first.connect()
        first.sendJson(MessageType.STREAM_START, """{"player":{"codec":"pcm","sample_rate":48000,"channels":2,"bit_depth":16}}""")
        first.sendJson(MessageType.SERVER_STATE, """{"metadata":{"timestamp":0,"title":"A"}}""")

        h.session.onClosed("socket dropped")
        assertEquals(ConnectionState.Closed("socket dropped"), h.session.state.value)
        assertEquals(PlayerEvent.StreamEnded, h.events.last())
        assertNull(h.session.metadata.value)
        h.session.onClosed() // idempotent

        // Clock bursts of the dead connection stop.
        val sent = first.receivedOf(MessageType.CLIENT_TIME).size
        advanceTimeBy(20_000); runCurrent(); first.pump()
        assertEquals(sent, first.receivedOf(MessageType.CLIENT_TIME).size)

        val second = h.server()
        second.connect()
        assertTrue(h.session.state.value is ConnectionState.Connected)
        second.sendJson(MessageType.STREAM_START, """{"player":{"codec":"pcm","sample_rate":44100,"channels":2,"bit_depth":16}}""")
        second.sendAudio(10, byteArrayOf(1))
        assertEquals(44_100, (h.events.last() as PlayerEvent.Chunk).chunk.format.sampleRate)
        h.converge(second)
    }

    @Test
    fun `goodbye on shutdown`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        h.session.disconnect()
        server.pump()
        assertEquals("shutdown", server.lastOf(MessageType.CLIENT_GOODBYE).decode(ClientGoodbye.serializer()).reason)
        assertNotNull(server.closedReason)
    }

    @Test
    fun `tampered ciphertext closes silently`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        h.inbox.clear()
        h.session.onBinary(ByteArray(40))
        assertTrue(h.inbox.single() is OutgoingFrame.Close)
        assertTrue(h.session.state.value is ConnectionState.Closed)
    }

    @Test
    fun `cleartext after transport mode closes`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        h.session.onText(fixture("server-hello.json"))
        server.pump()
        assertNotNull(server.closedReason)
    }

    @Test
    fun `handshake step timeout`() = runTest {
        val h = Harness(this)
        h.session.onOpen()
        advanceTimeBy(29_000); runCurrent()
        assertEquals(ConnectionState.Handshaking, h.session.state.value)
        advanceTimeBy(2_000); runCurrent()
        assertTrue(h.session.state.value is ConnectionState.Closed)
        assertTrue(h.inbox.last() is OutgoingFrame.Close)
    }

    @Test
    fun `server error on init closes`() = runTest {
        val h = Harness(this)
        h.session.onOpen()
        h.session.onText(fixture("server-error.json"))
        assertEquals(SessionNotice.ServerError("unsupported_suite"), h.notices.single())
        assertTrue(h.session.state.value is ConnectionState.Closed)
    }

    // ---------------- pairing & PSK selection ----------------

    @Test
    fun `pairing psk flow, re-handshake to long-term and paired reconnect`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect(PskCategory.PAIRING, h.store.pairingPsk, activities = "[]", roles = "[]")
        server.sendJson(MessageType.SERVER_ACTIVATE, MessageCodec.decode(fixture("server-activate-pairing-psk.json")).payload.toString())
        server.sendJson(MessageType.SERVER_ACTIVATE, MessageCodec.decode(fixture("server-activate-pairing-psk.json")).payload.toString())
        server.pump()

        // pairing_index counts pairing activations since the handshake; the newest attempt wins.
        val inits = server.receivedOf(MessageType.CLIENT_PAIR_INIT).map { it.decode(ClientPairInit.serializer()).pairingIndex }
        assertEquals(listOf(1, 2), inits)
        val ltPsk = Base64Url.decode(server.lastOf(MessageType.CLIENT_PAIR_FINALIZE).decode(ClientPairFinalize.serializer()).longTermPsk)
        assertEquals(32, ltPsk.size)
        assertTrue(h.store.records().isEmpty())

        server.sendJson(MessageType.SERVER_PAIR_FINALIZE)
        assertEquals(server.serverId, h.store.records().single().serverId)
        assertArrayEquals(ltPsk, h.store.records().single().longTermPsk)
        assertEquals(SessionNotice.Paired(server.serverId), h.notices.last())

        server.rehandshake(PskCategory.LONG_TERM, ltPsk)
        server.sendJson(MessageType.SERVER_ACTIVATE, MessageCodec.decode(fixture("server-activate-playback.json")).payload.toString())
        server.pump()
        val state = h.session.state.value as ConnectionState.Connected
        assertTrue(state.paired)
        assertEquals(4, state.activeRoles.size)
        server.sendJson(MessageType.STREAM_START, """{"player":{"codec":"pcm","sample_rate":48000,"channels":2,"bit_depth":16}}""")
        assertTrue(h.events.last() is PlayerEvent.StreamStarted)

        // Later connection from the same server on the long-term PSK.
        h.session.onClosed()
        val again = h.server(server.keys)
        again.connect(PskCategory.LONG_TERM, ltPsk)
        assertTrue((h.session.state.value as ConnectionState.Connected).paired)
    }

    @Test
    fun `unknown long-term psk falls back to sentinel`() = runTest {
        val h = Harness(this)
        val server = h.server()
        h.session.onOpen()
        server.startHandshake(PskCategory.LONG_TERM, psk = Psk.SENTINEL, pskId = Psk.id(Psk.generate()))
        assertNotNull(server.transport)
        server.sendJson(MessageType.SERVER_HELLO, "{}")
        server.pump()
        assertTrue(server.receivedOf(MessageType.CLIENT_HELLO).isNotEmpty())
    }

    @Test
    fun `record bound to another server fails the handshake`() = runTest {
        val h = Harness(this)
        val psk = Psk.generate()
        h.store.save(PairingRecord("someone-else", psk))
        val server = h.server()
        h.session.onOpen()
        server.startHandshake(PskCategory.LONG_TERM, psk)
        assertNull(server.transport)
        assertTrue(h.session.state.value is ConnectionState.Closed)
    }

    @Test
    fun `sentinel playback without unpaired access requires pairing`() = runTest {
        val h = Harness(this, baseConfig.copy(unpairedAccess = false))
        val server = h.server()
        server.connect()
        assertEquals(GoodbyeReasonCheck.PAIRING_REQUIRED, server.lastOf(MessageType.CLIENT_GOODBYE).decode(ClientGoodbye.serializer()).reason)
        assertTrue(h.session.state.value is ConnectionState.Closed)
        assertFalse(server.receivedOf(MessageType.CLIENT_HELLO).single().decode(ClientHello.serializer()).unpairedAccess.enabled)
    }

    @Test
    fun `pairing activity on a long-term session is unauthorized`() = runTest {
        val h = Harness(this)
        val server = h.server()
        val psk = Psk.generate()
        h.store.save(PairingRecord(server.serverId, psk))
        server.connect(PskCategory.LONG_TERM, psk, activities = """["pairing"]""", roles = "[]")
        assertEquals("unauthorized", server.lastOf(MessageType.CLIENT_GOODBYE).decode(ClientGoodbye.serializer()).reason)
    }

    @Test
    fun `pairing psk method on sentinel is aborted but the connection stays`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect(activities = """["pairing"]""", roles = "[]")
        server.sendJson(MessageType.SERVER_ACTIVATE, """{"activities":["pairing"],"pairing":{"method":"pairing_psk"}}""")
        server.pump()
        assertEquals("method_not_supported", server.lastOf(MessageType.PAIR_ABORT).decode(PairAbort.serializer()).reason)
        assertTrue(server.receivedOf(MessageType.CLIENT_PAIR_INIT).isEmpty())
        assertTrue(h.session.state.value is ConnectionState.Connected)
    }

    @Test
    fun `pairing attempt times out`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect(PskCategory.PAIRING, h.store.pairingPsk, activities = "[]", roles = "[]")
        server.sendJson(MessageType.SERVER_ACTIVATE, """{"activities":["pairing"],"pairing":{"method":"pairing_psk"}}""")
        server.answerTime = false
        advanceTimeBy(121_000); runCurrent(); server.pump()
        assertEquals("attempt_timeout", server.lastOf(MessageType.PAIR_ABORT).decode(PairAbort.serializer()).reason)
        server.sendJson(MessageType.SERVER_PAIR_FINALIZE) // late: nothing persisted
        assertTrue(h.store.records().isEmpty())
    }

    @Test
    fun `server unpair removes the record`() = runTest {
        val h = Harness(this)
        val server = h.server()
        val psk = Psk.generate()
        h.store.save(PairingRecord(server.serverId, psk))
        server.connect(PskCategory.LONG_TERM, psk)
        server.sendJson(MessageType.SERVER_UNPAIR)
        server.pump()
        assertTrue(h.store.records().isEmpty())
        assertEquals("unpaired", server.lastOf(MessageType.CLIENT_GOODBYE).decode(ClientGoodbye.serializer()).reason)
        assertEquals(SessionNotice.Unpaired(server.serverId), h.notices.last())
    }

    @Test
    fun `re-handshake with unknown psk closes`() = runTest {
        val h = Harness(this)
        val server = h.server()
        server.connect()
        server.rehandshake(PskCategory.LONG_TERM, Psk.generate())
        assertTrue(h.session.state.value is ConnectionState.Closed)
    }

    @Test
    fun `credential store keeps capacity with lru eviction`() {
        val store = InMemoryCredentialStore(capacity = 5)
        repeat(5) { store.save(PairingRecord("s$it", Psk.generate())) }
        store.touch("s0")
        store.save(PairingRecord("s5", Psk.generate()))
        assertEquals(listOf("s2", "s3", "s4", "s0", "s5"), store.records().map { it.serverId })
    }

    private object GoodbyeReasonCheck {
        const val PAIRING_REQUIRED = "pairing_required"
    }
}
