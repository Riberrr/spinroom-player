package io.github.riberrr.spinroom.core.maapi

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

/** Builds args, skipping null values. Supports String, Int, Long, Double, Boolean, List<String> and JsonElement. */
internal fun args(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((k, v) in pairs) when (v) {
        null -> Unit
        is String -> put(k, v)
        is Int -> put(k, v)
        is Long -> put(k, v)
        is Double -> put(k, v)
        is Boolean -> put(k, v)
        is JsonElement -> put(k, v)
        is List<*> -> put(k, JsonArray(v.map { JsonPrimitive(it.toString()) }))
        else -> error("Unsupported arg type for $k")
    }
}

internal inline fun <reified T> JsonElement.decode(): T = MaJson.decodeFromJsonElement(this)

/** Tolerant list decode: result may be null or contain items we cannot parse; those are skipped. */
internal inline fun <reified T> JsonElement.decodeList(): List<T> =
    if (this is JsonNull) emptyList() else (this as? JsonArray)?.mapNotNull { runCatching { it.decode<T>() }.getOrNull() }.orEmpty()
