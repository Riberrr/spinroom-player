package io.github.riberrr.spinroom.core.sendspin.session

import io.github.riberrr.spinroom.core.sendspin.api.ArtworkImage
import io.github.riberrr.spinroom.core.sendspin.api.AudioChunk
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommand
import io.github.riberrr.spinroom.core.sendspin.api.ControllerCommandType
import io.github.riberrr.spinroom.core.sendspin.api.GroupState
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCommand
import io.github.riberrr.spinroom.core.sendspin.api.PlayerCommandType
import io.github.riberrr.spinroom.core.sendspin.api.PlayerEvent
import io.github.riberrr.spinroom.core.sendspin.api.PlayerState
import io.github.riberrr.spinroom.core.sendspin.api.PlayerStateReporter
import io.github.riberrr.spinroom.core.sendspin.api.PlayerStream
import io.github.riberrr.spinroom.core.sendspin.api.RepeatMode
import io.github.riberrr.spinroom.core.sendspin.api.SendspinSessionState
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import io.github.riberrr.spinroom.core.sendspin.api.TrackMetadata
import io.github.riberrr.spinroom.core.sendspin.api.TrackProgress
import io.github.riberrr.spinroom.core.sendspin.clock.SendspinClock
import io.github.riberrr.spinroom.core.sendspin.clock.TimeBurst
import io.github.riberrr.spinroom.core.sendspin.noise.Base64Url
import io.github.riberrr.spinroom.core.sendspin.noise.KKpsk2Handshake
import io.github.riberrr.spinroom.core.sendspin.noise.NoiseException
import io.github.riberrr.spinroom.core.sendspin.noise.NoiseTransport
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.PskCategory
import io.github.riberrr.spinroom.core.sendspin.protocol.ArtworkChannelPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.ArtworkStatePayload
import io.github.riberrr.spinroom.core.sendspin.protocol.AudioFormatPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.BinaryFrame
import io.github.riberrr.spinroom.core.sendspin.protocol.BinaryFrames
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientCommand
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientGoodbye
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientHello
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientInit
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientPairFinalize
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientPairInit
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientState
import io.github.riberrr.spinroom.core.sendspin.protocol.ClientTime
import io.github.riberrr.spinroom.core.sendspin.protocol.ControllerCommandPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.ControllerPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.DeviceInfoPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.Envelope
import io.github.riberrr.spinroom.core.sendspin.protocol.FragmentAssembler
import io.github.riberrr.spinroom.core.sendspin.protocol.GoodbyeReason
import io.github.riberrr.spinroom.core.sendspin.protocol.GroupUpdate
import io.github.riberrr.spinroom.core.sendspin.protocol.HandshakeMessage1Payload
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageCodec
import io.github.riberrr.spinroom.core.sendspin.protocol.MessageType
import io.github.riberrr.spinroom.core.sendspin.protocol.MetadataPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.NoiseHandshake
import io.github.riberrr.spinroom.core.sendspin.protocol.PairAbort
import io.github.riberrr.spinroom.core.sendspin.protocol.PlayerStatePayload
import io.github.riberrr.spinroom.core.sendspin.protocol.PlayerSupportPayload
import io.github.riberrr.spinroom.core.sendspin.protocol.ProtocolException
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerActivate
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerCommand
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerError
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerHello
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerInit
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerState
import io.github.riberrr.spinroom.core.sendspin.protocol.ServerTime
import io.github.riberrr.spinroom.core.sendspin.protocol.StreamClear
import io.github.riberrr.spinroom.core.sendspin.protocol.StreamEnd
import io.github.riberrr.spinroom.core.sendspin.protocol.StreamStart
import io.github.riberrr.spinroom.core.sendspin.protocol.UnpairedAccessPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * Transport-independent Sendspin client (one per app, one WebSocket at a time).
 *
 * The transport calls [onOpen] when a server connects, then [onText]/[onBinary] for every
 * WebSocket message and [onClosed] when it goes away. Everything the transport must send comes out
 * of [outgoing], in order; [OutgoingFrame.Close] means "close the socket".
 *
 * Timers (handshake timeouts, clock bursts, scheduled metadata) run in [scope].
 * All entry points are thread-safe.
 */
class SendspinSession(
    private val config: SessionConfig,
    private val credentials: CredentialStore,
    private val scope: CoroutineScope,
    localNowUs: () -> Long = { System.nanoTime() / 1000 },
    private val log: (String) -> Unit = {},
) : PlayerStream, PlayerStateReporter, SendspinSessionState {

    val clock = SendspinClock(localNowUs)

    private val lock = Any()
    private val outgoingChannel = Channel<OutgoingFrame>(Channel.UNLIMITED)
    private val playerChannel = Channel<PlayerEvent>(Channel.UNLIMITED)

    /** Frames to write to the socket. Single collector (the transport). */
    val outgoing: Flow<OutgoingFrame> = outgoingChannel.receiveAsFlow()

    /** Audio-side events in wire order. Single collector (S04). Never completes. */
    override val events: Flow<PlayerEvent> = playerChannel.receiveAsFlow()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _notices = MutableSharedFlow<SessionNotice>(extraBufferCapacity = 16)
    val notices: SharedFlow<SessionNotice> = _notices.asSharedFlow()

    private val _metadata = MutableStateFlow<TrackMetadata?>(null)
    override val metadata: StateFlow<TrackMetadata?> = _metadata.asStateFlow()
    private val _group = MutableStateFlow<GroupState?>(null)
    override val group: StateFlow<GroupState?> = _group.asStateFlow()
    private val _artwork = MutableStateFlow<ArtworkImage?>(null)
    override val artwork: StateFlow<ArtworkImage?> = _artwork.asStateFlow()

    // ---- connection state (guarded by lock) ----
    private enum class Phase { IDLE, AWAIT_SERVER_INIT, AWAIT_NOISE_1, AWAIT_SERVER_HELLO, AWAIT_ACTIVATE, ACTIVE, CLOSED }

    private var phase = Phase.IDLE
    private var clientInitBytes = ByteArray(0)
    private var serverId = ""
    private var serverKey = ByteArray(0)
    private var serverName: String? = null
    private var transport: NoiseTransport? = null
    private var handshakeHash = ByteArray(0)
    private var matchedCategory = PskCategory.SENTINEL
    private var rehandshaking = false
    private val fragments = FragmentAssembler()
    private var activities = emptySet<Activity>()
    private var activeRoles = emptySet<String>()
    private var activatedOnce = false
    private var pairingIndex = 0
    private var pendingLongTermPsk: ByteArray? = null
    private var stepTimeout: Job? = null
    private var pairingTimeout: Job? = null

    // ---- role state ----
    private var playerState: PlayerState? = null
    private var audioAvailable = true
    private var lastClientState: String? = null
    private var streamFormat: StreamFormat? = null
    private var groupUpdate: GroupUpdate? = null
    private var controllerState: ControllerPayload? = null
    private var metadataJob: Job? = null
    private var artworkJob: Job? = null
    private var artworkTs = 0L
    private var artworkExpected = -1L
    private var artworkBuffer = ByteArrayOutputStream()

    // ---- clock sync ----
    private var burst: TimeBurst? = null
    private var pendingT1 = Long.MIN_VALUE
    private var timeJob: Job? = null

    init {
        scope.launch {
            clock.converged.collect { synchronized(lock) { sendClientState(force = false) } }
        }
    }

    // ================= transport entry points =================

    /** A WebSocket to a server is open: starts the handshake (sends `client/init`). */
    fun onOpen() = synchronized(lock) {
        if (phase != Phase.IDLE && phase != Phase.CLOSED) cleanup("replaced by a new connection", notify = false)
        resetConnection()
        clock.reset()
        val init = MessageCodec.encode(
            MessageType.CLIENT_INIT, ClientInit.serializer(),
            ClientInit(clientId = credentials.identity.id, suite = config.suite.wire),
        )
        clientInitBytes = init.toByteArray(Charsets.UTF_8)
        phase = Phase.AWAIT_SERVER_INIT
        _state.value = ConnectionState.Handshaking
        emit(OutgoingFrame.Text(init))
        armStepTimeout(config.handshakeStepTimeoutMs)
    }

    fun onText(text: String) = synchronized(lock) {
        guarded { handleText(text) }
    }

    fun onBinary(bytes: ByteArray, receivedAtUs: Long = clock.localNowUs()) = synchronized(lock) {
        guarded { handleBinary(bytes, receivedAtUs) }
    }

    /** The socket closed (by either side). Safe to call repeatedly. */
    fun onClosed(reason: String = "connection closed") = synchronized(lock) {
        if (phase == Phase.IDLE || phase == Phase.CLOSED) return@synchronized
        cleanup(reason, notify = false)
    }

    /** Leaves politely with `client/goodbye` (e.g. app shutdown: [GoodbyeReason.SHUTDOWN]). */
    fun disconnect(reason: String = GoodbyeReason.SHUTDOWN) = synchronized(lock) {
        if (phase == Phase.IDLE || phase == Phase.CLOSED) return@synchronized
        close(reason, goodbye = reason)
    }

    /** `client/leave`: leave the current group, stay connected. */
    fun leaveGroup() = synchronized(lock) {
        if (canSendApp()) sendJson(MessageType.CLIENT_LEAVE)
    }

    // ================= PlayerStateReporter =================

    override fun report(state: PlayerState) = synchronized(lock) {
        playerState = state
        sendClientState(force = false)
    }

    override fun setAvailable(available: Boolean) = synchronized(lock) {
        audioAvailable = available
        sendClientState(force = false)
    }

    // ================= SendspinSessionState =================

    override suspend fun send(command: ControllerCommand) {
        sendCommand(command)
    }

    /** Sends a controller command; returns false when the server does not allow it right now. */
    fun sendCommand(command: ControllerCommand): Boolean = synchronized(lock) {
        val controller = controllerState
        if (!canSendApp() || CONTROLLER !in activeRoles || controller == null) return@synchronized false
        val payload = controllerPayload(command, controller) ?: return@synchronized false
        sendJson(MessageType.CLIENT_COMMAND, ClientCommand.serializer(), ClientCommand(payload))
        true
    }

    // ================= text (cleartext handshake) =================

    private fun handleText(text: String) {
        when (phase) {
            Phase.AWAIT_SERVER_INIT -> {
                val env = MessageCodec.decode(text)
                when (env.type) {
                    MessageType.SERVER_ERROR -> {
                        val reason = env.decode(ServerError.serializer()).reason
                        _notices.tryEmit(SessionNotice.ServerError(reason))
                        close("server error: $reason")
                    }
                    MessageType.SERVER_INIT -> {
                        val init = env.decode(ServerInit.serializer())
                        if (init.version != 1) throw ProtocolException("unsupported server version")
                        val key = try { Base64Url.decode(init.serverId) } catch (e: IllegalArgumentException) {
                            throw ProtocolException("bad server_id", e)
                        }
                        if (key.size != 32) throw ProtocolException("bad server_id length")
                        serverId = init.serverId
                        serverKey = key
                        handshakeHash = clientInitBytes + text.toByteArray(Charsets.UTF_8) // prologue
                        phase = Phase.AWAIT_NOISE_1
                        armStepTimeout(config.handshakeStepTimeoutMs)
                    }
                    else -> throw ProtocolException("unexpected ${env.type} before server/init")
                }
            }
            Phase.AWAIT_NOISE_1 -> {
                val env = MessageCodec.decode(text)
                if (env.type != MessageType.NOISE_HANDSHAKE) throw ProtocolException("expected noise/handshake")
                val msg2 = runHandshake(env, prologue = handshakeHash, initial = true)
                emit(OutgoingFrame.Text(msg2))
                phase = Phase.AWAIT_SERVER_HELLO
                armStepTimeout(config.handshakeStepTimeoutMs)
            }
            // Cleartext after switching to transport mode (or before open) is a silent failure.
            else -> throw ProtocolException("unexpected text message in $phase")
        }
    }

    /** Processes Noise message 1, returns the `noise/handshake` JSON carrying message 2, switches keys. */
    private fun runHandshake(env: Envelope, prologue: ByteArray, initial: Boolean): String {
        val data = try { Base64Url.decode(env.decode(NoiseHandshake.serializer()).data) } catch (e: IllegalArgumentException) {
            throw ProtocolException("bad handshake data", e)
        }
        val hs = KKpsk2Handshake(false, config.suite, credentials.identity, serverKey, prologue)
        val payload = hs.readMessage1(data)
        val inner = try {
            MessageCodec.json.decodeFromString(HandshakeMessage1Payload.serializer(), String(payload, Charsets.UTF_8))
        } catch (e: Exception) {
            throw ProtocolException("malformed handshake payload", e)
        }
        val category = PskCategory.fromWire(inner.pskCategory) ?: throw ProtocolException("bad psk_category")
        val (psk, matched) = selectPsk(inner.pskId, category, initial)
        val msg2 = hs.writeMessage2(psk, "{}".toByteArray())
        val out = MessageCodec.encode(MessageType.NOISE_HANDSHAKE, NoiseHandshake.serializer(), NoiseHandshake(Base64Url.encode(msg2)))
        if (initial) {
            transport = hs.split()
        } else {
            sendJsonText(out) // message 2 still goes under the old keys
            transport = hs.split()
        }
        handshakeHash = hs.handshakeHash
        matchedCategory = matched
        pairingIndex = 0
        fragments.reset()
        if (matched == PskCategory.LONG_TERM) credentials.touch(serverId)
        log("handshake done, psk=${matched.wire}")
        return out
    }

    private fun selectPsk(pskId: String, category: PskCategory, initial: Boolean): Pair<ByteArray, PskCategory> {
        when (category) {
            PskCategory.LONG_TERM -> credentials.records().firstOrNull { Psk.id(it.longTermPsk) == pskId }?.let {
                if (it.serverId != serverId) throw ProtocolException("pairing record bound to another server")
                return it.longTermPsk to PskCategory.LONG_TERM
            }
            PskCategory.PAIRING -> if (Psk.id(credentials.pairingPsk) == pskId) {
                return credentials.pairingPsk to PskCategory.PAIRING
            }
            PskCategory.SENTINEL -> if (Psk.id(Psk.SENTINEL) == pskId) return Psk.SENTINEL to PskCategory.SENTINEL
        }
        if (!initial) throw ProtocolException("psk_id miss during re-handshake")
        return Psk.SENTINEL to PskCategory.SENTINEL // Sentinel fallback
    }

    // ================= binary (Noise transport) =================

    private fun handleBinary(bytes: ByteArray, receivedAtUs: Long) {
        val t = transport
        if (t == null || phase == Phase.CLOSED || phase == Phase.IDLE) throw ProtocolException("binary before transport mode")
        if (bytes.isEmpty()) throw ProtocolException("empty message")
        val plain = t.decrypt(bytes)
        when (val frame = BinaryFrames.parse(plain)) {
            is BinaryFrame.Fragment -> fragments.accept(frame)?.let { (type, body) ->
                val whole = ByteArray(body.size + 1).also { it[0] = type.toByte(); body.copyInto(it, 1) }
                handleFrame(BinaryFrames.parseMessage(type, whole, 1), receivedAtUs)
            }
            else -> handleFrame(frame, receivedAtUs)
        }
    }

    private fun handleFrame(frame: BinaryFrame, receivedAtUs: Long) {
        when (frame) {
            is BinaryFrame.Json -> handleJson(MessageCodec.decode(frame.text), receivedAtUs)
            is BinaryFrame.Audio -> onAudio(frame, receivedAtUs)
            is BinaryFrame.ArtworkAnnounce -> if (frame.channel == 0 && ARTWORK in activeRoles) onArtworkAnnounce(frame)
            is BinaryFrame.ArtworkPart -> if (frame.channel == 0 && ARTWORK in activeRoles) onArtworkPart(frame)
            is BinaryFrame.ArtworkCancel -> if (frame.channel == 0) resetArtworkTransfer()
            is BinaryFrame.Fragment, is BinaryFrame.Unknown -> Unit
        }
    }

    private fun handleJson(env: Envelope, receivedAtUs: Long) {
        when (phase) {
            Phase.AWAIT_SERVER_HELLO -> {
                if (env.type != MessageType.SERVER_HELLO) return // ignore anything else until hello
                serverName = env.decode(ServerHello.serializer()).name
                sendJson(MessageType.CLIENT_HELLO, ClientHello.serializer(), clientHello())
                phase = Phase.AWAIT_ACTIVATE
                armStepTimeout(config.activateTimeoutMs)
                return
            }
            Phase.AWAIT_ACTIVATE -> {
                when (env.type) {
                    MessageType.SERVER_ACTIVATE -> onActivate(env.decode(ServerActivate.serializer()))
                    MessageType.NOISE_HANDSHAKE -> onRehandshake(env)
                    MessageType.SERVER_UNPAIR -> onUnpair()
                }
                return
            }
            Phase.ACTIVE -> Unit
            else -> return
        }
        if (rehandshaking && env.type != MessageType.SERVER_ACTIVATE) {
            // After message 1 only the handshake and the new server/activate are allowed.
            if (env.type == MessageType.NOISE_HANDSHAKE) throw ProtocolException("second noise/handshake")
            return
        }
        when (env.type) {
            MessageType.SERVER_TIME -> onServerTime(env.decode(ServerTime.serializer()), receivedAtUs)
            MessageType.SERVER_ACTIVATE -> onActivate(env.decode(ServerActivate.serializer()))
            MessageType.NOISE_HANDSHAKE -> onRehandshake(env)
            MessageType.SERVER_COMMAND -> onServerCommand(env.decode(ServerCommand.serializer()))
            MessageType.STREAM_START -> onStreamStart(env.decode(StreamStart.serializer()))
            MessageType.STREAM_CLEAR -> onStreamClear(env.decode(StreamClear.serializer()))
            MessageType.STREAM_END -> onStreamEnd(env.decode(StreamEnd.serializer()).roles)
            MessageType.GROUP_UPDATE -> {
                groupUpdate = env.decode(GroupUpdate.serializer())
                publishGroup()
            }
            MessageType.SERVER_STATE -> onServerState(env.decode(ServerState.serializer()))
            MessageType.SERVER_UNPAIR -> onUnpair()
            MessageType.SERVER_PAIR_FINALIZE -> onServerPairFinalize()
            MessageType.PAIR_ABORT -> {
                val reason = env.decode(PairAbort.serializer()).reason
                if (pendingLongTermPsk != null || pairingTimeout != null) endPairingAttempt()
                _notices.tryEmit(SessionNotice.PairingAborted(reason))
            }
            MessageType.SERVER_ERROR -> close("server error: ${env.decode(ServerError.serializer()).reason}")
            else -> Unit // unknown types are ignored
        }
    }

    private fun onRehandshake(env: Envelope) {
        rehandshaking = true
        runHandshake(env, prologue = handshakeHash, initial = false)
        stopTimeSync()
        armStepTimeout(config.handshakeStepTimeoutMs)
    }

    // ================= activation & pairing =================

    private fun onActivate(msg: ServerActivate) {
        val acts = msg.activities.mapNotNull { Activity.fromWire(it) }.toSet()
        val explicitRoles = msg.activeRoles?.toSet()
        val capable = allowedActivities(acts + Activity.PLAYBACK, config.unpairedAccess)
        val roles = explicitRoles ?: if (!activatedOnce || !capable) emptySet() else activeRoles

        fun admissible(unpaired: Boolean) = allowedActivities(acts, unpaired) &&
            (roles.isEmpty() || allowedActivities(acts + Activity.PLAYBACK, unpaired))

        if (!admissible(config.unpairedAccess)) {
            val reason = if (matchedCategory != PskCategory.LONG_TERM && !config.unpairedAccess && admissible(true)) {
                GoodbyeReason.PAIRING_REQUIRED
            } else GoodbyeReason.UNAUTHORIZED
            close("activation rejected: $reason", goodbye = reason)
            return
        }

        stepTimeout?.cancel()
        val wasRehandshake = rehandshaking
        rehandshaking = false
        val newRoles = roles intersect supportedRoles()
        applyRoleRemovals(activeRoles - newRoles)
        activities = acts
        activeRoles = newRoles
        activatedOnce = true
        phase = Phase.ACTIVE
        publishState()

        if (Activity.PAIRING in acts) {
            pairingIndex++
            startPairingAttempt(msg.pairing?.method)
        } else if (pendingLongTermPsk != null || pairingTimeout != null) {
            endPairingAttempt() // a server/activate without pairing cancels the attempt, persisting nothing
        }

        if (timeJob == null && burst == null) startBurst()
        if (wasRehandshake) log("re-handshake complete")
        sendClientState(force = true)
    }

    private fun allowedActivities(acts: Set<Activity>, unpaired: Boolean): Boolean = when (matchedCategory) {
        PskCategory.LONG_TERM -> acts.isEmpty() || acts == setOf(Activity.PLAYBACK)
        else -> acts.isEmpty() || acts == setOf(Activity.PAIRING) || (unpaired && Activity.PLAYBACK in acts)
    }

    private fun startPairingAttempt(method: String?) {
        endPairingAttempt()
        if (method != PAIRING_PSK || matchedCategory != PskCategory.PAIRING) {
            sendJson(MessageType.PAIR_ABORT, PairAbort.serializer(), PairAbort("method_not_supported"))
            return
        }
        val ltPsk = Psk.generate()
        pendingLongTermPsk = ltPsk
        sendJson(MessageType.CLIENT_PAIR_INIT, ClientPairInit.serializer(), ClientPairInit(pairingIndex))
        sendJson(MessageType.CLIENT_PAIR_FINALIZE, ClientPairFinalize.serializer(), ClientPairFinalize(Base64Url.encode(ltPsk)))
        pairingTimeout = later(config.pairingAttemptTimeoutMs) {
            if (pendingLongTermPsk === ltPsk) {
                endPairingAttempt()
                if (canSendApp()) sendJson(MessageType.PAIR_ABORT, PairAbort.serializer(), PairAbort("attempt_timeout"))
                _notices.tryEmit(SessionNotice.PairingAborted("attempt_timeout"))
            }
        }
    }

    private fun onServerPairFinalize() {
        val psk = pendingLongTermPsk ?: return
        credentials.save(PairingRecord(serverId, psk))
        endPairingAttempt()
        _notices.tryEmit(SessionNotice.Paired(serverId))
    }

    private fun endPairingAttempt() {
        pendingLongTermPsk = null
        pairingTimeout?.cancel()
        pairingTimeout = null
    }

    private fun onUnpair() {
        credentials.remove(serverId)
        _notices.tryEmit(SessionNotice.Unpaired(serverId))
        close("unpaired by server", goodbye = GoodbyeReason.UNPAIRED)
    }

    private fun applyRoleRemovals(removed: Set<String>) {
        if (PLAYER in removed) endPlayerStream()
        if (METADATA in removed) { metadataJob?.cancel(); _metadata.value = null }
        if (ARTWORK in removed) { artworkJob?.cancel(); resetArtworkTransfer(); _artwork.value = null }
        if (CONTROLLER in removed) { controllerState = null; publishGroup() }
    }

    // ================= player role =================

    private fun onServerCommand(cmd: ServerCommand) {
        val p = cmd.player ?: return
        if (PLAYER !in activeRoles) return
        val supported = (playerState ?: config.defaultPlayerState).supportedCommands
        val command = when (p.command) {
            "volume" -> p.volume?.takeIf { PlayerCommandType.VOLUME in supported }?.let { PlayerCommand.SetVolume(it.coerceIn(0, 100)) }
            "mute" -> p.mute?.takeIf { PlayerCommandType.MUTE in supported }?.let { PlayerCommand.SetMuted(it) }
            "set_output_delay" -> p.outputDelayMs?.takeIf { PlayerCommandType.SET_OUTPUT_DELAY in supported }
                ?.let { PlayerCommand.SetOutputDelay(it.coerceAtLeast(0)) }
            else -> null
        } ?: return
        playerChannel.trySend(PlayerEvent.Command(command))
    }

    private fun onStreamStart(msg: StreamStart) {
        msg.player?.takeIf { PLAYER in activeRoles }?.let { p ->
            val codec = when (p.codec) {
                "pcm" -> Codec.PCM
                "flac" -> Codec.FLAC
                "opus" -> Codec.OPUS
                else -> throw ProtocolException("unknown codec ${p.codec}")
            }
            val header = p.codecHeader?.let {
                try { Base64.getDecoder().decode(it) } catch (e: IllegalArgumentException) { throw ProtocolException("bad codec_header", e) }
            }
            val format = StreamFormat(codec, p.sampleRate, p.channels, p.bitDepth, header)
            streamFormat = format
            playerChannel.trySend(PlayerEvent.StreamStarted(format))
        }
        // artwork: channel config is ours; images follow as binary messages.
    }

    private fun onStreamClear(msg: StreamClear) {
        val roles = msg.roles
        if ((roles == null || "player" in roles) && streamFormat != null) playerChannel.trySend(PlayerEvent.StreamCleared)
        if (roles == null || "artwork" in roles) resetArtworkTransfer()
    }

    private fun onStreamEnd(roles: List<String>?) {
        if (roles == null || "player" in roles) endPlayerStream()
        if (roles == null || "artwork" in roles) {
            resetArtworkTransfer()
            artworkJob?.cancel()
            _artwork.value = null
        }
    }

    private fun endPlayerStream() {
        if (streamFormat != null) {
            streamFormat = null
            playerChannel.trySend(PlayerEvent.StreamEnded)
        }
    }

    private fun onAudio(frame: BinaryFrame.Audio, receivedAtUs: Long) {
        val format = streamFormat ?: return
        if (PLAYER !in activeRoles) return
        playerChannel.trySend(PlayerEvent.Chunk(AudioChunk(frame.timestampUs, frame.sendAheadUs, frame.data, receivedAtUs, format)))
    }

    // ================= metadata / controller / artwork =================

    private fun onServerState(msg: ServerState) {
        msg.metadata?.takeIf { METADATA in activeRoles }?.let { m ->
            val meta = m.toTrackMetadata()
            metadataJob = scheduleAt(m.timestamp, metadataJob) { _metadata.value = meta }
        }
        msg.controller?.takeIf { CONTROLLER in activeRoles }?.let {
            controllerState = it
            publishGroup()
        }
    }

    private fun publishGroup() {
        val g = groupUpdate
        val c = controllerState
        if (g == null && c == null) { _group.value = null; return }
        _group.value = GroupState(
            groupId = g?.groupId.orEmpty(),
            groupName = g?.groupName.orEmpty(),
            playing = g?.playbackState == "playing",
            volume = c?.volume ?: 0,
            muted = c?.muted ?: false,
            repeat = when (c?.repeat) { "one" -> RepeatMode.ONE; "all" -> RepeatMode.ALL; else -> RepeatMode.OFF },
            shuffle = c?.shuffle ?: false,
            supportedCommands = c?.supportedCommands.orEmpty().mapNotNull { CONTROLLER_COMMANDS[it] }.toSet(),
            seekMaxMs = c?.seekMaxMs,
        )
    }

    private fun onArtworkAnnounce(frame: BinaryFrame.ArtworkAnnounce) {
        resetArtworkTransfer()
        if (frame.totalSize == 0L) {
            val ts = frame.timestampUs
            artworkJob = scheduleAt(ts, artworkJob) { _artwork.value = ArtworkImage(ts, null) }
            return
        }
        if (frame.totalSize > MAX_ARTWORK_BYTES) throw ProtocolException("artwork too large")
        artworkTs = frame.timestampUs
        artworkExpected = frame.totalSize
    }

    private fun onArtworkPart(frame: BinaryFrame.ArtworkPart) {
        if (artworkExpected < 0) return
        artworkBuffer.write(frame.data)
        if (artworkBuffer.size() > artworkExpected) throw ProtocolException("artwork larger than announced")
        if (artworkBuffer.size().toLong() == artworkExpected) {
            val image = ArtworkImage(artworkTs, artworkBuffer.toByteArray())
            resetArtworkTransfer()
            artworkJob = scheduleAt(image.timestampUs, artworkJob) { _artwork.value = image }
        }
    }

    private fun resetArtworkTransfer() {
        artworkExpected = -1
        artworkBuffer = ByteArrayOutputStream()
    }

    /** Runs [action] when the server clock reaches [serverUs]; returns the job if it was deferred. */
    private fun scheduleAt(serverUs: Long, previous: Job?, action: () -> Unit): Job? {
        previous?.cancel()
        val delayMs = if (clock.converged.value) (clock.serverToLocalUs(serverUs) - clock.localNowUs()) / 1000 else 0
        if (delayMs <= 0 || delayMs > config.maxScheduleAheadMs) {
            action()
            return null
        }
        return later(delayMs, action)
    }

    /** Runs [block] under the lock after [ms], unless cancelled meanwhile. */
    private fun later(ms: Long, block: () -> Unit): Job = scope.launch {
        delay(ms)
        synchronized(lock) { if (isActive) block() }
    }

    // ================= clock sync =================

    private fun startBurst() {
        timeJob = null
        if (!canSendApp()) return
        burst = TimeBurst(config.timeBurstSize)
        sendTimeRequest()
    }

    private fun sendTimeRequest() {
        val b = burst ?: return
        if (!canSendApp()) { burst = null; return }
        pendingT1 = clock.localNowUs()
        b.onSent()
        sendJson(MessageType.CLIENT_TIME, ClientTime.serializer(), ClientTime(pendingT1))
        val t1 = pendingT1
        timeJob = later(config.timeReplyTimeoutMs) { if (pendingT1 == t1 && burst === b) finishBurst() }
    }

    private fun onServerTime(msg: ServerTime, t4: Long) {
        val b = burst ?: return
        if (msg.clientTransmitted != pendingT1) return
        timeJob?.cancel()
        timeJob = null
        pendingT1 = Long.MIN_VALUE
        b.onReply(msg.clientTransmitted, msg.serverReceived, msg.serverTransmitted, t4)
        if (b.complete) finishBurst() else sendTimeRequest()
    }

    private fun finishBurst() {
        val b = burst ?: return
        burst = null
        pendingT1 = Long.MIN_VALUE
        b.best()?.let { clock.addExchange(it[0], it[1], it[2], it[3]) }
        val gap = if (clock.converged.value) config.timeBurstIntervalMs else config.timeBurstFastIntervalMs
        timeJob?.cancel()
        timeJob = later(gap) { if (burst == null) startBurst() }
    }

    private fun stopTimeSync() {
        timeJob?.cancel()
        timeJob = null
        burst = null
        pendingT1 = Long.MIN_VALUE
    }

    // ================= client/hello & client/state =================

    private fun supportedRoles(): Set<String> = buildSet {
        if (config.player != null) add(PLAYER)
        if (config.metadata) add(METADATA)
        if (config.artwork != null) add(ARTWORK)
        if (config.controller) add(CONTROLLER)
    }

    private fun clientHello() = ClientHello(
        name = config.name,
        deviceInfo = config.deviceInfo?.let { DeviceInfoPayload(it.productName, it.manufacturer, it.softwareVersion, it.macAddress) },
        supportedRoles = listOf(PLAYER, METADATA, ARTWORK, CONTROLLER).filter { it in supportedRoles() },
        playerSupport = config.player?.let { caps ->
            PlayerSupportPayload(caps.supportedFormats.map { it.toPayload() }, caps.bufferCapacityBytes)
        },
        supportedPairMethods = buildJsonObject {
            put(PAIRING_PSK, buildJsonObject { put("locations", JsonArray(listOf(JsonPrimitive("operator")))) })
        },
        unpairedAccess = UnpairedAccessPayload(config.unpairedAccess),
    )

    private fun sendClientState(force: Boolean) {
        if (!canSendApp() || !activatedOnce) return
        val hasPlayer = PLAYER in activeRoles
        val ps = playerState ?: config.defaultPlayerState
        val available = audioAvailable && clock.converged.value
        val payload = ClientState(
            available = if (hasPlayer) available else true,
            player = if (hasPlayer) ps.toPayload() else null,
            artwork = config.artwork?.takeIf { ARTWORK in activeRoles }?.let {
                ArtworkStatePayload(listOf(ArtworkChannelPayload(it.source.wire, it.format.wire, it.width, it.height)))
            },
        )
        val text = MessageCodec.encode(MessageType.CLIENT_STATE, ClientState.serializer(), payload)
        if (!force && text == lastClientState) return
        lastClientState = text
        sendJsonText(text)
    }

    // ================= plumbing =================

    private fun canSendApp() = phase == Phase.ACTIVE && !rehandshaking && transport != null

    private fun <T> sendJson(type: String, serializer: KSerializer<T>, payload: T) =
        sendJsonText(MessageCodec.encode(type, serializer, payload))

    private fun sendJson(type: String) = sendJsonText(MessageCodec.encode(type))

    private fun sendJsonText(text: String) {
        val t = transport ?: return
        for (plain in BinaryFrames.encodeJson(text)) emit(OutgoingFrame.Binary(t.encrypt(plain)))
    }

    private fun emit(frame: OutgoingFrame) {
        outgoingChannel.trySend(frame)
    }

    private fun armStepTimeout(ms: Long) {
        stepTimeout?.cancel()
        val armedPhase = phase
        stepTimeout = later(ms) {
            if (phase == armedPhase || (rehandshaking && phase == Phase.ACTIVE)) close("timeout in $armedPhase")
        }
    }

    private inline fun guarded(block: () -> Unit) {
        if (phase == Phase.IDLE || phase == Phase.CLOSED) return
        try {
            block()
        } catch (e: ProtocolException) {
            close("protocol error: ${e.message}")
        } catch (e: NoiseException) {
            close("noise error: ${e.message}")
        }
    }

    /** Ends the connection: optional goodbye, then [OutgoingFrame.Close]. */
    private fun close(reason: String, goodbye: String? = null) {
        if (phase == Phase.IDLE || phase == Phase.CLOSED) return
        if (goodbye != null && transport != null) {
            sendJson(MessageType.CLIENT_GOODBYE, ClientGoodbye.serializer(), ClientGoodbye(goodbye))
        }
        emit(OutgoingFrame.Close(reason))
        cleanup(reason, notify = true)
    }

    private fun cleanup(reason: String, notify: Boolean) {
        log("connection closed: $reason")
        phase = Phase.CLOSED
        stepTimeout?.cancel()
        stopTimeSync()
        endPairingAttempt()
        endPlayerStream()
        metadataJob?.cancel()
        artworkJob?.cancel()
        _metadata.value = null
        _artwork.value = null
        groupUpdate = null
        controllerState = null
        _group.value = null
        transport = null
        _state.value = ConnectionState.Closed(reason)
    }

    private fun resetConnection() {
        clientInitBytes = ByteArray(0)
        serverId = ""
        serverKey = ByteArray(0)
        serverName = null
        transport = null
        handshakeHash = ByteArray(0)
        matchedCategory = PskCategory.SENTINEL
        rehandshaking = false
        fragments.reset()
        activities = emptySet()
        activeRoles = emptySet()
        activatedOnce = false
        pairingIndex = 0
        lastClientState = null
        streamFormat = null
        resetArtworkTransfer()
    }

    private fun publishState() {
        _state.value = ConnectionState.Connected(
            serverId = serverId,
            serverName = serverName,
            activities = activities,
            activeRoles = activeRoles,
            paired = matchedCategory == PskCategory.LONG_TERM,
        )
    }

    private fun controllerPayload(command: ControllerCommand, c: ControllerPayload): ControllerCommandPayload? {
        val supported = c.supportedCommands.mapNotNull { CONTROLLER_COMMANDS[it] }.toSet()
        val (type, payload) = when (command) {
            ControllerCommand.Play -> ControllerCommandType.PLAY to ControllerCommandPayload("play")
            ControllerCommand.Pause -> ControllerCommandType.PAUSE to ControllerCommandPayload("pause")
            ControllerCommand.Stop -> ControllerCommandType.STOP to ControllerCommandPayload("stop")
            ControllerCommand.Next -> ControllerCommandType.NEXT to ControllerCommandPayload("next")
            ControllerCommand.Previous -> ControllerCommandType.PREVIOUS to ControllerCommandPayload("previous")
            is ControllerCommand.SetGroupVolume ->
                ControllerCommandType.VOLUME to ControllerCommandPayload("volume", volume = command.volume.coerceIn(0, 100))
            is ControllerCommand.SetGroupMuted -> ControllerCommandType.MUTE to ControllerCommandPayload("mute", mute = command.muted)
            is ControllerCommand.SetRepeat -> when (command.mode) {
                RepeatMode.OFF -> ControllerCommandType.REPEAT_OFF to ControllerCommandPayload("repeat_off")
                RepeatMode.ONE -> ControllerCommandType.REPEAT_ONE to ControllerCommandPayload("repeat_one")
                RepeatMode.ALL -> ControllerCommandType.REPEAT_ALL to ControllerCommandPayload("repeat_all")
            }
            is ControllerCommand.SetShuffle ->
                if (command.shuffle) ControllerCommandType.SHUFFLE to ControllerCommandPayload("shuffle")
                else ControllerCommandType.UNSHUFFLE to ControllerCommandPayload("unshuffle")
            is ControllerCommand.Seek -> ControllerCommandType.SEEK to ControllerCommandPayload(
                "seek", positionMs = command.positionMs.coerceIn(0, c.seekMaxMs ?: Long.MAX_VALUE),
            )
            is ControllerCommand.SeekRelative ->
                ControllerCommandType.SEEK_RELATIVE to ControllerCommandPayload("seek_relative", offsetMs = command.offsetMs)
        }
        return payload.takeIf { type in supported }
    }

    companion object {
        const val PLAYER = "player@v1"
        const val METADATA = "metadata@v1"
        const val ARTWORK = "artwork@v1"
        const val CONTROLLER = "controller@v1"
        private const val PAIRING_PSK = "pairing_psk"
        private const val MAX_ARTWORK_BYTES = 8L * 1024 * 1024

        private val CONTROLLER_COMMANDS = mapOf(
            "play" to ControllerCommandType.PLAY, "pause" to ControllerCommandType.PAUSE,
            "stop" to ControllerCommandType.STOP, "next" to ControllerCommandType.NEXT,
            "previous" to ControllerCommandType.PREVIOUS, "volume" to ControllerCommandType.VOLUME,
            "mute" to ControllerCommandType.MUTE, "repeat_off" to ControllerCommandType.REPEAT_OFF,
            "repeat_one" to ControllerCommandType.REPEAT_ONE, "repeat_all" to ControllerCommandType.REPEAT_ALL,
            "shuffle" to ControllerCommandType.SHUFFLE, "unshuffle" to ControllerCommandType.UNSHUFFLE,
            "switch" to ControllerCommandType.SWITCH, "seek" to ControllerCommandType.SEEK,
            "seek_relative" to ControllerCommandType.SEEK_RELATIVE,
        )
    }
}

private fun StreamFormat.toPayload() = AudioFormatPayload(
    codec = codec.name.lowercase(), channels = channels, sampleRate = sampleRate, bitDepth = bitDepth,
)

private fun PlayerState.toPayload() = PlayerStatePayload(
    volume = volume.coerceIn(0, 100),
    muted = muted,
    outputDelayMs = outputDelayMs,
    requiredLeadTimeMs = requiredLeadTimeMs,
    minBufferMs = minBufferMs,
    supportedCommands = supportedCommands.map {
        when (it) {
            PlayerCommandType.VOLUME -> "volume"
            PlayerCommandType.MUTE -> "mute"
            PlayerCommandType.SET_OUTPUT_DELAY -> "set_output_delay"
        }
    },
    format = preferredFormat?.toPayload(),
)

private fun MetadataPayload.toTrackMetadata() = TrackMetadata(
    timestampUs = timestamp,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    artworkUrl = artworkUrl,
    year = year,
    track = track,
    progress = progress?.let { TrackProgress(it.trackProgress, it.trackDuration, it.playbackSpeed) },
)
