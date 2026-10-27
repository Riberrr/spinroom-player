package io.github.riberrr.spinroom.core.maapi

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed interface MaConnectionState {
    data object Disconnected : MaConnectionState
    data object Connecting : MaConnectionState

    /** Socket open; [authenticated] is false when no token was given (login flow). */
    data class Connected(val server: ServerInfo, val authenticated: Boolean) : MaConnectionState
    data class Reconnecting(val attempt: Int, val cause: String?) : MaConnectionState

    /** Token rejected (code 23) or server too old; reconnecting would not help. */
    data class Failed(val code: Int, val reason: String) : MaConnectionState
}

/**
 * Music Assistant WebSocket client: one socket, `message_id` correlation, per-command timeout,
 * automatic reconnect with exponential backoff, server push as [events].
 * Never logs anything: command args may contain tokens/passwords.
 */
class MaClient(
    private val http: OkHttpClient,
    private val scope: CoroutineScope,
    private val backoffMs: List<Long> = listOf(1_000, 2_000, 4_000, 8_000, 16_000, 30_000),
    private val defaultTimeoutMs: Long = 15_000,
) {
    private val _state = MutableStateFlow<MaConnectionState>(MaConnectionState.Disconnected)
    val state: StateFlow<MaConnectionState> = _state

    private val _events = MutableSharedFlow<MaEvent>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<MaEvent> = _events

    private class Pending {
        val result = CompletableDeferred<JsonElement>()
        val chunks = mutableListOf<JsonElement>()
    }

    private val pending = ConcurrentHashMap<String, Pending>()
    private val ids = AtomicLong()
    @Volatile private var socket: WebSocket? = null
    private var job: Job? = null

    /** (Re)connects to [baseUrl] (`http://host:8095`, `host`, `host:8095`…). [token] null = unauthenticated (login flow). */
    fun connect(baseUrl: String, token: String?) {
        disconnect()
        val url = wsUrl(baseUrl)
        _state.value = MaConnectionState.Connecting
        job = scope.launch { runLoop(url, token) }
    }

    fun disconnect() {
        job?.cancel()
        job = null
        socket?.cancel()
        socket = null
        failAll(MaException(MaException.LOCAL, "Rozłączono"))
        _state.value = MaConnectionState.Disconnected
    }

    /** Sends [command] and returns its `result` (partial chunks are merged into one array). Waits for the connection. */
    suspend fun command(command: String, args: JsonObject? = null, timeoutMs: Long = defaultTimeoutMs): JsonElement {
        withTimeout(timeoutMs) { _state.first { it is MaConnectionState.Connected } }
        return send(command, args, timeoutMs)
    }

    private suspend fun send(command: String, args: JsonObject?, timeoutMs: Long): JsonElement {
        val ws = socket ?: throw MaException(MaException.LOCAL, "Brak połączenia")
        val id = ids.incrementAndGet().toString()
        val p = Pending()
        pending[id] = p
        val msg = JsonObject(
            buildMap {
                put("message_id", JsonPrimitive(id))
                put("command", JsonPrimitive(command))
                if (args != null) put("args", args)
            },
        )
        if (!ws.send(msg.toString())) {
            pending.remove(id)
            throw MaException(MaException.LOCAL, "Brak połączenia")
        }
        try {
            return withTimeout(timeoutMs) { p.result.await() }
        } finally {
            pending.remove(id)
        }
    }

    private suspend fun runLoop(url: String, token: String?) {
        var attempt = 0
        while (true) {
            val serverInfo = CompletableDeferred<ServerInfo>()
            val closed = CompletableDeferred<String?>()
            val ws = http.newWebSocket(Request.Builder().url(url).build(), Listener(serverInfo, closed))
            socket = ws
            var cause: String?
            try {
                val info = withTimeout(defaultTimeoutMs) { serverInfo.await() }
                if (!info.isSupported) {
                    _state.value = MaConnectionState.Failed(0, "Serwer Music Assistant jest zbyt stary")
                    ws.close(1000, null)
                    return
                }
                if (token != null) send("auth", JsonObject(mapOf("token" to JsonPrimitive(token))), defaultTimeoutMs)
                _state.value = MaConnectionState.Connected(info, authenticated = token != null)
                attempt = 0
                cause = closed.await()
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    cause = "Przekroczono czas oczekiwania"
                } else {
                    ws.cancel()
                    throw e
                }
            } catch (e: MaException) {
                if (e.code == MaException.INVALID_TOKEN || e.code == MaException.AUTH_REQUIRED) {
                    _state.value = MaConnectionState.Failed(e.code, e.message ?: "Nieprawidłowy token")
                    ws.close(1000, null)
                    return
                }
                cause = e.message
            } catch (e: Exception) {
                cause = e.message
            }
            ws.cancel()
            socket = null
            failAll(MaException(MaException.LOCAL, cause ?: "Połączenie zerwane"))
            attempt++
            _state.value = MaConnectionState.Reconnecting(attempt, cause)
            delay(backoffMs[(attempt - 1).coerceAtMost(backoffMs.lastIndex)])
        }
    }

    private fun failAll(e: Throwable) {
        pending.values.forEach { it.result.completeExceptionally(e) }
        pending.clear()
    }

    private inner class Listener(
        private val serverInfo: CompletableDeferred<ServerInfo>,
        private val closed: CompletableDeferred<String?>,
    ) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { MaJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
            val id = obj["message_id"]?.jsonPrimitive?.content
            when {
                id != null -> handleResponse(id, obj)
                obj.containsKey("event") -> _events.tryEmit(
                    MaEvent(
                        event = obj["event"]!!.jsonPrimitive.content,
                        objectId = obj["object_id"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content,
                        data = obj["data"]?.takeIf { it !is JsonNull },
                    ),
                )
                obj.containsKey("server_version") && !serverInfo.isCompleted ->
                    serverInfo.complete(MaJson.decodeFromJsonElement(ServerInfo.serializer(), obj))
            }
        }

        private fun handleResponse(id: String, obj: JsonObject) {
            val p = pending[id] ?: return
            val code = obj["error_code"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.int
            if (code != null) {
                p.result.completeExceptionally(MaException(code, obj["details"]?.jsonPrimitive?.content ?: "Błąd $code"))
                return
            }
            val result = obj["result"] ?: JsonNull
            val partial = obj["partial"]?.jsonPrimitive?.content == "true"
            val array = result as? JsonArray
            when {
                partial -> array?.let { p.chunks.addAll(it) }
                p.chunks.isNotEmpty() -> p.result.complete(JsonArray(p.chunks + (array ?: emptyList())))
                else -> p.result.complete(result)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
            end(reason.ifBlank { "Zamknięto ($code)" })
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = end(reason.ifBlank { "Zamknięto ($code)" })
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = end(t.message ?: "Błąd połączenia")

        private fun end(reason: String) {
            serverInfo.completeExceptionally(MaException(MaException.LOCAL, reason))
            closed.complete(reason)
        }
    }

    companion object {
        /** `192.168.1.5`, `host:8095`, `http://host:8095/` → `ws://host:8095/ws`. Default port 8095. */
        fun wsUrl(input: String): String = "${normalizeBaseUrl(input).replaceFirst("http", "ws")}/ws"

        fun normalizeBaseUrl(input: String): String {
            var s = input.trim().trimEnd('/')
            val secure = s.startsWith("https://") || s.startsWith("wss://")
            s = s.substringAfter("://")
            if (!s.substringAfterLast(']').contains(':')) s += ":8095"
            return (if (secure) "https://" else "http://") + s
        }
    }
}
