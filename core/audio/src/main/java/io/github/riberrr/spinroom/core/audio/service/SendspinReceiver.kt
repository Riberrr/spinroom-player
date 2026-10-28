package io.github.riberrr.spinroom.core.audio.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import io.github.riberrr.spinroom.core.audio.AudioCapabilities
import io.github.riberrr.spinroom.core.audio.AudioEngineState
import io.github.riberrr.spinroom.core.audio.SpinroomAudioEngine
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.session.Activity
import io.github.riberrr.spinroom.core.sendspin.session.ConnectionState
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore
import io.github.riberrr.spinroom.core.sendspin.session.DeviceInfo
import io.github.riberrr.spinroom.core.sendspin.session.SendspinSession
import io.github.riberrr.spinroom.core.sendspin.session.SessionConfig
import io.github.riberrr.spinroom.core.sendspin.transport.NsdAdvertiser
import io.github.riberrr.spinroom.core.sendspin.transport.SendspinNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The TV as a Sendspin player: session + audio engine + network, plus what keeps it alive with the
 * screen off (Wi-Fi lock, partial wake lock while audio plays) and a restart of the server/mDNS
 * when the network comes back. Owned by [SpinroomPlaybackService]; one instance per service.
 *
 * [start] and [stop] block (socket and mDNS setup) — call them off the main thread.
 */
class SendspinReceiver(
    context: Context,
    credentials: CredentialStore,
    deviceInfo: DeviceInfo,
    private val log: (String) -> Unit,
    /** Called on every `server/activate` with this TV's Sendspin client id. */
    private val onConnected: (clientId: String, serverName: String?) -> Unit = { _, _ -> },
) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clientId: String = credentials.identity.id
    val name: String = NsdAdvertiser.deviceName(app)

    val session = SendspinSession(
        SessionConfig(name = name, deviceInfo = deviceInfo, player = AudioCapabilities.probe()),
        credentials,
        scope,
        log = log,
    )
    val engine = SpinroomAudioEngine(session.clock)
    private val network = SendspinNetwork(app, session, scope, log)
    private val restartLock = Mutex()

    val snapshot: StateFlow<PlaybackSnapshot> = combine(
        session.state,
        session.group,
        session.metadata,
        engine.state,
        session.artwork.map { it?.bytes }.distinctUntilChanged { a, b -> a === b },
    ) { conn, group, metadata, audio, artwork ->
        val connected = conn as? ConnectionState.Connected
        PlaybackSnapshot(
            connected = connected != null,
            serverName = connected?.serverName,
            playbackAllowed = connected?.activities?.contains(Activity.PLAYBACK) == true,
            group = group,
            metadata = metadata,
            audio = audio,
            artwork = artwork,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackSnapshot())

    private val wifiLocks: List<WifiManager.WifiLock> = run {
        val wifi = app.getSystemService(WifiManager::class.java) ?: return@run emptyList()
        buildList {
            @Suppress("DEPRECATION") // still effective on the Android 9–13 TVs we target
            add(wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Spinroom:sendspin"))
            if (Build.VERSION.SDK_INT >= 29) add(wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Spinroom:audio"))
        }.onEach { it.setReferenceCounted(false) }
    }
    private val wakeLock: PowerManager.WakeLock? = app.getSystemService(PowerManager::class.java)
        ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Spinroom:audio")
        ?.apply { setReferenceCounted(false) }

    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private var current: Network? = null
        private var lost = false

        override fun onAvailable(network: Network) {
            // The first callback is the network we started on; restart only after a change or loss.
            val changed = (current != null && current != network) || lost
            current = network
            lost = false
            if (changed) restartServer("network changed")
        }

        override fun onLost(network: Network) {
            if (network == current) lost = true
        }
    }

    fun start() {
        scope.launch { session.events.collect(engine::handle) } // the session's single PlayerStream collector
        scope.launch { engine.playerState.collect(session::report) }
        scope.launch { session.state.collect { log("state: $it") } }
        scope.launch { session.notices.collect { log("notice: $it") } }
        scope.launch {
            // Once per connection (Connected is re-emitted when roles/activities change).
            session.state.map { (it as? ConnectionState.Connected)?.let { c -> c.serverId to c.serverName } }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { (_, serverName) -> onConnected(clientId, serverName) }
        }
        scope.launch {
            session.group.map { g -> g?.let { "group: ${it.groupName} playing=${it.playing} commands=${it.supportedCommands.size}" } }
                .distinctUntilChanged()
                .filterNotNull()
                .collect(log)
        }
        scope.launch {
            engine.state.collect { state ->
                val active = state is AudioEngineState.Playing || state is AudioEngineState.Buffering
                if (active) wakeLock?.acquire(WAKE_LOCK_MAX_MS) else wakeLock?.takeIf { it.isHeld }?.release()
            }
        }
        session.setAvailable(true) // the session reports available only once the clock converged
        wifiLocks.forEach { it.acquire() }
        network.startServer(name)
        runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback) }
            .onFailure { log("network callback failed: $it") }
        log("started as \"$name\"")
    }

    fun send(command: ControllerCommand) {
        val sent = session.sendCommand(command)
        log(if (sent) "command: $command" else "command not supported now: $command")
    }

    /** Server clock now (µs), once the clock converged. */
    fun serverNowUs(): Long? {
        val clock = session.clock
        return if (clock.converged.value) clock.localToServerUs(clock.localNowUs()) else null
    }

    /** Stops everything; the receiver cannot be restarted (make a new one). */
    fun stop() {
        runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
        session.disconnect()
        Thread.sleep(GOODBYE_GRACE_MS) // let the goodbye go out
        network.shutdown()
        engine.release()
        scope.cancel()
        wifiLocks.forEach { if (it.isHeld) it.release() }
        wakeLock?.takeIf { it.isHeld }?.release()
        log("stopped")
    }

    private fun restartServer(reason: String) {
        scope.launch(Dispatchers.IO) {
            restartLock.withLock {
                log("restarting server: $reason")
                session.disconnect()
                network.startServer(name) // re-binds the socket and re-registers mDNS on the new network
            }
        }
    }

    private companion object {
        const val GOODBYE_GRACE_MS = 200L

        /** Safety net; re-acquired on every engine state change while audio plays. */
        const val WAKE_LOCK_MAX_MS = 6 * 60 * 60 * 1000L
    }
}
