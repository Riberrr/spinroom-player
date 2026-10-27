package io.github.riberrr.spinroom.core.maapi

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/** Media type segments used in `music/<type>/…` commands. */
object MediaTypes {
    const val ARTISTS = "artists"
    const val ALBUMS = "albums"
    const val TRACKS = "tracks"
    const val PLAYLISTS = "playlists"
    const val RADIOS = "radios"
    const val PODCASTS = "podcasts"
    const val AUDIOBOOKS = "audiobooks"
}

/** `music/<type>/…` library commands. */
class LibraryApi(private val client: MaClient) {

    /** One page; the end is reached when fewer than [limit] items come back. */
    suspend fun items(
        type: String,
        limit: Int = 50,
        offset: Int = 0,
        favorite: Boolean? = null,
        search: String? = null,
        orderBy: String? = null,
    ): List<MediaItem> = client.command(
        "music/$type/library_items",
        args("limit" to limit, "offset" to offset, "favorite" to favorite, "search" to search, "order_by" to orderBy),
    ).decodeList()

    suspend fun count(type: String, favoriteOnly: Boolean = false): Int =
        client.command("music/$type/count", args("favorite_only" to favoriteOnly)).jsonPrimitive.int

    suspend fun album(itemId: String, provider: String): MediaItem = get(MediaTypes.ALBUMS, itemId, provider)
    suspend fun artist(itemId: String, provider: String): MediaItem = get(MediaTypes.ARTISTS, itemId, provider)
    suspend fun playlist(itemId: String, provider: String): MediaItem = get(MediaTypes.PLAYLISTS, itemId, provider)

    private suspend fun get(type: String, itemId: String, provider: String): MediaItem =
        client.command("music/$type/get", args("item_id" to itemId, "provider_instance_id_or_domain" to provider)).decode()

    suspend fun albumTracks(itemId: String, provider: String): List<MediaItem> = client.command(
        "music/albums/album_tracks",
        args("item_id" to itemId, "provider_instance_id_or_domain" to provider),
    ).decodeList()

    suspend fun artistAlbums(itemId: String, provider: String): List<MediaItem> = client.command(
        "music/artists/artist_albums",
        args("item_id" to itemId, "provider_instance_id_or_domain" to provider),
    ).decodeList()

    suspend fun artistTopTracks(itemId: String, provider: String): List<MediaItem> = client.command(
        "music/artists/top_tracks",
        args("item_id" to itemId, "provider_instance_id_or_domain" to provider),
    ).decodeList()

    suspend fun playlistTracks(itemId: String, provider: String): List<MediaItem> = client.command(
        "music/playlists/playlist_tracks",
        args("item_id" to itemId, "provider_instance_id_or_domain" to provider),
        timeoutMs = 60_000,
    ).decodeList()

    suspend fun itemByUri(uri: String): MediaItem = client.command("music/item_by_uri", args("uri" to uri)).decode()

    suspend fun addFavorite(uri: String) {
        client.command("music/favorites/add_item", args("item" to JsonPrimitive(uri)))
    }

    suspend fun removeFavorite(mediaType: String, libraryItemId: String) {
        client.command("music/favorites/remove_item", args("media_type" to mediaType, "library_item_id" to libraryItemId))
    }
}
