package io.github.riberrr.spinroom.core.sendspin.transport

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Server-initiated mode: the TV listens on `ws://0.0.0.0:<port><path>` and Music Assistant connects.
 * Only one connection at a time; a second one is closed with 1013 (try again later).
 * Upgrades carrying an `Origin` header are rejected with 403 (spec: no browser connections).
 */
class SendspinServer(
    private val bridge: SessionBridge,
    private val port: Int = DEFAULT_PORT,
    private val path: String = DEFAULT_PATH,
    private val log: (String) -> Unit = {},
) {
    private var server: EmbeddedServer<*, *>? = null

    @Synchronized
    fun start() {
        if (server != null) return
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 30.seconds
                maxFrameSize = MAX_FRAME_BYTES
            }
            install(
                createApplicationPlugin("RejectOrigin") {
                    onCall { call ->
                        if (call.request.headers[HttpHeaders.Origin] != null) {
                            log("rejected connection with Origin header")
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                },
            )
            routing {
                webSocket(path) { serve(this) }
            }
        }.start(wait = false)
        log("server listening on port ${boundPort()}$path")
    }

    /** Actual port (useful when constructed with port 0); -1 when stopped. */
    fun boundPort(): Int = runBlocking {
        server?.engine?.resolvedConnectors()?.firstOrNull()?.port ?: -1
    }

    @Synchronized
    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1_000)
        server = null
    }

    private suspend fun serve(ws: DefaultWebSocketServerSession) {
        val link = KtorLink(ws)
        val remote = ws.call.request.local.remoteAddress
        if (!bridge.attach(link)) {
            log("rejected second connection from $remote")
            ws.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "busy"))
            return
        }
        log("connected: $remote")
        var reason = "connection closed"
        try {
            for (frame in ws.incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val at = bridge.nowUs()
                        bridge.onBinary(link, frame.readBytes(), at)
                    }
                    is Frame.Text -> bridge.onText(link, frame.readText())
                    else -> Unit
                }
            }
            ws.closeReason.await()?.let { reason = "closed by peer: ${it.code} ${it.message}".trim() }
        } catch (e: CancellationException) {
            reason = "cancelled"
            throw e
        } catch (e: Exception) {
            reason = "error: ${e.message}"
        } finally {
            log("disconnected: $remote ($reason)")
            bridge.detach(link, reason)
        }
    }

    private class KtorLink(private val ws: DefaultWebSocketServerSession) : WsLink {
        override suspend fun sendText(text: String) = ws.send(Frame.Text(text))
        override suspend fun sendBinary(bytes: ByteArray) = ws.send(Frame.Binary(true, bytes))
        override suspend fun close(reason: String) {
            runCatching { ws.close(CloseReason(CloseReason.Codes.NORMAL, reason.closeReasonText())) }
        }
    }

    companion object {
        const val DEFAULT_PORT = 8928
        const val DEFAULT_PATH = "/sendspin"
        const val MAX_FRAME_BYTES = 4L * 1024 * 1024
    }
}

/** WS close reasons are limited to 123 bytes. */
internal fun String.closeReasonText(): String = truncateUtf8(this, 123)

/** Longest prefix of [text] that fits in [maxBytes] UTF-8 bytes without splitting a character. */
fun truncateUtf8(text: String, maxBytes: Int): String {
    if (text.toByteArray(Charsets.UTF_8).size <= maxBytes) return text
    val sb = StringBuilder()
    var used = 0
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val n = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
        if (used + n > maxBytes) break
        sb.appendCodePoint(cp)
        used += n
        i += Character.charCount(cp)
    }
    return sb.toString()
}
