# :core:sendspin

Sendspin 1.0 client protocol (spec `main`, Noise `KKpsk2`): handshake, pairing, messages, binary frames, clock sync. Roles: player, metadata, artwork (1 channel), controller. Pure logic, no sockets, no audio; JVM unit tests only. Spec digest: `docs/SENDSPIN.md`.

## Public API (for S03 transport, S04 audio, S05 service/UI)

### `session.SendspinSession`
One instance per app, one WebSocket at a time.
```kotlin
val session = SendspinSession(config, credentials, scope /* lives as long as the service */)
```
**Transport (S03)** — the session never touches sockets:
- server connected → `session.onOpen()` (sends `client/init`)
- each WS text → `session.onText(text)`; each WS binary → `session.onBinary(bytes, receivedAtUs = clock.localNowUs())` (take the time as soon as the message arrives)
- socket closed → `session.onClosed(reason)` (idempotent)
- collect `session.outgoing: Flow<OutgoingFrame>` (single collector) and write `Text`/`Binary` in order; on `Close` close the socket and call `onClosed`.
- `disconnect(GoodbyeReason.SHUTDOWN)` on app exit; `leaveGroup()` sends `client/leave`.
- Only one connection at a time: S03 must reject (or close) a second server while one is up — multi-server arbitration from the spec is not implemented.

**Audio (S04)** — `session` implements the S01a contracts:
- `PlayerStream.events: Flow<PlayerEvent>` — `StreamStarted(format)`, `Chunk(AudioChunk)`, `StreamCleared`, `StreamEnded`, `Command(PlayerCommand)`, in wire order. **Single collector** (Channel-backed, nothing is dropped). `StreamEnded` is also emitted when the connection drops mid-stream.
- `PlayerStateReporter.report(PlayerState)` / `setAvailable(Boolean)` → `client/state` (deduplicated; `available:true` only when audio is available **and** the clock converged).
- `session.clock: SendspinClock` implements `ClockSource` (`serverToLocalUs`, `localToServerUs`, `converged`). Reset on every new connection, kept across re-handshake.

**UI / MediaSession (S05+)** — `SendspinSessionState`:
- `metadata: StateFlow<TrackMetadata?>` (future `timestamp` applied on time), `group: StateFlow<GroupState?>` (group/update + controller), `artwork: StateFlow<ArtworkImage?>` (channel 0, `bytes == null` = cleared).
- `send(ControllerCommand)` / `sendCommand(...): Boolean` — dropped unless listed in `GroupState.supportedCommands`; seek clamped to `seek_max_ms`.
- `state: StateFlow<ConnectionState>` (`Idle`, `Handshaking`, `Connected(serverId, serverName, activities, activeRoles, paired)`, `Closed(reason)`), `notices: SharedFlow<SessionNotice>` (`Paired`, `PairingAborted`, `Unpaired`, `ServerError`).

### Config & credentials
- `SessionConfig(name, deviceInfo, player = PlayerCapabilities(...), artwork = ArtworkChannelSpec(w, h), unpairedAccess = true, ...)` — timeouts and burst timing have spec defaults.
- `CredentialStore` (sync interface): `identity: StaticKeyPair` (client_id = `identity.id`), `pairingPsk`, pairing records (save/remove/touch, ≥ 5, LRU). S05 implements it on DataStore/Keystore and loads it before creating the session; `InMemoryCredentialStore` for tests.
- Pairing token for the settings screen: `PairingToken.encode(identity.publicKey, pairingPsk)` → `SP:0…` (show as text + QR).

### Lower layers (used internally, public for tests/tools)
- `noise.KKpsk2Handshake`, `NoiseTransport`, `Psk` (`SENTINEL`, `id()`), `StaticKeyPair`, `Base64Url` — own `KKpsk2` on noise-java primitives (noise-java has no `psk2` modifier); verified with the cacophony vector.
- `protocol.MessageCodec` (envelope), `Messages.kt` (kotlinx.serialization payloads), `BinaryFrames` + `FragmentAssembler`.
- `clock.TimeFilter` (Kalman filter port of Sendspin/time-filter), `SendspinClock`.

## Behaviour notes
- Bursts of 8 `client/time`; fast bursts (100 ms apart) until converged (≥ 3 samples, σ ≤ 2 ms), then every 5 s.
- Protocol errors (AEAD failure, bad frames, cleartext after transport mode, audio < 13 B, handshake timeout 30 s, no `server/activate` in 30 s) close silently with `OutgoingFrame.Close`.
- `server/activate` admissibility per spec; unpaired playback with `unpairedAccess = false` → goodbye `pairing_required`.
- Pairing PSK flow: `client/pair-init` + `client/pair-finalize` on a pairing activation (only when the matched PSK is the pairing PSK), record saved on `server/pair-finalize`, 2 min attempt timeout.
