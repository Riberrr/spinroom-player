package io.github.riberrr.spinroom.core.maapi

/**
 * What Radio Browser (via `music/browse`) can filter on. The server takes **one** dimension at a time
 * (language, country, tag or popularity); combining them would mean filtering 600–1000 stations in the app.
 */
sealed interface RadioFilter {
    /** Browse path for `music/browse`. */
    val path: String

    data class Language(val key: String) : RadioFilter {
        override val path get() = "$ROOT/category/language/${key.lowercase()}"
    }

    data class Country(val key: String) : RadioFilter {
        override val path get() = "$ROOT/category/country/${key.lowercase()}"
    }

    data class Tag(val key: String) : RadioFilter {
        override val path get() = "$ROOT/category/tag/${key.lowercase()}"
    }

    data object Popular : RadioFilter {
        override val path get() = "$ROOT/popularity/popular"
    }

    companion object {
        const val ROOT = "radiobrowser:/"
    }
}

/** One selectable value of a filter chip (the folder's display name and the last path segment). */
data class RadioFilterOption(val name: String, val key: String)

enum class RadioFilterKind(val listPath: String) {
    LANGUAGE("${RadioFilter.ROOT}/category/language"),
    COUNTRY("${RadioFilter.ROOT}/category/country"),
    TAG("${RadioFilter.ROOT}/category/tag"),
}

/** Radio Browser through MA: station lists per filter, filter values, name search. */
class RadioApi(private val client: MaClient, private val browse: BrowseApi = BrowseApi(client)) {

    /** Playable stations for [filter], in the server's order. One response, no paging. */
    suspend fun stations(filter: RadioFilter): List<MediaItem> =
        browse.browse(filter.path).filter { it.mediaType == "radio" }

    /** Values for a chip. The ".." entry is dropped; the server lists the most common ones first (languages) or A–Z. */
    suspend fun options(kind: RadioFilterKind): List<RadioFilterOption> =
        browse.browse(kind.listPath).mapNotNull { folder ->
            val path = folder.path ?: return@mapNotNull null
            if (folder.name == ".." || !path.startsWith(kind.listPath + "/")) return@mapNotNull null
            RadioFilterOption(folder.name, path.removePrefix(kind.listPath + "/"))
        }

    /** Station name search; filters can't be combined with it. */
    suspend fun search(query: String, limit: Int = 30): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return client.command(
            "music/search",
            args("search_query" to query.trim(), "media_types" to listOf("radio"), "limit" to limit, "providers" to listOf("radiobrowser")),
        ).decode<SearchResults>().radio
    }
}
