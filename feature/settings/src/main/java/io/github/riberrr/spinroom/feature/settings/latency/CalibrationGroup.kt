package io.github.riberrr.spinroom.feature.settings.latency

import io.github.riberrr.spinroom.core.maapi.Player

/** The MA group the TV plays in: where to send the test signal, and who the other speakers are. */
data class CalibrationGroup(val queueId: String, val otherSpeakers: List<String>)

object CalibrationGroups {
    /**
     * Finds the TV's group among MA [players]. Null when the TV plays alone (nothing to compare with).
     * - TV synced to a leader speaker → leader's queue; others = leader + its members.
     * - TV is a member of a group player → that group's queue; others = its members (the group itself is virtual).
     * - TV leads a sync group → its own queue; others = its members.
     */
    fun find(players: List<Player>, tvId: String): CalibrationGroup? {
        if (tvId.isEmpty()) return null
        val byId = players.associateBy { it.playerId }
        val tv = byId[tvId]
        val (queueId, memberIds) = tv?.syncedTo?.let { byId[it] }?.let { it.playerId to (listOf(it.playerId) + it.groupMembers) }
            ?: players.firstOrNull { it.playerId != tvId && tvId in it.groupMembers }?.let { it.playerId to it.groupMembers }
            ?: tv?.takeIf { it.groupMembers.any { m -> m != tvId } }?.let { it.playerId to it.groupMembers }
            ?: return null
        val names = memberIds.distinct().filter { it != tvId }.map { byId[it]?.name ?: it }
        return CalibrationGroup(queueId, names).takeIf { names.isNotEmpty() }
    }
}
