package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** "Odtwórz" / "Losowo" on an album, playlist, artist or track: replaces the queue of the player chosen in settings. */
@Singleton
class PlayItem @Inject constructor(
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
) {
    /** Returns false when no player is chosen yet or the server refused. */
    suspend fun play(uri: String, shuffle: Boolean = false): Boolean {
        val playerId = settings.settings.first().playerId
        if (playerId.isBlank()) return false
        return runCatching { queues.playMedia(playerId, listOf(uri), "replace", shuffle = if (shuffle) true else null) }.isSuccess
    }
}
