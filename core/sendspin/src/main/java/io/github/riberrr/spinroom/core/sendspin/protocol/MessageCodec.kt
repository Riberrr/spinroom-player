package io.github.riberrr.spinroom.core.sendspin.protocol

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class ProtocolException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Envelope `{"type": ..., "payload": {...}}`. */
class Envelope(val type: String, val payload: JsonObject) {
    fun <T> decode(deserializer: DeserializationStrategy<T>): T = try {
        MessageCodec.json.decodeFromJsonElement(deserializer, payload)
    } catch (e: SerializationException) {
        throw ProtocolException("malformed $type", e)
    } catch (e: IllegalArgumentException) {
        throw ProtocolException("malformed $type", e)
    }
}

object MessageCodec {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun <T> encode(type: String, serializer: KSerializer<T>, payload: T): String =
        encode(type, json.encodeToJsonElement(serializer, payload))

    fun encode(type: String, payload: JsonElement = JsonObject(emptyMap())): String =
        buildJsonObject {
            put("type", JsonPrimitive(type))
            put("payload", payload)
        }.toString()

    fun decode(text: String): Envelope {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw ProtocolException("not a JSON object", e)
        }
        val type = (root["type"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
            ?: throw ProtocolException("missing type")
        val payload = when (val p = root["payload"]) {
            null -> JsonObject(emptyMap())
            is JsonObject -> p
            else -> throw ProtocolException("payload is not an object")
        }
        return Envelope(type, payload)
    }
}
