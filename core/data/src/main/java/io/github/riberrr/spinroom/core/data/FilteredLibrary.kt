package io.github.riberrr.spinroom.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.riberrr.spinroom.core.maapi.MaClient
import io.github.riberrr.spinroom.core.maapi.MaJson
import io.github.riberrr.spinroom.core.maapi.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A music source (provider) offered in the "Źródło" filter. */
data class SourceOption(val domain: String, val name: String)

/** `library_items` with server-side filters (genre, provider, favorite, order), plus the lists the filter panel needs. */
class FilteredLibrary(private val client: MaClient) {

    suspend fun items(
        type: String,
        limit: Int,
        offset: Int,
        filter: LibraryFilter = LibraryFilter(),
        orderBy: String? = "name",
    ): List<MediaItem> = client.command(
        "music/$type/library_items",
        buildJsonObject {
            put("limit", limit)
            put("offset", offset)
            orderBy?.let { put("order_by", it) }
            if (filter.favoriteOnly) put("favorite", true)
            filter.provider?.let { put("provider", it) }
            filter.genreId?.toIntOrNull()?.let { put("genre", it) }
        },
    ).decodeItems()

    /** Paged list of [type]; a decade (client-side) is applied while paging. */
    fun pager(type: String, filter: LibraryFilter, orderBy: String? = "name", pageSize: Int = PAGE_SIZE): Flow<PagingData<MediaItem>> =
        Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) {
            FilteringPagingSource(pageSize, filter::matches) { limit, offset -> items(type, limit, offset, filter, orderBy) }
        }.flow

    suspend fun genres(): List<MediaItem> = items("genres", 200, 0, orderBy = "name")

    /** Music providers known to the server (`providers/manifests`), by domain. */
    suspend fun sources(): List<SourceOption> {
        val arr = client.command("providers/manifests") as? JsonArray ?: return emptyList()
        return arr.mapNotNull { e ->
            val o = runCatching { e.jsonObject }.getOrNull() ?: return@mapNotNull null
            if (o["type"]?.jsonPrimitive?.contentOrNull != "music") return@mapNotNull null
            val domain = o["domain"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            SourceOption(domain, o["name"]?.jsonPrimitive?.contentOrNull ?: domain)
        }.sortedBy { it.name.lowercase() }
    }

    private fun JsonElement.decodeItems(): List<MediaItem> =
        if (this is JsonNull) emptyList()
        else (this as? JsonArray)?.mapNotNull { runCatching { MaJson.decodeFromJsonElement<MediaItem>(it) }.getOrNull() }.orEmpty()

    companion object {
        const val PAGE_SIZE = 50
    }
}
