package io.github.riberrr.spinroom.core.sendspin.noise

import com.southernstorm.noise.protocol.Noise
import java.security.MessageDigest
import java.util.Base64

/** base64url without padding, as used for `client_id`, `server_id`, `psk_id` and PSKs on the wire. */
object Base64Url {
    fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getUrlDecoder().decode(text)
}

/** Curve25519 static identity. `id` is the `client_id` (base64url of the public key). */
class StaticKeyPair(privateKey: ByteArray) {
    val privateKey: ByteArray = privateKey.copyOf()
    val publicKey: ByteArray = X25519.publicKey(this.privateKey)
    val id: String get() = Base64Url.encode(publicKey)

    companion object {
        fun generate(): StaticKeyPair = StaticKeyPair(randomBytes(32))
    }
}

enum class PskCategory(val wire: String) {
    LONG_TERM("lt"), PAIRING("pr"), SENTINEL("sn");

    companion object {
        fun fromWire(value: String): PskCategory? = entries.firstOrNull { it.wire == value }
    }
}

object Psk {
    /** `SHA-256("sendspin-sentinel-psk-v1")`, a published constant. */
    val SENTINEL: ByteArray = sha256("sendspin-sentinel-psk-v1".toByteArray())

    /** `base64url(SHA-256("sendspin-psk-id-v1" || psk))`. */
    fun id(psk: ByteArray): String = Base64Url.encode(sha256("sendspin-psk-id-v1".toByteArray() + psk))

    fun generate(): ByteArray = randomBytes(32)
}

/** Version-0 pairing token `SP:0…` shown to the operator (text + QR). */
object PairingToken {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(clientPublicKey: ByteArray, pairingPsk: ByteArray): String {
        require(clientPublicKey.size == 32 && pairingPsk.size == 32)
        return "SP:0" + base32(clientPublicKey + pairingPsk).replace('2', '9')
    }

    /** RFC 4648 base32, no padding. */
    private fun base32(data: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return out.toString()
    }
}

internal object X25519 {
    fun publicKey(privateKey: ByteArray): ByteArray {
        val dh = Noise.createDH("25519")
        dh.setPrivateKey(privateKey, 0)
        return ByteArray(32).also { dh.getPublicKey(it, 0); dh.destroy() }
    }

    fun agree(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val local = Noise.createDH("25519")
        val remote = Noise.createDH("25519")
        local.setPrivateKey(privateKey, 0)
        remote.setPublicKey(publicKey, 0)
        return ByteArray(32).also {
            local.calculate(it, 0, remote)
            local.destroy()
            remote.destroy()
        }
    }
}

internal fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

internal fun randomBytes(size: Int): ByteArray = ByteArray(size).also { Noise.random(it) }
