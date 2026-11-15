package io.github.riberrr.spinroom.feature.settings

import io.github.riberrr.spinroom.core.maapi.Player

/** One entry of the player list: [player] plus what the UI needs to label it. */
data class PlayerRow(val player: Player, val isOwn: Boolean, val isSelected: Boolean)

/**
 * Rows for the player list. This TV's own player ([ownId]) comes first, then available players by name,
 * then the chosen one if MA reports it unavailable. Other unavailable players are stale entries
 * (old browser tabs, replaced TVs) and are hidden.
 */
internal fun buildPlayerRows(players: List<Player>, ownId: String, selectedId: String): List<PlayerRow> =
    players
        .filter { it.available || it.playerId == ownId || it.playerId == selectedId }
        .map { PlayerRow(it, isOwn = ownId.isNotEmpty() && it.playerId == ownId, isSelected = it.playerId == selectedId) }
        .sortedWith(
            compareBy<PlayerRow>({ !it.isOwn }, { !it.player.available }, { it.player.name.lowercase() }, { it.player.playerId }),
        )
