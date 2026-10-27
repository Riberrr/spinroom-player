package io.github.riberrr.spinroom.core.audio

import android.media.MediaCodecList
import io.github.riberrr.spinroom.core.audio.decode.MediaCodecDecoder
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCapabilities
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat

/**
 * What this device can play, for `client/hello`. Order = priority: FLAC > Opus > PCM (docs/SENDSPIN.md).
 * FLAC is offered only when a platform decoder exists (API 27+); PCM is always there, so the spec's
 * "flac or pcm" requirement holds on API 26 too.
 */
object AudioCapabilities {
    /** Outstanding compressed bytes the server may send ahead (~5 s of 48 kHz PCM16 stereo). */
    const val BUFFER_CAPACITY_BYTES = 1_048_576

    fun probe(): PlayerCapabilities = build(
        hasFlac = hasDecoder(Codec.FLAC),
        hasOpus = hasDecoder(Codec.OPUS),
    )

    /** Pure part of [probe], for tests. */
    fun build(hasFlac: Boolean, hasOpus: Boolean): PlayerCapabilities {
        val formats = buildList {
            if (hasFlac) {
                add(StreamFormat(Codec.FLAC, 48_000, 2, 16))
                add(StreamFormat(Codec.FLAC, 44_100, 2, 16))
            }
            if (hasOpus) add(StreamFormat(Codec.OPUS, 48_000, 2, 16))
            add(StreamFormat(Codec.PCM, 48_000, 2, 16))
            add(StreamFormat(Codec.PCM, 44_100, 2, 16))
        }
        return PlayerCapabilities(formats, BUFFER_CAPACITY_BYTES)
    }

    fun hasDecoder(codec: Codec): Boolean {
        if (codec == Codec.PCM) return true
        val mime = MediaCodecDecoder.mimeFor(codec)
        return MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }
    }
}
