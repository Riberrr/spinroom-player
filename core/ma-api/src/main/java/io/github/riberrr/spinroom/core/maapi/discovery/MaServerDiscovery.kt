package io.github.riberrr.spinroom.core.maapi.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.ArrayDeque
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** A Music Assistant server seen on the local network. [address] is ready for the address field. */
data class DiscoveredServer(val serverId: String, val name: String, val address: String) {
    companion object {
        /**
         * From an mDNS answer: TXT `base_url` when present (MA pads it with spaces), else host:port.
         * Returns null when there is no usable address.
         */
        fun from(serviceName: String, host: String?, port: Int, txt: Map<String, ByteArray?>): DiscoveredServer? {
            fun attr(key: String) = txt[key]?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotEmpty() }
            val address = attr("base_url")?.trimEnd('/')
                ?: host?.takeIf { it.isNotBlank() && port > 0 }?.let { h -> if (':' in h) "[$h]:$port" else "$h:$port" }
                ?: return null
            return DiscoveredServer(serverId = attr("server_id") ?: address, name = serviceName, address = address)
        }
    }
}

/**
 * Browses `_mass._tcp` with [NsdManager]. Emits the full list of servers found so far; collecting
 * starts the scan, cancelling stops it. Resolves one service at a time (older Android allows one).
 */
class MaServerDiscovery(context: Context) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun servers(): Flow<List<DiscoveredServer>> = callbackFlow {
        val found = LinkedHashMap<String, DiscoveredServer>()
        val queue = ArrayDeque<NsdServiceInfo>()
        var resolving = false

        fun resolveNext() {
            if (resolving) return
            val next = queue.pollFirst() ?: return
            resolving = true
            @Suppress("DEPRECATION")
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = done()
                override fun onServiceResolved(info: NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    val host = info.host?.hostAddress
                    DiscoveredServer.from(info.serviceName, host, info.port, info.attributes)?.let {
                        synchronized(found) { found[it.serverId] = it }
                        trySend(synchronized(found) { found.values.toList() })
                    }
                    done()
                }

                private fun done() = synchronized(queue) { resolving = false; resolveNext() }
            })
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) = synchronized(queue) { queue.addLast(info); resolveNext() }
            override fun onServiceLost(info: NsdServiceInfo) {
                val gone = synchronized(found) { found.entries.removeAll { it.value.name == info.serviceName } }
                if (gone) trySend(synchronized(found) { found.values.toList() })
            }
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        trySend(emptyList())
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }

    companion object {
        const val SERVICE_TYPE = "_mass._tcp."
    }
}
