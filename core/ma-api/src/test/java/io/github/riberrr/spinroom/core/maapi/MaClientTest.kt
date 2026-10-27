package io.github.riberrr.spinroom.core.maapi

import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MaClientTest {
    private val server = MockWebServer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val received = CopyOnWriteArrayList<JsonObject>()
    private val connections = CopyOnWriteArrayList<WebSocket>()

    private val serverInfo = """{"server_id":"x","server_version":"2.10.8","schema_version":65,"min_supported_schema_version":28,"base_url":"   http://10.0.0.1:8095"}"""

    /** Fake MA: sends server_info, accepts only token "good", answers canned results by command. */
    private fun fakeMa(respond: (WebSocket, String, String, JsonObject) -> Unit = { _, _, _, _ -> }) {
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connections += webSocket
                    webSocket.send(serverInfo)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val msg = MaJson.parseToJsonElement(text).jsonObject
                    received += msg
                    val id = msg["message_id"]!!.jsonPrimitive.content
                    val cmd = msg["command"]!!.jsonPrimitive.content
                    val a = msg["args"]?.jsonObject ?: JsonObject(emptyMap())
                    if (cmd == "auth") {
                        if (a["token"]?.jsonPrimitive?.content == "good") webSocket.send("""{"message_id":"$id","result":{"ok":true}}""")
                        else webSocket.send("""{"message_id":"$id","error_code":23,"details":"Invalid token"}""")
                    } else {
                        respond(webSocket, id, cmd, a)
                    }
                }
            }),
        )
    }

    private fun newClient() = MaClient(OkHttpClient(), scope, backoffMs = listOf(50), defaultTimeoutMs = 3_000)

    @Before fun setUp() = server.start()

    @After fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun base() = "http://${server.hostName}:${server.port}"

    @Test fun authenticatesAndCorrelatesResponses() = runBlocking {
        fakeMa { ws, id, cmd, a ->
            ws.send("""{"message_id":"$id","result":[{"item_id":"1","provider":"library","media_type":"album","name":"$cmd","unknown_field":5}]}""")
        }
        val c = newClient()
        c.connect(base(), "good")
        val api = LibraryApi(c)
        val items = api.items(MediaTypes.ALBUMS, limit = 2, offset = 4)
        assertEquals("music/albums/library_items", items.single().name)
        val sent = received.last { it["command"]!!.jsonPrimitive.content != "auth" }
        assertEquals("4", sent["args"]!!.jsonObject["offset"]!!.jsonPrimitive.content)
        assertEquals("auth", received.first()["command"]!!.jsonPrimitive.content)
        assertTrue((c.state.value as MaConnectionState.Connected).authenticated)
        c.disconnect()
    }

    @Test fun mergesPartialChunks() = runBlocking {
        fakeMa { ws, id, _, _ ->
            ws.send("""{"message_id":"$id","result":[{"item_id":"1","name":"a"}],"partial":true}""")
            ws.send("""{"message_id":"$id","result":[{"item_id":"2","name":"b"}],"partial":false}""")
        }
        val c = newClient()
        c.connect(base(), "good")
        val r = LibraryApi(c).playlistTracks("p", "library")
        assertEquals(listOf("a", "b"), r.map { it.name })
        c.disconnect()
    }

    @Test fun errorCodeBecomesException() = runBlocking {
        fakeMa { ws, id, _, _ -> ws.send("""{"message_id":"$id","error_code":999,"details":"nope"}""") }
        val c = newClient()
        c.connect(base(), "good")
        val e = runCatching { BrowseApi(c).recommendations() }.exceptionOrNull() as MaException
        assertEquals(999, e.code)
        c.disconnect()
    }

    @Test fun badTokenFailsWithoutReconnect() = runBlocking {
        fakeMa()
        val c = newClient()
        c.connect(base(), "bad")
        val s = withTimeout(5_000) { c.state.first { it is MaConnectionState.Failed } } as MaConnectionState.Failed
        assertEquals(MaException.INVALID_TOKEN, s.code)
        assertEquals(1, server.requestCount)
        c.disconnect()
    }

    @Test fun emitsEvents() = runBlocking {
        fakeMa()
        val c = newClient()
        c.connect(base(), "good")
        withTimeout(5_000) { c.state.first { it is MaConnectionState.Connected } }
        val pending = scope.async { c.events.first() }
        delay(100)
        connections.last().send("""{"event":"queue_updated","object_id":"q1","data":{"queue_id":"q1","state":"playing","shuffle_enabled":true,"items":12}}""")
        val ev = withTimeout(5_000) { pending.await() }
        assertEquals("queue_updated", ev.event)
        val q = ev.asQueue()
        assertNotNull(q)
        assertTrue(q!!.shuffleEnabled)
        assertEquals("playing", q.state)
        c.disconnect()
    }

    @Test fun reconnectsAfterDrop() = runBlocking {
        fakeMa()
        fakeMa()
        val c = newClient()
        c.connect(base(), "good")
        withTimeout(5_000) { c.state.first { it is MaConnectionState.Connected } }
        connections.first().close(1001, "bye")
        withTimeout(5_000) { c.state.first { it is MaConnectionState.Reconnecting } }
        withTimeout(5_000) { c.state.first { it is MaConnectionState.Connected } }
        assertEquals(2, connections.size)
        c.disconnect()
    }

    @Test fun loginFlowReturnsLongToken() = runBlocking {
        fakeMa { ws, id, cmd, _ ->
            when (cmd) {
                "auth/login" -> ws.send("""{"message_id":"$id","result":{"success":true,"token":"good","user":{}}}""")
                "auth/token/create" -> ws.send("""{"message_id":"$id","result":"long-token"}""")
            }
        }
        val token = AuthApi(OkHttpClient()).login(base(), "u", "p", "Spinroom TV", scope)
        assertEquals("long-token", token)
    }

    @Test fun loginWrongPassword() = runBlocking {
        fakeMa { ws, id, _, _ -> ws.send("""{"message_id":"$id","result":{"success":false,"error":"Invalid username or password"}}""") }
        val e = runCatching { AuthApi(OkHttpClient()).login(base(), "u", "x", "TV", scope) }.exceptionOrNull()
        assertTrue(e is AuthApi.LoginFailed)
    }

    @Test fun urlNormalization() {
        assertEquals("ws://10.0.0.1:8095/ws", MaClient.wsUrl("10.0.0.1"))
        assertEquals("ws://h:9000/ws", MaClient.wsUrl(" http://h:9000/ "))
        assertEquals("wss://h:8095/ws", MaClient.wsUrl("https://h"))
    }

    @Test fun toleratesItemMappingAndUnknownFields() {
        val el = MaJson.parseToJsonElement("""[{"item_id":"1","name":"A","artists":[{"item_id":"2","name":"B"}],"image":{"path":"p","provider":"x"},"future":{"a":1}},"garbage"]""")
        val items = el.decodeList<MediaItem>()
        assertEquals(1, items.size)
        assertEquals("B", items[0].artistLine)
        assertFalse(items[0].favorite)
        assertEquals("p", items[0].bestImage?.path)
    }

    @Test fun argsSkipNulls() {
        val a = args("a" to 1, "b" to null, "c" to listOf("x", "y"))
        assertEquals(setOf("a", "c"), a.keys)
        assertEquals(2, (a["c"] as JsonArray).size)
        assertEquals(JsonPrimitive(1), a["a"])
    }
}
