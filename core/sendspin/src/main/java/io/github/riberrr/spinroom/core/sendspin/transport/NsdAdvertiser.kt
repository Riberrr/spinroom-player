package io.github.riberrr.spinroom.core.sendspin.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.provider.Settings

/**
 * Announces the player over mDNS as `_sendspin._tcp` with TXT `path` (required) and `name`,
 * so Music Assistant finds it and connects (server-initiated mode).
 */
class NsdAdvertiser(
    context: Context,
    private val log: (String) -> Unit = {},
) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null

    @Synchronized
    fun register(name: String, port: Int = SendspinServer.DEFAULT_PORT, path: String = SendspinServer.DEFAULT_PATH) {
        unregister()
        val info = NsdServiceInfo().apply {
            serviceName = truncateUtf8(name, MAX_NAME_BYTES)
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("path", path)
            setAttribute("name", truncateUtf8(name, MAX_NAME_BYTES))
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) =
                log("mDNS registered: ${info.serviceName} $SERVICE_TYPE port $port")

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) =
                log("mDNS registration failed: $errorCode")

            override fun onServiceUnregistered(info: NsdServiceInfo) = log("mDNS unregistered")

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) =
                log("mDNS unregistration failed: $errorCode")
        }
        listener = l
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
    }

    @Synchronized
    fun unregister() {
        val l = listener ?: return
        listener = null
        runCatching { nsd.unregisterService(l) }
    }

    companion object {
        const val SERVICE_TYPE = "_sendspin._tcp"
        const val MAX_NAME_BYTES = 63

        /** Name the user gave the TV (Settings › Device name), else the model. */
        fun deviceName(context: Context): String {
            val name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            return name?.takeIf { it.isNotBlank() } ?: Build.MODEL ?: "Android TV"
        }
    }
}
