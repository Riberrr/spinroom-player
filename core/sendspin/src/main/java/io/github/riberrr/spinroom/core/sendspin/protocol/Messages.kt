package io.github.riberrr.spinroom.core.sendspin.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Wire payloads (docs/SENDSPIN.md §4). Field names follow the spec; unknown fields are ignored.

@Serializable
data class ClientInit(
    @SerialName("client_id") val clientId: String,
    val version: Int = 1,
    val suite: String,
)

@Serializable
data class ServerInit(@SerialName("server_id") val serverId: String, val version: Int = 1)

@Serializable
data class ServerError(val reason: String? = null)

@Serializable
data class NoiseHandshake(val data: String)

/** Encrypted payload of Noise message 1. */
@Serializable
data class HandshakeMessage1Payload(
    @SerialName("psk_id") val pskId: String,
    @SerialName("psk_category") val pskCategory: String,
)

@Serializable
data class ServerHello(val name: String? = null, val languages: List<String>? = null)

@Serializable
data class DeviceInfoPayload(
    @SerialName("product_name") val productName: String? = null,
    val manufacturer: String? = null,
    @SerialName("software_version") val softwareVersion: String? = null,
    @SerialName("mac_address") val macAddress: String? = null,
)

@Serializable
data class AudioFormatPayload(
    val codec: String,
    val channels: Int,
    @SerialName("sample_rate") val sampleRate: Int,
    @SerialName("bit_depth") val bitDepth: Int,
)

@Serializable
data class PlayerSupportPayload(
    @SerialName("supported_formats") val supportedFormats: List<AudioFormatPayload>,
    @SerialName("buffer_capacity") val bufferCapacity: Int,
)

@Serializable
data class UnpairedAccessPayload(val enabled: Boolean)

@Serializable
data class ClientHello(
    val name: String,
    @SerialName("device_info") val deviceInfo: DeviceInfoPayload? = null,
    @SerialName("supported_roles") val supportedRoles: List<String>,
    @SerialName("player@v1_support") val playerSupport: PlayerSupportPayload? = null,
    @SerialName("supported_pair_methods") val supportedPairMethods: JsonObject,
    @SerialName("unpaired_access") val unpairedAccess: UnpairedAccessPayload,
)

@Serializable
data class PairingParams(val method: String, val format: String? = null)

@Serializable
data class ServerActivate(
    val activities: List<String> = emptyList(),
    @SerialName("active_roles") val activeRoles: List<String>? = null,
    val pairing: PairingParams? = null,
)

@Serializable
data class PlayerStatePayload(
    val volume: Int,
    val muted: Boolean,
    @SerialName("output_delay_ms") val outputDelayMs: Int,
    @SerialName("required_lead_time_ms") val requiredLeadTimeMs: Int,
    @SerialName("min_buffer_ms") val minBufferMs: Int,
    @SerialName("supported_commands") val supportedCommands: List<String>,
    val format: AudioFormatPayload? = null,
)

@Serializable
data class ArtworkChannelPayload(val source: String, val format: String, val width: Int, val height: Int)

@Serializable
data class ArtworkStatePayload(val channels: List<ArtworkChannelPayload>)

@Serializable
data class ClientState(
    val available: Boolean,
    val player: PlayerStatePayload? = null,
    val artwork: ArtworkStatePayload? = null,
)

@Serializable
data class PlayerCommandPayload(
    val command: String,
    val volume: Int? = null,
    val mute: Boolean? = null,
    @SerialName("output_delay_ms") val outputDelayMs: Int? = null,
)

@Serializable
data class ServerCommand(val player: PlayerCommandPayload? = null)

@Serializable
data class StreamPlayerPayload(
    val codec: String,
    @SerialName("sample_rate") val sampleRate: Int,
    val channels: Int,
    @SerialName("bit_depth") val bitDepth: Int = 16,
    @SerialName("codec_header") val codecHeader: String? = null,
)

@Serializable
data class StreamArtworkPayload(val channels: List<ArtworkChannelPayload> = emptyList())

@Serializable
data class StreamStart(
    @SerialName("server_transmitted") val serverTransmitted: Long? = null,
    val player: StreamPlayerPayload? = null,
    val artwork: StreamArtworkPayload? = null,
)

@Serializable
data class StreamClear(
    @SerialName("server_transmitted") val serverTransmitted: Long? = null,
    val roles: List<String>? = null,
)

@Serializable
data class StreamEnd(val roles: List<String>? = null)

@Serializable
data class GroupUpdate(
    @SerialName("playback_state") val playbackState: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
)

@Serializable
data class ProgressPayload(
    @SerialName("track_progress") val trackProgress: Long,
    @SerialName("track_duration") val trackDuration: Long,
    @SerialName("playback_speed") val playbackSpeed: Int,
)

@Serializable
data class MetadataPayload(
    val timestamp: Long,
    val title: String? = null,
    val artist: String? = null,
    @SerialName("album_artist") val albumArtist: String? = null,
    val album: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val year: Int? = null,
    val track: Int? = null,
    val progress: ProgressPayload? = null,
)

@Serializable
data class ControllerPayload(
    @SerialName("supported_commands") val supportedCommands: List<String> = emptyList(),
    val volume: Int = 0,
    val muted: Boolean = false,
    val repeat: String = "off",
    val shuffle: Boolean = false,
    @SerialName("seek_max_ms") val seekMaxMs: Long? = null,
)

@Serializable
data class ServerState(val metadata: MetadataPayload? = null, val controller: ControllerPayload? = null)

@Serializable
data class ControllerCommandPayload(
    val command: String,
    val volume: Int? = null,
    val mute: Boolean? = null,
    @SerialName("position_ms") val positionMs: Long? = null,
    @SerialName("offset_ms") val offsetMs: Long? = null,
)

@Serializable
data class ClientCommand(val controller: ControllerCommandPayload)

@Serializable
data class ClientGoodbye(val reason: String)

@Serializable
data class ClientTime(@SerialName("client_transmitted") val clientTransmitted: Long)

@Serializable
data class ServerTime(
    @SerialName("client_transmitted") val clientTransmitted: Long,
    @SerialName("server_received") val serverReceived: Long,
    @SerialName("server_transmitted") val serverTransmitted: Long,
)

@Serializable
data class ClientPairInit(@SerialName("pairing_index") val pairingIndex: Int)

@Serializable
data class ClientPairFinalize(@SerialName("long_term_psk") val longTermPsk: String)

@Serializable
data class PairAbort(val reason: String)

/** Message type names. */
object MessageType {
    const val CLIENT_INIT = "client/init"
    const val SERVER_INIT = "server/init"
    const val SERVER_ERROR = "server/error"
    const val NOISE_HANDSHAKE = "noise/handshake"
    const val SERVER_HELLO = "server/hello"
    const val CLIENT_HELLO = "client/hello"
    const val SERVER_ACTIVATE = "server/activate"
    const val CLIENT_STATE = "client/state"
    const val SERVER_STATE = "server/state"
    const val SERVER_COMMAND = "server/command"
    const val CLIENT_COMMAND = "client/command"
    const val STREAM_START = "stream/start"
    const val STREAM_CLEAR = "stream/clear"
    const val STREAM_END = "stream/end"
    const val GROUP_UPDATE = "group/update"
    const val CLIENT_TIME = "client/time"
    const val SERVER_TIME = "server/time"
    const val CLIENT_GOODBYE = "client/goodbye"
    const val CLIENT_LEAVE = "client/leave"
    const val SERVER_UNPAIR = "server/unpair"
    const val CLIENT_PAIR_INIT = "client/pair-init"
    const val CLIENT_PAIR_FINALIZE = "client/pair-finalize"
    const val SERVER_PAIR_FINALIZE = "server/pair-finalize"
    const val PAIR_ABORT = "pair/abort"
}

/** `client/goodbye` reasons. */
object GoodbyeReason {
    const val ANOTHER_SERVER = "another_server"
    const val SHUTDOWN = "shutdown"
    const val RESTART = "restart"
    const val USER_REQUEST = "user_request"
    const val UNAUTHORIZED = "unauthorized"
    const val PAIRING_REQUIRED = "pairing_required"
    const val CONCURRENT_ATTEMPT = "concurrent_attempt"
    const val UNPAIRED = "unpaired"
}
