package io.github.riberrr.spinroom.core.sendspin.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

/**
 * Client-initiated fallback: the TV connects to `ws://<server>:8927/sendspin` itself
 * (address typed by the user). Reconnects with exponential backoff until [stop].
 * Don't advertise `_sendspin._tcp` while this mode is on (spec: exactly one mode).
 */
class SendspinClient(
    private val bridge: SessionBridge,
    private val url: String,
    private val scope: CoroutineScope,
    private val http: OkHttpClient = defaultHttp,
    private val minBackoffMs: Long = 1_000,
    private val maxBackoffMs: Long = 30_000,
    private val log: (String) -> Unit = {},
) {
    private var job: Job? = null
    @Volatile private var socket: WebSocket? = null

    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            var backoff = minBackoffMs
            while (true) {
                if (connectOnce()) backoff = minBackoffMs
                log("reconnecting to $url in $backoff ms")
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(maxBackoffMs)
            }
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        socket?.close(1000, "stopped")
        socket = null
    }

    /** Runs one connection until it closes. Returns true if the socket was opened. */
    private suspend fun connectOnce(): Boolean {
        val done = CompletableDeferred<Boolean>()
        val link = OkLink()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                link.ws = webSocket
                if (bridge.attach(link)) {
                    log("connected to $url")
                } else {
                    log("another connection is active, closing $url")
                    webSocket.close(1000, "busy")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) = bridge.onText(link, text)

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val at = bridge.nowUs()
                bridge.onBinary(link, bytes.toByteArray(), at)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                finish("closed by server: $code $reason".trim())

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                finish("failed: ${t.message}")

            private fun finish(reason: String) {
                log("disconnected from $url ($reason)")
                bridge.detach(link, reason)
                done.complete(link.ws != null)
            }
        }
        val ws = http.newWebSocket(Request.Builder().url(url).build(), listener)
        socket = ws
        return try {
            done.await()
        } finally {
            ws.cancel()
            bridge.detach(link, "stopped")
        }
    }

    private class OkLink : WsLink {
        @Volatile var ws: WebSocket? = null

        override suspend fun sendText(text: String) {
            check(ws?.send(text) == true) { "socket closed" }
        }

        override suspend fun sendBinary(bytes: ByteArray) {
            check(ws?.send(bytes.toByteString()) == true) { "socket closed" }
        }

        override suspend fun close(reason: String) {
            ws?.close(1000, reason.closeReasonText())
        }
    }

    companion object {
        const val DEFAULT_PORT = 8927

        /** `ws://<host>:8927/sendspin` from what the user typed: bare host, host:port or a full ws:// URL. */
        fun urlFor(host: String): String {
            val h = host.trim().trimEnd('/')
            if (h.startsWith("ws://") || h.startsWith("wss://")) return h
            val hasPort = if (h.startsWith("[")) h.contains("]:") else h.count { it == ':' } == 1
            val hostPort = when {
                hasPort -> h
                !h.startsWith("[") && h.contains(':') -> "[$h]:$DEFAULT_PORT" // bare IPv6
                else -> "$h:$DEFAULT_PORT"
            }
            return "ws://$hostPort${SendspinServer.DEFAULT_PATH}"
        }

        private val defaultHttp by lazy {
            OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
        }
    }
}
