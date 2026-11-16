package io.github.riberrr.spinroom.feature.settings

import io.github.riberrr.spinroom.core.maapi.Player

/**
 * One entry of the player list: [player] plus what the UI needs to label it.
 * [shortId] is set only when another row has the same name, so the user can tell them apart.
 */
data class PlayerRow(val player: Player, val isOwn: Boolean, val isSelected: Boolean, val shortId: String? = null)

/**
 * True when MA's [playerId] is this TV's Sendspin identity [ownId]. Kept as a fallback: MA 2.10 names Sendspin
 * clients behind a universal player `up<8 hex>` (not derived from the key), but may return the key itself or a
 * wrapped form, so an exact match or a containment of the 43-char key still counts.
 */
internal fun isOwnPlayerId(playerId: String, ownId: String): Boolean =
    ownId.isNotEmpty() && playerId.isNotEmpty() && (playerId == ownId || playerId.contains(ownId))

private fun Player.isChromecast(): Boolean = deviceInfo?.isChromecast == true

/**
 * The player_id MA uses for this TV. First an id match; otherwise, because the real id is an opaque `up…` one,
 * a heuristic: our Sendspin client reports `Build.MODEL` ([ownModel]) and no network identity, whereas the TV's
 * built-in Chromecast (same model) carries a `cast_uuid`/`ip_address`. Ties go to the chosen player, then the
 * available one, then the lowest id. Null when nothing matches.
 */
internal fun resolveOwnPlayerId(players: List<Player>, ownId: String, ownModel: String, selectedId: String): String? {
    players.firstOrNull { isOwnPlayerId(it.playerId, ownId) }?.let { return it.playerId }
    if (ownModel.isBlank()) return null
    val candidates = players.filter {
        val info = it.deviceInfo
        !it.isChromecast() && info?.address.isNullOrBlank() && info?.model?.trim().equals(ownModel.trim(), ignoreCase = true) &&
            it.provider != "sendspin"
    }
    return candidates.sortedWith(compareBy({ it.playerId != selectedId }, { !it.available }, { it.playerId })).firstOrNull()?.playerId
}

/**
 * Rows for the player list. This TV's own player comes first, then available players by name,
 * then the chosen one if MA reports it unavailable. Other unavailable players are stale entries
 * (old browser tabs, replaced TVs) and are hidden.
 */
internal fun buildPlayerRows(players: List<Player>, ownId: String, selectedId: String, ownModel: String = ""): List<PlayerRow> {
    val own = resolveOwnPlayerId(players, ownId, ownModel, selectedId)
    val visible = players.filter { it.available || it.playerId == own || it.playerId == selectedId }
    val dupNames = visible.groupingBy { it.name.trim().lowercase() }.eachCount().filterValues { it > 1 }.keys
    return visible
        .map {
            val dup = it.name.trim().lowercase() in dupNames && !it.isChromecast()
            PlayerRow(
                it,
                isOwn = it.playerId == own,
                isSelected = it.playerId == selectedId,
                shortId = if (dup) it.playerId.takeLast(SHORT_ID_LEN).ifEmpty { null } else null,
            )
        }
        .sortedWith(
            compareBy<PlayerRow>({ !it.isOwn }, { !it.player.available }, { it.player.name.lowercase() }, { it.player.playerId }),
        )
}

private const val SHORT_ID_LEN = 6
