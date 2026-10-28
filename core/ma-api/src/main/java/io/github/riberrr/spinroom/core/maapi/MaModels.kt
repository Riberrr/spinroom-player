package io.github.riberrr.spinroom.core.maapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Tolerant parser: MA adds fields between versions and ItemMapping is a cut-down version of full objects. */
val MaJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

@Serializable
data class ServerInfo(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("server_version") val serverVersion: String = "",
    @SerialName("schema_version") val schemaVersion: Int = 0,
    @SerialName("min_supported_schema_version") val minSupportedSchemaVersion: Int = 0,
    @SerialName("base_url") val baseUrl: String = "",
    val name: String? = null,
) {
    /** The server must still speak a schema at least as new as the one this client was written against. */
    val isSupported: Boolean get() = schemaVersion == 0 || schemaVersion >= CLIENT_MIN_SCHEMA

    companion object {
        const val CLIENT_MIN_SCHEMA = 28
    }
}

@Serializable
data class MediaItemImage(
    val type: String = "",
    val path: String = "",
    val provider: String = "",
    @SerialName("remotely_accessible") val remotelyAccessible: Boolean = false,
    @SerialName("proxy_id") val proxyId: String? = null,
)

@Serializable
data class MediaMetadata(
    val images: List<MediaItemImage> = emptyList(),
    val description: String? = null,
)

/**
 * One class for every media type (artist, album, track, playlist, radio, folder…) and for both full objects and
 * `ItemMapping`. Missing fields stay null/empty; use the `…/get` commands for details.
 */
@Serializable
data class MediaItem(
    @SerialName("item_id") val itemId: String = "",
    val provider: String = "",
    @SerialName("media_type") val mediaType: String = "",
    val name: String = "",
    val uri: String? = null,
    val version: String? = null,
    val favorite: Boolean = false,
    val year: Int? = null,
    val duration: Int? = null,
    val metadata: MediaMetadata? = null,
    /** ItemMapping / QueueItem style single image. */
    val image: MediaItemImage? = null,
    val artists: List<MediaItem> = emptyList(),
    val album: MediaItem? = null,
    /** BrowseFolder only. */
    val path: String? = null,
    @SerialName("is_playable") val isPlayable: Boolean? = null,
    @SerialName("sort_name") val sortName: String? = null,
) {
    val artistLine: String get() = artists.joinToString(", ") { it.name }
    val bestImage: MediaItemImage?
        get() = image ?: metadata?.images?.let { imgs -> imgs.firstOrNull { it.type == "thumb" } ?: imgs.firstOrNull() }
}

@Serializable
data class PlayerMedia(
    val uri: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val duration: Int? = null,
    @SerialName("queue_item_id") val queueItemId: String? = null,
)

@Serializable
data class Player(
    @SerialName("player_id") val playerId: String = "",
    val provider: String = "",
    val name: String = "",
    val available: Boolean = true,
    @SerialName("playback_state") val playbackState: String = "unknown",
    @SerialName("elapsed_time") val elapsedTime: Double? = null,
    @SerialName("volume_level") val volumeLevel: Int? = null,
    @SerialName("volume_muted") val volumeMuted: Boolean? = null,
    @SerialName("current_media") val currentMedia: PlayerMedia? = null,
    /** Leader's player id when this player is synced to another one (S14c). */
    @SerialName("synced_to") val syncedTo: String? = null,
    /** Members when this player leads a sync group or is a group player (S14c). */
    @SerialName("group_members") val groupMembers: List<String> = emptyList(),
)

@Serializable
data class QueueItem(
    @SerialName("queue_id") val queueId: String = "",
    @SerialName("queue_item_id") val queueItemId: String = "",
    val name: String = "",
    val duration: Int? = null,
    val index: Int? = null,
    @SerialName("media_item") val mediaItem: MediaItem? = null,
    val image: MediaItemImage? = null,
    val available: Boolean = true,
)

@Serializable
data class PlayerQueue(
    @SerialName("queue_id") val queueId: String = "",
    @SerialName("display_name") val displayName: String = "",
    val active: Boolean = false,
    val available: Boolean = true,
    /** Number of items in the queue (an int in the API, not the list). */
    val items: Int = 0,
    @SerialName("shuffle_enabled") val shuffleEnabled: Boolean = false,
    @SerialName("repeat_mode") val repeatMode: String = "off",
    @SerialName("current_index") val currentIndex: Int? = null,
    @SerialName("elapsed_time") val elapsedTime: Double = 0.0,
    val state: String = "idle",
    @SerialName("current_item") val currentItem: QueueItem? = null,
    @SerialName("next_item") val nextItem: QueueItem? = null,
)

@Serializable
data class SearchResults(
    val artists: List<MediaItem> = emptyList(),
    val albums: List<MediaItem> = emptyList(),
    val tracks: List<MediaItem> = emptyList(),
    val playlists: List<MediaItem> = emptyList(),
    val radio: List<MediaItem> = emptyList(),
    val audiobooks: List<MediaItem> = emptyList(),
    val podcasts: List<MediaItem> = emptyList(),
)

@Serializable
data class RecommendationFolder(
    @SerialName("item_id") val itemId: String = "",
    val provider: String = "",
    val name: String = "",
    @SerialName("translation_key") val translationKey: String? = null,
    val items: List<MediaItem> = emptyList(),
)

@Serializable
data class AuthLoginResult(val success: Boolean = false, val token: String? = null, val error: String? = null)

/** Server push. [data] is raw so consumers decode only what they need (see [asPlayer], [asQueue]). */
data class MaEvent(val event: String, val objectId: String?, val data: JsonElement?) {
    fun asPlayer(): Player? = data?.let { runCatching { MaJson.decodeFromJsonElement(Player.serializer(), it) }.getOrNull() }
    fun asQueue(): PlayerQueue? = data?.let { runCatching { MaJson.decodeFromJsonElement(PlayerQueue.serializer(), it) }.getOrNull() }
}

class MaException(val code: Int, message: String) : Exception(message) {
    companion object {
        const val AUTH_REQUIRED = 20
        const val INVALID_TOKEN = 23
        const val LOCAL = -1
    }
}
