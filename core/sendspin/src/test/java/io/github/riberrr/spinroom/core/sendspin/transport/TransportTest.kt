package io.github.riberrr.spinroom.core.sendspin.transport

import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCapabilities
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageType
import io.github.riberrr.spinroom.core.sendspin.session.ConnectionState
import io.github.riberrr.spinroom.core.sendspin.session.InMemoryCredentialStore
import io.github.riberrr.spinroom.core.sendspin.session.SendspinSession
import io.github.riberrr.spinroom.core.sendspin.session.SessionConfig
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TransportTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = SendspinSession(
        SessionConfig(
            name = "Test TV",
            player = PlayerCapabilities(listOf(StreamFormat(Codec.PCM, 48_000, 2, 16)), 1 shl 20),
        ),
        InMemoryCredentialStore(),
        scope,
    )
    private val bridge = SessionBridge(session, scope)
    private val http = OkHttpClient()
    private val cleanups = ArrayList<() -> Unit>()

    @After
    fun tearDown() {
        cleanups.reversed().forEach { runCatching { it() } }
        scope.cancel()
    }

    /** Records what a test-side WebSocket client sees. */
    private class Probe : WebSocketListener() {
        val texts = LinkedBlockingQueue<String>()
        val closes = LinkedBlockingQueue<Int>()
        val failures = LinkedBlockingQueue<Int>()

        override fun onMessage(webSocket: WebSocket, text: String) { texts += text }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            closes += code
            webSocket.close(code, null)
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            failures += response?.code ?: -1
        }
    }

    private fun startServer(): Int {
        val server = SendspinServer(bridge, port = 0)
        server.start()
        cleanups += { server.stop() }
        return server.boundPort()
    }

    private fun connect(port: Int, origin: String? = null): Pair<WebSocket, Probe> {
        val probe = Probe()
        val req = Request.Builder().url("ws://127.0.0.1:$port/sendspin")
            .apply { if (origin != null) header("Origin", origin) }.build()
        val ws = http.newWebSocket(req, probe)
        cleanups += { ws.cancel() }
        return ws to probe
    }

    private fun awaitState(predicate: (ConnectionState) -> Boolean) = runBlocking {
        withTimeout(5_000) { session.state.first(predicate) }
    }

    @Test
    fun serverSendsClientInitOnConnect() {
        val port = startServer()
        val (_, probe) = connect(port)
        val first = probe.texts.poll(5, TimeUnit.SECONDS)
        assertTrue("expected client/init, got $first", first!!.contains(MessageType.CLIENT_INIT))
        assertTrue(bridge.isConnected)
        assertEquals(ConnectionState.Handshaking, session.state.value)
    }

    @Test
    fun serverRejectsOriginHeader() {
        val port = startServer()
        val (_, probe) = connect(port, origin = "http://evil.example")
        assertEquals(403, probe.failures.poll(5, TimeUnit.SECONDS))
        assertTrue(!bridge.isConnected)
    }

    @Test
    fun serverRejectsSecondConnection() {
        val port = startServer()
        val (_, first) = connect(port)
        first.texts.poll(5, TimeUnit.SECONDS)!!
        val (_, second) = connect(port)
        assertEquals(CloseReason.Codes.TRY_AGAIN_LATER.code.toInt(), second.closes.poll(5, TimeUnit.SECONDS))
        assertTrue(second.texts.isEmpty())
        assertTrue(bridge.isConnected)
    }

    @Test
    fun peerCloseClosesSessionAndAllowsReconnect() {
        val port = startServer()
        val (ws, probe) = connect(port)
        probe.texts.poll(5, TimeUnit.SECONDS)!!
        ws.close(1000, "bye")
        awaitState { it is ConnectionState.Closed }
        val (_, again) = connect(port)
        assertTrue(again.texts.poll(5, TimeUnit.SECONDS)!!.contains(MessageType.CLIENT_INIT))
    }

    @Test
    fun garbageBinaryIsCountedAndClosesConnection() {
        val port = startServer()
        val (ws, probe) = connect(port)
        probe.texts.poll(5, TimeUnit.SECONDS)!!
        ws.send(byteArrayOf(1, 2, 3).toByteString()) // binary before the handshake = protocol error
        assertTrue(probe.closes.poll(5, TimeUnit.SECONDS) != null)
        assertEquals(1L, bridge.receivedBinaryFrames)
        awaitState { it is ConnectionState.Closed }
    }

    @Test
    fun clientConnectsAndReconnects() {
        val inits = LinkedBlockingQueue<String>()
        val server = embeddedServer(CIO, port = 0, host = "127.0.0.1") {
            install(WebSockets)
            routing {
                webSocket("/sendspin") {
                    inits += (incoming.receive() as Frame.Text).readText()
                    close(CloseReason(CloseReason.Codes.GOING_AWAY, "restart"))
                }
            }
        }.start(wait = false)
        cleanups += { server.stop(0, 500) }
        val port = runBlocking { server.engine.resolvedConnectors().first().port }

        val client = SendspinClient(bridge, SendspinClient.urlFor("127.0.0.1:$port"), scope, minBackoffMs = 50)
        client.start()
        cleanups += { client.stop() }
        assertTrue(inits.poll(5, TimeUnit.SECONDS)!!.contains(MessageType.CLIENT_INIT))
        assertTrue(inits.poll(5, TimeUnit.SECONDS)!!.contains(MessageType.CLIENT_INIT))
    }

    @Test
    fun urlForAcceptsWhatUsersType() {
        assertEquals("ws://10.0.1.50:8927/sendspin", SendspinClient.urlFor("10.0.1.50"))
        assertEquals("ws://10.0.1.50:9000/sendspin", SendspinClient.urlFor(" 10.0.1.50:9000/ "))
        assertEquals("ws://ma.local:8927/sendspin", SendspinClient.urlFor("ma.local"))
        assertEquals("ws://[fe80::1]:8927/sendspin", SendspinClient.urlFor("fe80::1"))
        assertEquals("ws://[fe80::1]:9000/sendspin", SendspinClient.urlFor("[fe80::1]:9000"))
        assertEquals("ws://x:1/custom", SendspinClient.urlFor("ws://x:1/custom"))
    }

    @Test
    fun truncateUtf8KeepsWholeCharacters() {
        assertEquals("abc", truncateUtf8("abc", 10))
        assertEquals("Sal", truncateUtf8("Salon", 3))
        assertEquals("Łó", truncateUtf8("Łóżko", 5)) // Ł, ó = 2 B each, ż would split
        assertEquals("", truncateUtf8("🎵", 3))
    }
}
