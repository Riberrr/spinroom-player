package io.github.riberrr.spinroom.debug

import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import io.github.riberrr.spinroom.BuildConfig
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCapabilities
import io.github.riberrr.spinroom.core.sendspin.api.PlayerEvent
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import io.github.riberrr.spinroom.core.sendspin.noise.Base64Url
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair
import io.github.riberrr.spinroom.core.sendspin.session.DeviceInfo
import io.github.riberrr.spinroom.core.sendspin.session.InMemoryCredentialStore
import io.github.riberrr.spinroom.core.sendspin.session.SendspinSession
import io.github.riberrr.spinroom.core.sendspin.session.SessionConfig
import io.github.riberrr.spinroom.core.sendspin.transport.NsdAdvertiser
import io.github.riberrr.spinroom.core.sendspin.transport.SendspinNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Temporary debug-build switch (S03) that runs the Sendspin player in-process so MA can find it.
 * S05 replaces this with the foreground service; delete `app/src/debug` then.
 *
 * Toggle (persists across restarts, default on):
 * ```
 * adb shell am broadcast -n io.github.riberrr.spinroom.debug/io.github.riberrr.spinroom.debug.SendspinDebugReceiver -a io.github.riberrr.spinroom.debug.SENDSPIN_STOP
 * adb shell am broadcast -n io.github.riberrr.spinroom.debug/io.github.riberrr.spinroom.debug.SendspinDebugReceiver -a io.github.riberrr.spinroom.debug.SENDSPIN_START [--es server 10.0.1.50]
 * ```
 * With `server` the TV connects to `ws://<server>:8927/sendspin` instead of advertising itself.
 */
object SendspinDebug {
    private const val TAG = "Spinroom"
    private const val PREFS = "sendspin_debug"

    /** Start/stop run one after another here, so a restart never races for port 8928. */
    private val worker = Executors.newSingleThreadExecutor()

    // Touched only on [worker].
    private var scope: CoroutineScope? = null
    private var network: SendspinNetwork? = null

    private fun log(msg: String) {
        Log.i(TAG, "sendspin: $msg")
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setEnabled(context: Context, enabled: Boolean, server: String?) {
        prefs(context).edit().putBoolean("enabled", enabled).putString("server", server).apply()
        if (enabled) start(context) else stop()
    }

    fun startIfEnabled(context: Context) {
        if (prefs(context).getBoolean("enabled", true)) start(context)
    }

    fun start(context: Context) {
        val app = context.applicationContext
        worker.execute {
            doStop()
            runCatching { doStart(app) }.onFailure { Log.e(TAG, "sendspin: start failed", it) }
        }
    }

    fun stop() {
        worker.execute { doStop() }
    }

    private fun doStart(app: Context) {
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val name = NsdAdvertiser.deviceName(app)
        val session = SendspinSession(config(name), credentials(app), s, log = ::log)
        val net = SendspinNetwork(app, session, s, ::log)
        scope = s
        network = net
        session.setAvailable(true) // no audio engine yet (S04), but MA must see the player as ready
        s.launch { session.state.collect { log("state: $it") } }
        s.launch { session.notices.collect { log("notice: $it") } }
        s.launch { session.clock.converged.collect { log("clock converged: $it") } }
        countAudio(s, session, net)
        val server = prefs(app).getString("server", null)
        if (server.isNullOrBlank()) net.startServer(name) else net.startClient(server)
        log("started as \"$name\"" + (server?.let { " -> $it" } ?: ""))
    }

    private fun doStop() {
        val net = network ?: return
        network = null
        net.session.disconnect()
        Thread.sleep(200) // let the goodbye go out
        net.shutdown()
        scope?.cancel()
        scope = null
        log("stopped")
    }

    /** Logs the received-chunk counter every 5 s while it changes (the S03 "done" check). */
    private fun countAudio(scope: CoroutineScope, session: SendspinSession, net: SendspinNetwork) {
        val chunks = AtomicLong()
        val bytes = AtomicLong()
        scope.launch {
            session.events.collect { e ->
                when (e) {
                    is PlayerEvent.Chunk -> {
                        chunks.incrementAndGet()
                        bytes.addAndGet(e.chunk.data.size.toLong())
                    }
                    else -> log("event: $e")
                }
            }
        }
        scope.launch {
            var last = -1L
            while (isActive) {
                delay(5_000)
                val n = chunks.get()
                if (n != last) {
                    log("audio chunks: $n (${bytes.get() / 1024} KiB), ws binary frames: ${net.bridge.receivedBinaryFrames}")
                    last = n
                }
            }
        }
    }

    private fun config(name: String) = SessionConfig(
        name = name,
        deviceInfo = DeviceInfo(
            productName = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            softwareVersion = BuildConfig.VERSION_NAME,
        ),
        player = PlayerCapabilities(
            supportedFormats = listOf(
                StreamFormat(Codec.FLAC, 48_000, 2, 16),
                StreamFormat(Codec.OPUS, 48_000, 2, 16),
                StreamFormat(Codec.PCM, 48_000, 2, 16),
                StreamFormat(Codec.PCM, 44_100, 2, 16),
            ),
            bufferCapacityBytes = 4 * 1024 * 1024,
        ),
    )

    /** Keeps the identity stable across restarts so MA sees the same player (debug only, not secured). */
    private fun credentials(context: Context): InMemoryCredentialStore {
        val p = prefs(context)
        val key = p.getString("identity", null)?.let(Base64Url::decode) ?: StaticKeyPair.generate().privateKey
        val psk = p.getString("pairing_psk", null)?.let(Base64Url::decode) ?: Psk.generate()
        p.edit().putString("identity", Base64Url.encode(key)).putString("pairing_psk", Base64Url.encode(psk)).apply()
        return InMemoryCredentialStore(StaticKeyPair(key), psk)
    }
}

class SendspinDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when {
            action.endsWith(".SENDSPIN_START") -> SendspinDebug.setEnabled(context, true, intent.getStringExtra("server"))
            action.endsWith(".SENDSPIN_STOP") -> SendspinDebug.setEnabled(context, false, null)
        }
    }
}

/** Runs at process start (debug builds only). */
class SendspinDebugStarter : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let(SendspinDebug::startIfEnabled)
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?) = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
