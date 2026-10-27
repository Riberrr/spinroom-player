package io.github.riberrr.spinroom.core.maapi

/** player_queues and players commands. For the TV (Sendspin player) the queue id equals the player id. */
class PlayerQueueApi(private val client: MaClient) {

    suspend fun players(): List<Player> = client.command("players/all").decodeList()
    suspend fun queues(): List<PlayerQueue> = client.command("player_queues/all").decodeList()
    suspend fun queue(queueId: String): PlayerQueue = client.command("player_queues/get", args("queue_id" to queueId)).decode()

    suspend fun items(queueId: String, limit: Int = 100, offset: Int = 0): List<QueueItem> =
        client.command("player_queues/items", args("queue_id" to queueId, "limit" to limit, "offset" to offset)).decodeList()

    /** [option]: play | replace | next | replace_next | add. [media] is one uri or a list of uris. */
    suspend fun playMedia(
        queueId: String,
        media: List<String>,
        option: String = "play",
        startItem: String? = null,
        radioMode: Boolean? = null,
        shuffle: Boolean? = null,
    ) {
        val mediaArg: Any = if (media.size == 1) media[0] else media
        client.command(
            "player_queues/play_media",
            args(
                "queue_id" to queueId,
                "media" to mediaArg,
                "option" to option,
                "start_item" to startItem,
                "radio_mode" to radioMode,
                "shuffle" to shuffle,
            ),
        )
    }

    suspend fun playPause(queueId: String) = simple("play_pause", queueId)
    suspend fun play(queueId: String) = simple("play", queueId)
    suspend fun pause(queueId: String) = simple("pause", queueId)
    suspend fun stop(queueId: String) = simple("stop", queueId)
    suspend fun next(queueId: String) = simple("next", queueId)
    suspend fun previous(queueId: String) = simple("previous", queueId)
    suspend fun clear(queueId: String) = simple("clear", queueId)

    suspend fun seek(queueId: String, positionSeconds: Int) {
        client.command("player_queues/seek", args("queue_id" to queueId, "position" to positionSeconds))
    }

    suspend fun playIndex(queueId: String, index: Int) {
        client.command("player_queues/play_index", args("queue_id" to queueId, "index" to index))
    }

    suspend fun shuffle(queueId: String, enabled: Boolean) {
        client.command("player_queues/shuffle", args("queue_id" to queueId, "shuffle_enabled" to enabled))
    }

    /** [mode]: off | one | all. */
    suspend fun repeat(queueId: String, mode: String) {
        client.command("player_queues/repeat", args("queue_id" to queueId, "repeat_mode" to mode))
    }

    suspend fun deleteItem(queueId: String, itemIdOrIndex: String) {
        client.command("player_queues/delete_item", args("queue_id" to queueId, "item_id_or_index" to itemIdOrIndex))
    }

    suspend fun setVolume(playerId: String, level: Int) {
        client.command("players/cmd/volume_set", args("player_id" to playerId, "volume_level" to level.coerceIn(0, 100)))
    }

    private suspend fun simple(cmd: String, queueId: String) {
        client.command("player_queues/$cmd", args("queue_id" to queueId))
    }
}
