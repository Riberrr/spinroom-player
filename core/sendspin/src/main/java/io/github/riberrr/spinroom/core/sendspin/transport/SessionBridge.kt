package io.github.riberrr.spinroom.core.sendspin.transport

import io.github.riberrr.spinroom.core.sendspin.session.OutgoingFrame
import io.github.riberrr.spinroom.core.sendspin.session.SendspinSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** One open WebSocket, whatever library carries it. */
interface WsLink {
    suspend fun sendText(text: String)
    suspend fun sendBinary(bytes: ByteArray)

    /** Close the socket; must not throw. */
    suspend fun close(reason: String)
}

/**
 * Connects a [SendspinSession] to at most one [WsLink] at a time.
 *
 * Owns the single collector of [SendspinSession.outgoing] for the whole session lifetime and routes
 * frames to the current link; frames produced while no link is attached are dropped.
 */
class SessionBridge(
    val session: SendspinSession,
    scope: CoroutineScope,
    private val log: (String) -> Unit = {},
) {
    private val lock = Any()
    @Volatile private var current: WsLink? = null

    private val binaryFrames = AtomicLong()
    private val binaryBytes = AtomicLong()

    /** Binary WS messages received since start (all connections). */
    val receivedBinaryFrames: Long get() = binaryFrames.get()
    val receivedBinaryBytes: Long get() = binaryBytes.get()

    val isConnected: Boolean get() = current != null

    private val pump: Job = scope.launch {
        session.outgoing.collect { frame ->
            val link = current ?: return@collect
            try {
                when (frame) {
                    is OutgoingFrame.Text -> link.sendText(frame.text)
                    is OutgoingFrame.Binary -> link.sendBinary(frame.bytes)
                    is OutgoingFrame.Close -> {
                        log("closing connection: ${frame.reason}")
                        detach(link, frame.reason)
                        link.close(frame.reason)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("send failed: ${e.message}")
                detach(link, "send failed")
                link.close("send failed")
            }
        }
    }

    /**
     * Makes [link] the active connection and starts the handshake (`client/init`).
     * Returns false (and does nothing) when another connection is already up — the caller closes [link].
     */
    fun attach(link: WsLink): Boolean = synchronized(lock) {
        if (current != null) return false
        current = link
        session.onOpen()
        true
    }

    fun onText(link: WsLink, text: String) {
        if (current === link) session.onText(text)
    }

    /** [receivedAtUs] must be taken as soon as the message arrived ([nowUs]). */
    fun onBinary(link: WsLink, bytes: ByteArray, receivedAtUs: Long) {
        if (current !== link) return
        binaryFrames.incrementAndGet()
        binaryBytes.addAndGet(bytes.size.toLong())
        session.onBinary(bytes, receivedAtUs)
    }

    /** The socket behind [link] closed. Idempotent; ignored for links that are not current. */
    fun detach(link: WsLink, reason: String) {
        synchronized(lock) {
            if (current !== link) return
            current = null
        }
        session.onClosed(reason)
    }

    fun nowUs(): Long = session.clock.localNowUs()

    /** Stops routing frames; the session itself is left to its owner. */
    fun shutdown() {
        pump.cancel()
    }
}
