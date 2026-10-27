package io.github.riberrr.spinroom.core.sendspin.noise

import com.southernstorm.noise.protocol.CipherState
import com.southernstorm.noise.protocol.Noise
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Noise_KKpsk2 (noise-java ships only the legacy `NoisePSK_` mode, so the pattern is implemented here
 * on top of its X25519 and AEAD primitives). Verified against the cacophony test vector.
 *
 * ```
 * KKpsk2:  -> s   <- s   ...   -> e, es, ss   <- e, ee, se, psk
 * ```
 * Sendspin: the server is the initiator, the client the responder.
 */
enum class NoiseSuite(val wire: String, internal val cipherName: String) {
    CHACHAPOLY("25519_ChaChaPoly_SHA256", "ChaChaPoly"),
    AESGCM("25519_AESGCM_SHA256", "AESGCM");

    val protocolName: String get() = "Noise_KKpsk2_$wire"
}

class NoiseException(message: String, cause: Throwable? = null) : Exception(message, cause)

class KKpsk2Handshake(
    private val initiator: Boolean,
    private val suite: NoiseSuite,
    private val localStatic: StaticKeyPair,
    private val remoteStatic: ByteArray,
    prologue: ByteArray,
    /** Fixed ephemeral private key; tests only. */
    private val ephemeralPrivate: ByteArray? = null,
) {
    private val ss = SymmetricState(suite)
    private var ePriv: ByteArray? = null
    private var re: ByteArray? = null
    private var step = 0

    init {
        ss.mixHash(prologue)
        // Pre-messages: initiator's static first, then responder's.
        if (initiator) {
            ss.mixHash(localStatic.publicKey); ss.mixHash(remoteStatic)
        } else {
            ss.mixHash(remoteStatic); ss.mixHash(localStatic.publicKey)
        }
    }

    /** Initiator: `-> e, es, ss` + payload. */
    fun writeMessage1(payload: ByteArray): ByteArray {
        check(initiator && step == 0)
        val e = newEphemeral()
        val ePub = X25519.publicKey(e)
        ss.mixHash(ePub); ss.mixKey(ePub)
        ss.mixKey(X25519.agree(e, remoteStatic)) // es
        ss.mixKey(X25519.agree(localStatic.privateKey, remoteStatic)) // ss
        step = 1
        return checkSize(ePub + ss.encryptAndHash(payload))
    }

    /** Responder: reads message 1, returns its decrypted payload (readable without the PSK). */
    fun readMessage1(message: ByteArray): ByteArray {
        check(!initiator && step == 0)
        if (message.size < 32 + 16 || message.size > MAX_MESSAGE) throw NoiseException("bad message 1 size")
        val rePub = message.copyOfRange(0, 32)
        re = rePub
        ss.mixHash(rePub); ss.mixKey(rePub)
        ss.mixKey(X25519.agree(localStatic.privateKey, rePub)) // es
        ss.mixKey(X25519.agree(localStatic.privateKey, remoteStatic)) // ss
        step = 1
        return ss.decryptAndHash(message.copyOfRange(32, message.size))
    }

    /** Responder: `<- e, ee, se, psk` + payload. */
    fun writeMessage2(psk: ByteArray, payload: ByteArray): ByteArray {
        check(!initiator && step == 1)
        val e = newEphemeral()
        val ePub = X25519.publicKey(e)
        ss.mixHash(ePub); ss.mixKey(ePub)
        ss.mixKey(X25519.agree(e, re!!)) // ee
        ss.mixKey(X25519.agree(e, remoteStatic)) // se
        ss.mixKeyAndHash(psk)
        step = 2
        return checkSize(ePub + ss.encryptAndHash(payload))
    }

    /** Initiator: reads message 2 using [psk], returns its payload. */
    fun readMessage2(psk: ByteArray, message: ByteArray): ByteArray {
        check(initiator && step == 1)
        if (message.size < 32 + 16 || message.size > MAX_MESSAGE) throw NoiseException("bad message 2 size")
        val rePub = message.copyOfRange(0, 32)
        ss.mixHash(rePub); ss.mixKey(rePub)
        ss.mixKey(X25519.agree(ePriv!!, rePub)) // ee
        ss.mixKey(X25519.agree(localStatic.privateKey, rePub)) // se
        ss.mixKeyAndHash(psk)
        step = 2
        return ss.decryptAndHash(message.copyOfRange(32, message.size))
    }

    /** Handshake hash `h`; after completion it is the prologue of the next re-handshake. */
    val handshakeHash: ByteArray get() = ss.h.copyOf()

    /** Transport keys after message 2. */
    fun split(): NoiseTransport {
        check(step == 2)
        val (c1, c2) = ss.split()
        return if (initiator) NoiseTransport(c1, c2) else NoiseTransport(c2, c1)
    }

    private fun newEphemeral(): ByteArray = (ephemeralPrivate ?: randomBytes(32)).copyOf().also { ePriv = it }

    private fun checkSize(message: ByteArray): ByteArray {
        if (message.size > MAX_MESSAGE) throw NoiseException("handshake message too large")
        return message
    }

    companion object {
        const val MAX_MESSAGE = 65535
        const val TAG = 16
    }
}

/** Noise transport mode: one cipher per direction, empty AD, implicit nonce counter. */
class NoiseTransport internal constructor(private val send: CipherState, private val recv: CipherState) {
    fun encrypt(plaintext: ByteArray): ByteArray {
        if (plaintext.size > KKpsk2Handshake.MAX_MESSAGE - KKpsk2Handshake.TAG) throw NoiseException("plaintext too large")
        val out = ByteArray(plaintext.size + KKpsk2Handshake.TAG)
        send.encryptWithAd(null, plaintext, 0, out, 0, plaintext.size)
        return out
    }

    fun decrypt(ciphertext: ByteArray): ByteArray {
        if (ciphertext.size < KKpsk2Handshake.TAG || ciphertext.size > KKpsk2Handshake.MAX_MESSAGE) {
            throw NoiseException("bad transport message size")
        }
        val out = ByteArray(ciphertext.size - KKpsk2Handshake.TAG)
        try {
            recv.decryptWithAd(null, ciphertext, 0, out, 0, ciphertext.size)
        } catch (e: Exception) {
            throw NoiseException("AEAD failure", e)
        }
        return out
    }
}

internal class SymmetricState(private val suite: NoiseSuite) {
    var h: ByteArray
    private var ck: ByteArray
    private val cipher: CipherState = Noise.createCipher(suite.cipherName)
    private var hasKey = false

    init {
        val name = suite.protocolName.toByteArray()
        h = if (name.size <= 32) name.copyOf(32) else sha256(name)
        ck = h.copyOf()
    }

    fun mixHash(data: ByteArray) {
        h = sha256(h + data)
    }

    fun mixKey(ikm: ByteArray) {
        val out = hkdf(ck, ikm, 2)
        ck = out[0]
        cipher.initializeKey(out[1], 0)
        hasKey = true
    }

    fun mixKeyAndHash(ikm: ByteArray) {
        val out = hkdf(ck, ikm, 3)
        ck = out[0]
        mixHash(out[1])
        cipher.initializeKey(out[2], 0)
        hasKey = true
    }

    fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val ct = if (hasKey) {
            ByteArray(plaintext.size + 16).also { cipher.encryptWithAd(h, plaintext, 0, it, 0, plaintext.size) }
        } else plaintext
        mixHash(ct)
        return ct
    }

    fun decryptAndHash(ciphertext: ByteArray): ByteArray {
        val pt = if (hasKey) {
            if (ciphertext.size < 16) throw NoiseException("short ciphertext")
            try {
                ByteArray(ciphertext.size - 16).also { cipher.decryptWithAd(h, ciphertext, 0, it, 0, ciphertext.size) }
            } catch (e: Exception) {
                throw NoiseException("handshake AEAD failure", e)
            }
        } else ciphertext
        mixHash(ciphertext)
        return pt
    }

    fun split(): Pair<CipherState, CipherState> {
        val out = hkdf(ck, ByteArray(0), 2)
        val c1 = Noise.createCipher(suite.cipherName).apply { initializeKey(out[0], 0) }
        val c2 = Noise.createCipher(suite.cipherName).apply { initializeKey(out[1], 0) }
        cipher.destroy()
        return c1 to c2
    }

    private fun hkdf(chainingKey: ByteArray, ikm: ByteArray, outputs: Int): List<ByteArray> {
        val temp = hmac(chainingKey, ikm)
        val result = ArrayList<ByteArray>(outputs)
        var prev = ByteArray(0)
        for (i in 1..outputs) {
            prev = hmac(temp, prev + byteArrayOf(i.toByte()))
            result += prev
        }
        return result
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(data)
        }
}
