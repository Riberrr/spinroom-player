package io.github.riberrr.spinroom.core.maapi

/** Home rows, browse tree and search. */
class BrowseApi(private val client: MaClient) {

    suspend fun recentlyPlayed(limit: Int = 20): List<MediaItem> =
        client.command("music/recently_played_items", args("limit" to limit)).decodeList()

    suspend fun recommendations(): List<RecommendationFolder> =
        client.command("music/recommendations").decodeList()

    /** [path] null = root (list of providers). Folders have [MediaItem.path]; playable items can go to `play_media`. */
    suspend fun browse(path: String? = null): List<MediaItem> =
        client.command("music/browse", args("path" to path)).decodeList()

    suspend fun search(query: String, limit: Int = 10, libraryOnly: Boolean = false): SearchResults =
        client.command(
            "music/search",
            args("search_query" to query, "limit" to limit, "library_only" to libraryOnly),
        ).decode()
}
