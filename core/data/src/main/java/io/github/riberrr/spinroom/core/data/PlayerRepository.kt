package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.MaClient
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.PlayerQueue
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import io.github.riberrr.spinroom.core.maapi.QueueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull

/** Players and queues: snapshots plus live updates from MA events. Transport commands via [api]. */
class PlayerRepository(
    val api: PlayerQueueApi,
    private val client: MaClient,
) {
    /** Emits the queue now and after every `queue_updated` for [queueId]; re-fetches after reconnects. */
    fun queue(queueId: String): Flow<PlayerQueue> = flow {
        // Subscribe to events before the snapshot so updates in between are not lost.
        val live = client.events.filter { it.objectId == queueId && (it.event == "queue_updated" || it.event == "queue_added") }
            .mapNotNull { it.asQueue() }
        runCatching { api.queue(queueId) }.getOrNull()?.let { emit(it) }
        live.collect { emit(it) }
    }

    /** Emits the queue content now and whenever MA says it changed (`queue_items_updated`). */
    fun queueItems(queueId: String, limit: Int = 100): Flow<List<QueueItem>> = flow {
        val changed = client.events.filter { it.objectId == queueId && it.event == "queue_items_updated" }
        runCatching { api.items(queueId, limit) }.getOrNull()?.let { emit(it) }
        changed.collect { emit(api.items(queueId, limit)) }
    }

    /** Current players; refreshed on any player_* event. */
    fun players(): Flow<List<Player>> = flow {
        val changed = client.events.filter { it.event.startsWith("player_") && it.event != "player_settings_updated" }
        runCatching { api.players() }.getOrNull()?.let { emit(it) }
        changed.collect { emit(api.players()) }
    }

    /** The TV's own player, once it has registered with MA (via Sendspin). */
    suspend fun findPlayer(playerId: String): Player? = api.players().firstOrNull { it.playerId == playerId }

    val isConnected: Boolean get() = client.state.value is MaConnectionState.Connected
}
