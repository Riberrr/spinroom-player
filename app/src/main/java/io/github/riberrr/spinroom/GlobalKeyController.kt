package io.github.riberrr.spinroom

import android.util.Log
import io.github.riberrr.spinroom.core.data.FavoriteStationsRepository
import io.github.riberrr.spinroom.core.data.SettingsStore
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** What the corner card shows right after a global key ran. */
data class CardInfo(
    val kind: Kind,
    /** 1-based position among favorite stations (STATION only). */
    val stationNumber: Int = 0,
    val stationCount: Int = 0,
    val stationName: String = "",
    val imageUrl: String? = null,
) {
    enum class Kind { STATION, NEXT, PREVIOUS, PAUSED, RESUMED }
}

/**
 * Runs the actions of globally assigned keys without the activity (SUI9): MA commands for this TV's player.
 * Station order and the CH+/CH− rule are the same as in the app ([stationIndex], [cycleIndex]).
 */
@Singleton
class GlobalKeyController @Inject constructor(
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
    private val stations: FavoriteStationsRepository,
) {
    /** Returns what happened for the card, or null when nothing could be done. */
    suspend fun run(action: RemoteAction): CardInfo? {
        val playerId = settings.settings.first().playerId
        if (playerId.isBlank()) return null
        return try {
            when (action) {
                RemoteAction.PLAY_PAUSE -> {
                    val playing = queues.queue(playerId).state == "playing"
                    queues.playPause(playerId)
                    CardInfo(if (playing) CardInfo.Kind.PAUSED else CardInfo.Kind.RESUMED)
                }
                RemoteAction.NEXT, RemoteAction.PREVIOUS -> step(playerId, forward = action == RemoteAction.NEXT)
                else -> action.stationNumber?.let { playNumber(playerId, it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "global key: ${action.name} failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun favorites() = stations.favorites.value.ifEmpty { runCatching { stations.refresh() }; stations.favorites.value }

    private suspend fun step(playerId: String, forward: Boolean): CardInfo {
        val media = queues.queue(playerId).currentItem?.mediaItem
        val list = favorites()
        if (media?.mediaType == "radio" && list.isNotEmpty()) {
            val favs = list.map { Favorite(it.uri, it.name, it.imageUrl, true) }
            val index = cycleIndex(stationIndex(favs, media.uri.orEmpty(), media.name), favs.size, forward)
            return play(playerId, index, list.size)
        }
        if (forward) queues.next(playerId) else queues.previous(playerId)
        return CardInfo(if (forward) CardInfo.Kind.NEXT else CardInfo.Kind.PREVIOUS)
    }

    private suspend fun playNumber(playerId: String, n: Int): CardInfo? {
        val list = favorites()
        return if (n in 1..list.size) play(playerId, n - 1, list.size) else null
    }

    private suspend fun play(playerId: String, index: Int, count: Int): CardInfo {
        val s = favorites()[index]
        queues.playMedia(playerId, listOf(s.uri), "play")
        return CardInfo(CardInfo.Kind.STATION, index + 1, count, s.name, s.imageUrl)
    }

    private companion object { const val TAG = "Spinroom" }
}
