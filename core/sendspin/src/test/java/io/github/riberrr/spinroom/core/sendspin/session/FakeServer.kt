package io.github.riberrr.spinroom.core.sendspin.session

import io.github.riberrr.spinroom.core.sendspin.noise.Base64Url
import io.github.riberrr.spinroom.core.sendspin.noise.KKpsk2Handshake
import io.github.riberrr.spinroom.core.sendspin.noise.NoiseSuite
import io.github.riberrr.spinroom.core.sendspin.noise.NoiseTransport
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.PskCategory
import io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair
import io.github.riberrr.spinroom.core.sendspin.protocol.BinaryFrame
import io.github.riberrr.spinroom.core.sendspin.protocol.BinaryFrames
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientInit
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientTime
import io.github.riberrr.spinroom.core.sendspin.protocol.Envelope
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageCodec
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageType
import io.github.riberrr.spinroom.core.sendspin.protocol.NoiseHandshake
import java.nio.ByteBuffer

/**
 * Minimal Sendspin server (Noise initiator) driving a [SendspinSession] in tests.
 * Frames the session emits land in [inbox] (filled by a collector); [pump] processes them.
 */
class FakeServer(
    private val session: SendspinSession,
    private val inbox: ArrayDeque<OutgoingFrame>,
    val keys: StaticKeyPair = StaticKeyPair.generate(),
) {
    val serverId: String get() = keys.id
    /** Server clock = client clock + offset. */
    var offsetUs = 7_000_000_000L
    var oneWayUs = 1_500L
    var answerTime = true

    var clientKey = ByteArray(0); private set
    var clientInitText = ""; private set
    val received = ArrayList<Envelope>()
    var closedReason: String? = null; private set
    var transport: NoiseTransport? = null; private set
    private var hash = ByteArray(0)
    private var pending: Pair<KKpsk2Handshake, ByteArray>? = null

    fun serverTime(localUs: Long) = localUs + offsetUs

    fun receivedOf(type: String) = received.filter { it.type == type }

    fun lastOf(type: String) = received.last { it.type == type }

    /** Processes everything the client sent so far (including replies that trigger more frames). */
    fun pump() {
        while (inbox.isNotEmpty()) handle(inbox.removeFirst())
    }

    private fun handle(frame: OutgoingFrame) {
        when (frame) {
            is OutgoingFrame.Close -> { closedReason = frame.reason; transport = null }
            is OutgoingFrame.Text -> {
                val env = MessageCodec.decode(frame.text)
                received += env
                when (env.type) {
                    MessageType.CLIENT_INIT -> {
                        clientInitText = frame.text
                        clientKey = Base64Url.decode(env.decode(ClientInit.serializer()).clientId)
                    }
                    MessageType.NOISE_HANDSHAKE -> finishHandshake(env)
                }
            }
            is OutgoingFrame.Binary -> {
                val plain = transport!!.decrypt(frame.bytes)
                val json = BinaryFrames.parse(plain) as BinaryFrame.Json
                val env = MessageCodec.decode(json.text)
                received += env
                when (env.type) {
                    MessageType.NOISE_HANDSHAKE -> finishHandshake(env)
                    MessageType.CLIENT_TIME -> if (answerTime) answerTime(env.decode(ClientTime.serializer()).clientTransmitted)
                }
            }
        }
    }

    private fun answerTime(t1: Long) {
        val t2 = serverTime(t1 + oneWayUs)
        val t3 = t2 + 30
        val t4 = t1 + 2 * oneWayUs + 30
        sendJson(
            MessageType.SERVER_TIME,
            """{"client_transmitted":$t1,"server_received":$t2,"server_transmitted":$t3}""",
            receivedAtUs = t4,
        )
    }

    /** Sends `server/init` + Noise message 1 for [psk] of [category]. Call after the client sent `client/init`. */
    fun startHandshake(category: PskCategory = PskCategory.SENTINEL, psk: ByteArray = Psk.SENTINEL, pskId: String = Psk.id(psk)) {
        pump()
        val init = """{"type":"server/init","payload":{"server_id":"$serverId","version":1}}"""
        session.onText(init)
        val hs = KKpsk2Handshake(true, NoiseSuite.CHACHAPOLY, keys, clientKey, (clientInitText + init).toByteArray())
        val msg1 = hs.writeMessage1("""{"psk_id":"$pskId","psk_category":"${category.wire}"}""".toByteArray())
        pending = hs to psk
        session.onText(MessageCodec.encode(MessageType.NOISE_HANDSHAKE, NoiseHandshake.serializer(), NoiseHandshake(Base64Url.encode(msg1))))
        pump()
    }

    /** In-band re-handshake to [psk]. */
    fun rehandshake(category: PskCategory, psk: ByteArray) {
        val hs = KKpsk2Handshake(true, NoiseSuite.CHACHAPOLY, keys, clientKey, hash)
        val msg1 = hs.writeMessage1("""{"psk_id":"${Psk.id(psk)}","psk_category":"${category.wire}"}""".toByteArray())
        pending = hs to psk
        sendJson(MessageType.NOISE_HANDSHAKE, """{"data":"${Base64Url.encode(msg1)}"}""")
        pump()
    }

    private fun finishHandshake(env: Envelope) {
        val (hs, psk) = pending!!
        val payload = hs.readMessage2(psk, Base64Url.decode(env.decode(NoiseHandshake.serializer()).data))
        check(String(payload) == "{}")
        transport = hs.split()
        hash = hs.handshakeHash
        pending = null
    }

    fun sendJson(type: String, payload: String = "{}", receivedAtUs: Long? = null) {
        for (plain in BinaryFrames.encodeJson("""{"type":"$type","payload":$payload}""")) sendPlain(plain, receivedAtUs)
    }

    fun sendPlain(plain: ByteArray, receivedAtUs: Long? = null) {
        val ct = transport!!.encrypt(plain)
        if (receivedAtUs != null) session.onBinary(ct, receivedAtUs) else session.onBinary(ct)
    }

    fun sendAudio(serverTsUs: Long, data: ByteArray) {
        sendPlain(ByteBuffer.allocate(13 + data.size).put(4).putLong(serverTsUs).putInt(100_000).put(data).array())
    }

    /** Full connect: handshake, hello, activate with [roles]. */
    fun connect(
        category: PskCategory = PskCategory.SENTINEL,
        psk: ByteArray = Psk.SENTINEL,
        activities: String = """["playback"]""",
        roles: String = """["player@v1","metadata@v1","artwork@v1","controller@v1"]""",
    ) {
        session.onOpen()
        startHandshake(category, psk)
        sendJson(MessageType.SERVER_HELLO, """{"name":"Music Assistant"}""")
        pump()
        sendJson(MessageType.SERVER_ACTIVATE, """{"activities":$activities,"active_roles":$roles}""")
        pump()
    }
}
