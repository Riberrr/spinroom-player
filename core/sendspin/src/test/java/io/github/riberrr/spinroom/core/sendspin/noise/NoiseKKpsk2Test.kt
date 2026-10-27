package io.github.riberrr.spinroom.core.sendspin.noise

import io.github.riberrr.spinroom.core.sendspin.hex
import io.github.riberrr.spinroom.core.sendspin.resource
import io.github.riberrr.spinroom.core.sendspin.toHex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoiseKKpsk2Test {
    private val vector = Json.parseToJsonElement(resource("noise/cacophony-KKpsk2_25519_ChaChaPoly_SHA256.json")).jsonObject
    private fun field(name: String) = hex(vector[name]!!.jsonPrimitive.content)
    private fun psk() = hex(vector["init_psks"]!!.jsonArray[0].jsonPrimitive.content)

    @Test
    fun `matches cacophony vector for both roles`() {
        val init = KKpsk2Handshake(
            initiator = true, suite = NoiseSuite.CHACHAPOLY,
            localStatic = StaticKeyPair(field("init_static")), remoteStatic = field("init_remote_static"),
            prologue = field("init_prologue"), ephemeralPrivate = field("init_ephemeral"),
        )
        val resp = KKpsk2Handshake(
            initiator = false, suite = NoiseSuite.CHACHAPOLY,
            localStatic = StaticKeyPair(field("resp_static")), remoteStatic = field("resp_remote_static"),
            prologue = field("resp_prologue"), ephemeralPrivate = field("resp_ephemeral"),
        )
        val messages = vector["messages"]!!.jsonArray.map {
            hex(it.jsonObject["payload"]!!.jsonPrimitive.content) to it.jsonObject["ciphertext"]!!.jsonPrimitive.content
        }

        val m1 = init.writeMessage1(messages[0].first)
        assertEquals(messages[0].second, m1.toHex())
        assertArrayEquals(messages[0].first, resp.readMessage1(m1))

        val m2 = resp.writeMessage2(psk(), messages[1].first)
        assertEquals(messages[1].second, m2.toHex())
        assertArrayEquals(messages[1].first, init.readMessage2(psk(), m2))

        assertEquals(vector["handshake_hash"]!!.jsonPrimitive.content, init.handshakeHash.toHex())
        assertEquals(init.handshakeHash.toHex(), resp.handshakeHash.toHex())

        val ti = init.split()
        val tr = resp.split()
        messages.drop(2).forEachIndexed { i, (payload, ct) ->
            val (sender, receiver) = if (i % 2 == 0) ti to tr else tr to ti
            val encrypted = sender.encrypt(payload)
            assertEquals(ct, encrypted.toHex())
            assertArrayEquals(payload, receiver.decrypt(encrypted))
        }
    }

    @Test
    fun `wrong psk fails on the initiator`() {
        val server = StaticKeyPair.generate()
        val client = StaticKeyPair.generate()
        val init = KKpsk2Handshake(true, NoiseSuite.CHACHAPOLY, server, client.publicKey, byteArrayOf(1))
        val resp = KKpsk2Handshake(false, NoiseSuite.CHACHAPOLY, client, server.publicKey, byteArrayOf(1))
        resp.readMessage1(init.writeMessage1("{}".toByteArray()))
        val m2 = resp.writeMessage2(Psk.SENTINEL, "{}".toByteArray())
        assertThrows(NoiseException::class.java) { init.readMessage2(Psk.generate(), m2) }
    }

    @Test
    fun `different prologue fails`() {
        val server = StaticKeyPair.generate()
        val client = StaticKeyPair.generate()
        val init = KKpsk2Handshake(true, NoiseSuite.CHACHAPOLY, server, client.publicKey, byteArrayOf(1))
        val resp = KKpsk2Handshake(false, NoiseSuite.CHACHAPOLY, client, server.publicKey, byteArrayOf(2))
        assertThrows(NoiseException::class.java) { resp.readMessage1(init.writeMessage1(byteArrayOf())) }
    }

    @Test
    fun `tampered transport message fails`() {
        val server = StaticKeyPair.generate()
        val client = StaticKeyPair.generate()
        val init = KKpsk2Handshake(true, NoiseSuite.CHACHAPOLY, server, client.publicKey, byteArrayOf())
        val resp = KKpsk2Handshake(false, NoiseSuite.CHACHAPOLY, client, server.publicKey, byteArrayOf())
        resp.readMessage1(init.writeMessage1(byteArrayOf()))
        init.readMessage2(Psk.SENTINEL, resp.writeMessage2(Psk.SENTINEL, byteArrayOf()))
        val ct = init.split().encrypt(byteArrayOf(0, 1, 2))
        ct[0] = (ct[0].toInt() xor 1).toByte()
        assertThrows(NoiseException::class.java) { resp.split().decrypt(ct) }
    }

    @Test
    fun `sentinel psk and its id match the spec`() {
        assertEquals("1b5e24dbc1aed95fc2a5a338a90c05df44bd10f5ec1f4cd66cbf86272767b9d3", Psk.SENTINEL.toHex())
        assertEquals("GFsV9tLaSQm9HcFWpKsgYQOr7wFTvNUtkmFwuVz3zoo", Psk.id(Psk.SENTINEL))
    }

    @Test
    fun `pairing token matches the spec vector`() {
        val key = ByteArray(32) { it.toByte() }
        val psk = ByteArray(32) { (0xe0 + it).toByte() }
        val expected = resource("fixtures/pairing-token-v0.txt").lines().first { it.startsWith("SP:") }.trim()
        assertEquals(expected, PairingToken.encode(key, psk))
    }
}
