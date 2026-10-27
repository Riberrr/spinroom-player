package io.github.riberrr.spinroom.core.sendspin.transport

import android.content.Context
import io.github.riberrr.spinroom.core.sendspin.session.SendspinSession
import kotlinx.coroutines.CoroutineScope

/**
 * Network side of one [SendspinSession]: either server-initiated (WS server + mDNS, the default)
 * or client-initiated (connect to a typed server address). Exactly one mode runs at a time.
 * The owner (S05 service) decides when to start/stop.
 */
class SendspinNetwork(
    context: Context,
    val session: SendspinSession,
    private val scope: CoroutineScope,
    private val log: (String) -> Unit = {},
) {
    val bridge = SessionBridge(session, scope, log)
    private val advertiser = NsdAdvertiser(context, log)
    private var server: SendspinServer? = null
    private var client: SendspinClient? = null

    /** Listen on [port] and announce `_sendspin._tcp` as [name]. */
    @Synchronized
    fun startServer(name: String, port: Int = SendspinServer.DEFAULT_PORT) {
        stop()
        server = SendspinServer(bridge, port, log = log).also { it.start() }
        advertiser.register(name, port)
    }

    /** Connect to a Sendspin server ourselves; [host] as typed (`10.0.1.50`, `host:8927`, `ws://…`). */
    @Synchronized
    fun startClient(host: String) {
        stop()
        client = SendspinClient(bridge, SendspinClient.urlFor(host), scope, log = log).also { it.start() }
    }

    /** Stops whichever mode runs. Call `session.disconnect()` first to say goodbye. */
    @Synchronized
    fun stop() {
        advertiser.unregister()
        server?.stop()
        server = null
        client?.stop()
        client = null
    }

    /** [stop] plus releasing the outgoing-frame collector; the network can't be restarted afterwards. */
    fun shutdown() {
        stop()
        bridge.shutdown()
    }
}
