package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.BrowseApi
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.RecommendationFolder

/** Home rows and the sources browse tree. */
class BrowseRepository(private val api: BrowseApi) {
    suspend fun recentlyPlayed(limit: Int = 20): List<MediaItem> = api.recentlyPlayed(limit)
    suspend fun recommendations(): List<RecommendationFolder> = api.recommendations()

    /** [path] null = root (providers); otherwise [MediaItem.path] of a folder. */
    suspend fun browse(path: String? = null): List<MediaItem> = api.browse(path)
}
