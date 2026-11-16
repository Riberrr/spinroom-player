package io.github.riberrr.spinroom.feature.settings

import io.github.riberrr.spinroom.core.maapi.Player

/**
 * One entry of the player list: [player] plus what the UI needs to label it.
 * [shortId] is set only when another row has the same name, so the user can tell them apart.
 */
data class PlayerRow(val player: Player, val isOwn: Boolean, val isSelected: Boolean, val shortId: String? = null)

/**
 * True when MA's [playerId] is this TV's Sendspin identity [ownId]. MA may use the client id as is or wrap it
 * (prefix/suffix), so an exact match or a containment of the 43-char key both count.
 */
internal fun isOwnPlayerId(playerId: String, ownId: String): Boolean =
    ownId.isNotEmpty() && playerId.isNotEmpty() && (playerId == ownId || playerId.contains(ownId))

/**
 * Rows for the player list. This TV's own player ([ownId]) comes first, then available players by name,
 * then the chosen one if MA reports it unavailable. Other unavailable players are stale entries
 * (old browser tabs, replaced TVs) and are hidden.
 */
internal fun buildPlayerRows(players: List<Player>, ownId: String, selectedId: String): List<PlayerRow> {
    val visible = players.filter { it.available || isOwnPlayerId(it.playerId, ownId) || it.playerId == selectedId }
    val dupNames = visible.groupingBy { it.name.trim().lowercase() }.eachCount().filterValues { it > 1 }.keys
    return visible
        .map {
            PlayerRow(
                it,
                isOwn = isOwnPlayerId(it.playerId, ownId),
                isSelected = it.playerId == selectedId,
                shortId = if (it.name.trim().lowercase() in dupNames) it.playerId.takeLast(SHORT_ID_LEN).ifEmpty { null } else null,
            )
        }
        .sortedWith(
            compareBy<PlayerRow>({ !it.isOwn }, { !it.player.available }, { it.player.name.lowercase() }, { it.player.playerId }),
        )
}

private const val SHORT_ID_LEN = 6
