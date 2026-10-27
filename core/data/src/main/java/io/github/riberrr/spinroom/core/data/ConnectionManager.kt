package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.MaClient
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Keeps the shared [MaClient] connected according to [SettingsStore]. Call [start] once (idempotent). */
class ConnectionManager(
    private val settings: SettingsStore,
    private val client: MaClient,
    private val scope: CoroutineScope,
) {
    private var started = false

    val state: StateFlow<MaConnectionState> get() = client.state

    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            settings.settings.distinctUntilChanged().collectLatest { s ->
                if (s.isConfigured) client.connect(s.serverAddress, s.token) else client.disconnect()
            }
        }
    }
}
