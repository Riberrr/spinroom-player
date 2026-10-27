package io.github.riberrr.spinroom.core.sendspin.session

import io.github.riberrr.spinroom.core.sendspin.api.PlayerCapabilities
import io.github.riberrr.spinroom.core.sendspin.api.PlayerState
import io.github.riberrr.spinroom.core.sendspin.noise.NoiseSuite
import io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair

/** What the transport (S03) must put on the WebSocket, in order. */
sealed interface OutgoingFrame {
    class Text(val text: String) : OutgoingFrame
    class Binary(val bytes: ByteArray) : OutgoingFrame

    /** Close the WebSocket now (after flushing earlier frames). */
    data class Close(val reason: String) : OutgoingFrame
}

data class DeviceInfo(
    val productName: String? = null,
    val manufacturer: String? = null,
    val softwareVersion: String? = null,
    val macAddress: String? = null,
)

enum class ArtworkSource(val wire: String) { ALBUM("album"), ARTIST("artist") }
enum class ArtworkFormat(val wire: String) { JPEG("jpeg"), PNG("png") }

/** The single artwork channel we request (channel 0). */
data class ArtworkChannelSpec(
    val width: Int,
    val height: Int,
    val source: ArtworkSource = ArtworkSource.ALBUM,
    val format: ArtworkFormat = ArtworkFormat.JPEG,
)

data class SessionConfig(
    /** Friendly name shown in MA (≤ 64 bytes UTF-8). */
    val name: String,
    val deviceInfo: DeviceInfo? = null,
    /** Null disables the player role. */
    val player: PlayerCapabilities?,
    /** Sent until S04 calls `report()`. */
    val defaultPlayerState: PlayerState = PlayerState(volume = 50, muted = false, requiredLeadTimeMs = 200, minBufferMs = 500),
    val metadata: Boolean = true,
    val controller: Boolean = true,
    /** Null disables the artwork role. */
    val artwork: ArtworkChannelSpec? = ArtworkChannelSpec(600, 600),
    /** Allow playback without pairing (Sentinel PSK) once the operator approves in MA. */
    val unpairedAccess: Boolean = true,
    val suite: NoiseSuite = NoiseSuite.CHACHAPOLY,
    val handshakeStepTimeoutMs: Long = 30_000,
    val activateTimeoutMs: Long = 30_000,
    val pairingAttemptTimeoutMs: Long = 120_000,
    val timeBurstSize: Int = 8,
    /** Gap between bursts once the clock converged (spec: bursts start ≤ 10 s apart). */
    val timeBurstIntervalMs: Long = 5_000,
    /** Gap between bursts until the clock converged. */
    val timeBurstFastIntervalMs: Long = 100,
    val timeReplyTimeoutMs: Long = 1_000,
    /** Scheduled metadata/artwork further ahead than this is applied immediately. */
    val maxScheduleAheadMs: Long = 60_000,
)

/** Long-term pairing record: PSK shared with one server. */
class PairingRecord(val serverId: String, val longTermPsk: ByteArray)

/**
 * Persistent client secrets. S05 backs it with DataStore/Keystore; calls are synchronous, so load
 * everything into memory before creating the session.
 */
interface CredentialStore {
    /** Static Curve25519 identity; `identity.id` is the `client_id`. Never changes. */
    val identity: StaticKeyPair

    /** Per-device random pairing PSK (32 B), long-lived, used for the `SP:0…` token. */
    val pairingPsk: ByteArray

    fun records(): List<PairingRecord>

    /** Persist, replacing any record for the same server; evict (e.g. LRU) beyond capacity (≥ 5). */
    fun save(record: PairingRecord)

    fun remove(serverId: String)

    /** Called when a record backs a new connection (LRU bookkeeping). */
    fun touch(serverId: String) {}
}

/** In-memory LRU store; for tests and as a cache layer. */
class InMemoryCredentialStore(
    override val identity: StaticKeyPair = StaticKeyPair.generate(),
    override val pairingPsk: ByteArray = io.github.riberrr.spinroom.core.sendspin.noise.Psk.generate(),
    private val capacity: Int = 8,
) : CredentialStore {
    private val list = ArrayList<PairingRecord>() // most recently used last

    @Synchronized override fun records(): List<PairingRecord> = list.toList()

    @Synchronized override fun save(record: PairingRecord) {
        list.removeAll { it.serverId == record.serverId }
        list += record
        while (list.size > capacity) list.removeAt(0)
    }

    @Synchronized override fun remove(serverId: String) {
        list.removeAll { it.serverId == serverId }
    }

    @Synchronized override fun touch(serverId: String) {
        val r = list.firstOrNull { it.serverId == serverId } ?: return
        list.remove(r); list += r
    }
}

enum class Activity(val wire: String) {
    PLAYBACK("playback"), PAIRING("pairing");

    companion object {
        fun fromWire(value: String): Activity? = entries.firstOrNull { it.wire == value }
    }
}

sealed interface ConnectionState {
    /** No connection yet. */
    data object Idle : ConnectionState

    /** client/init … client/hello sent, waiting for `server/activate`. */
    data object Handshaking : ConnectionState

    data class Connected(
        val serverId: String,
        val serverName: String?,
        val activities: Set<Activity>,
        val activeRoles: Set<String>,
        /** True when the session runs on a long-term PSK (paired), false on Sentinel/pairing PSK. */
        val paired: Boolean,
    ) : ConnectionState

    data class Closed(val reason: String) : ConnectionState
}

/** One-off notices for the UI (pairing result etc.). */
sealed interface SessionNotice {
    data class Paired(val serverId: String) : SessionNotice
    data class PairingAborted(val reason: String) : SessionNotice
    data class Unpaired(val serverId: String) : SessionNotice
    data class ServerError(val reason: String?) : SessionNotice
}
