package io.github.riberrr.spinroom.core.sendspin.protocol

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/** Decrypted binary message (docs/SENDSPIN.md §5). Byte 0 is the message type. */
sealed interface BinaryFrame {
    class Json(val text: String) : BinaryFrame
    class Fragment(val first: Boolean, val last: Boolean, val origType: Int?, val data: ByteArray) : BinaryFrame
    class Audio(val timestampUs: Long, val sendAheadUs: Long, val data: ByteArray) : BinaryFrame
    class ArtworkAnnounce(val channel: Int, val timestampUs: Long, val totalSize: Long) : BinaryFrame
    class ArtworkPart(val channel: Int, val data: ByteArray) : BinaryFrame
    class ArtworkCancel(val channel: Int) : BinaryFrame
    class Unknown(val type: Int) : BinaryFrame
}

object BinaryFrames {
    const val TYPE_JSON = 0
    const val TYPE_FRAGMENT = 1
    const val TYPE_AUDIO = 4
    const val TYPE_ARTWORK_FIRST = 8
    const val TYPE_ARTWORK_LAST = 11

    /** Max plaintext of one Noise transport message (65535 − 16-byte tag), type byte included. */
    const val MAX_PLAINTEXT = 65519

    private const val FLAG_FIRST = 0x02
    private const val FLAG_LAST = 0x01
    private const val ARTWORK_ANNOUNCE = 2
    private const val ARTWORK_CANCEL = 1
    private const val ARTWORK_PART = 0

    fun parse(bytes: ByteArray): BinaryFrame {
        if (bytes.isEmpty()) throw ProtocolException("empty binary message")
        val type = bytes[0].toInt() and 0xFF
        if (type == TYPE_FRAGMENT) return parseFragment(bytes)
        return parseMessage(type, bytes, 1)
    }

    /** Parses a message of [type] whose body starts at [offset] (used for reassembled fragments too). */
    fun parseMessage(type: Int, bytes: ByteArray, offset: Int): BinaryFrame {
        val len = bytes.size - offset
        return when (type) {
            TYPE_JSON -> BinaryFrame.Json(String(bytes, offset, len, Charsets.UTF_8))
            TYPE_FRAGMENT -> throw ProtocolException("nested fragment")
            TYPE_AUDIO -> {
                if (len < 12) throw ProtocolException("audio message shorter than 13 bytes")
                val buf = ByteBuffer.wrap(bytes, offset, len)
                val ts = buf.long
                val ahead = buf.int.toLong() and 0xFFFFFFFFL
                BinaryFrame.Audio(ts, ahead, bytes.copyOfRange(offset + 12, bytes.size))
            }
            in TYPE_ARTWORK_FIRST..TYPE_ARTWORK_LAST -> parseArtwork(type - TYPE_ARTWORK_FIRST, bytes, offset)
            else -> BinaryFrame.Unknown(type)
        }
    }

    private fun parseArtwork(channel: Int, bytes: ByteArray, offset: Int): BinaryFrame {
        if (bytes.size - offset < 1) throw ProtocolException("artwork message without flags")
        return when (bytes[offset].toInt() and 0xFF) {
            ARTWORK_ANNOUNCE -> {
                if (bytes.size - offset < 13) throw ProtocolException("short artwork announce")
                val buf = ByteBuffer.wrap(bytes, offset + 1, 12)
                BinaryFrame.ArtworkAnnounce(channel, buf.long, buf.int.toLong() and 0xFFFFFFFFL)
            }
            ARTWORK_PART -> BinaryFrame.ArtworkPart(channel, bytes.copyOfRange(offset + 1, bytes.size))
            ARTWORK_CANCEL -> BinaryFrame.ArtworkCancel(channel)
            else -> throw ProtocolException("bad artwork flags")
        }
    }

    private fun parseFragment(bytes: ByteArray): BinaryFrame.Fragment {
        if (bytes.size < 2) throw ProtocolException("short fragment")
        val flags = bytes[1].toInt() and 0xFF
        if (flags and 0xFC != 0) throw ProtocolException("reserved fragment flags")
        val first = flags and FLAG_FIRST != 0
        val last = flags and FLAG_LAST != 0
        return if (first) {
            if (bytes.size < 3) throw ProtocolException("short first fragment")
            BinaryFrame.Fragment(true, last, bytes[2].toInt() and 0xFF, bytes.copyOfRange(3, bytes.size))
        } else {
            BinaryFrame.Fragment(false, last, null, bytes.copyOfRange(2, bytes.size))
        }
    }

    /** JSON message as one or more plaintext binary messages (fragmented when over the limit). */
    fun encodeJson(text: String): List<ByteArray> {
        val body = text.toByteArray(Charsets.UTF_8)
        if (body.size + 1 <= MAX_PLAINTEXT) return listOf(byteArrayOf(TYPE_JSON.toByte()) + body)
        val out = ArrayList<ByteArray>()
        var pos = 0
        while (pos < body.size) {
            val first = pos == 0
            val header = if (first) 3 else 2
            val end = minOf(body.size, pos + MAX_PLAINTEXT - header)
            val flags = (if (first) FLAG_FIRST else 0) or (if (end == body.size) FLAG_LAST else 0)
            val head = if (first) byteArrayOf(TYPE_FRAGMENT.toByte(), flags.toByte(), TYPE_JSON.toByte())
            else byteArrayOf(TYPE_FRAGMENT.toByte(), flags.toByte())
            out += head + body.copyOfRange(pos, end)
            pos = end
        }
        return out
    }
}

/** Reassembles fragment messages; one fragmented message at a time. */
class FragmentAssembler(private val maxSize: Int = 16 * 1024 * 1024) {
    private var type: Int? = null
    private var buffer = ByteArrayOutputStream()

    /** Returns the complete message (type + body) after its last fragment, else null. */
    fun accept(fragment: BinaryFrame.Fragment): Pair<Int, ByteArray>? {
        if (fragment.first) {
            if (type != null) throw ProtocolException("new fragmented message before the previous one ended")
            type = fragment.origType
            buffer = ByteArrayOutputStream()
        } else if (type == null) {
            throw ProtocolException("continuation fragment without a first fragment")
        }
        if (buffer.size() + fragment.data.size > maxSize) throw ProtocolException("fragmented message too large")
        buffer.write(fragment.data)
        if (!fragment.last) return null
        val complete = type!! to buffer.toByteArray()
        reset()
        return complete
    }

    fun reset() {
        type = null
        buffer = ByteArrayOutputStream()
    }
}
