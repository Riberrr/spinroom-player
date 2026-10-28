# :core:sendspin

Sendspin 1.0 client protocol (spec `main`, Noise `KKpsk2`): handshake, pairing, messages, binary frames, clock sync. Roles: player, metadata, artwork (1 channel), controller. Protocol logic is pure (no sockets, no audio); the `transport` package adds the WebSocket server/client and mDNS. JVM unit tests only. Spec digest: `docs/SENDSPIN.md`.

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

### `transport` (S03) — sockets and mDNS
The only Android/network code in the module (Ktor CIO server, OkHttp client, `NsdManager`); the manifest adds `INTERNET`.
- `SendspinNetwork(context, session, scope, log)` — what the S05 service should own. Exactly one mode at a time:
  - `startServer(name, port = 8928)` — server-initiated (default): WS server on `0.0.0.0:8928/sendspin` + mDNS `_sendspin._tcp` with TXT `path=/sendspin`, `name=<name>`.
  - `startClient(host)` — fallback: connect to `ws://<host>:8927/sendspin` (`SendspinClient.urlFor` accepts `10.0.1.50`, `host:port`, IPv6, full `ws://` URL), exponential backoff 1 → 30 s.
  - `stop()` (call `session.disconnect()` first for a goodbye), `shutdown()` (also stops the outgoing collector; not restartable).
  - `bridge.receivedBinaryFrames/receivedBinaryBytes`, `bridge.isConnected` for diagnostics.
- `NsdAdvertiser.deviceName(context)` — `Settings.Global.DEVICE_NAME`, else `Build.MODEL` (default player name).
- Building blocks: `SessionBridge` (owns the single `outgoing` collector for the session's lifetime, one `WsLink` at a time, takes `receivedAtUs` on arrival), `SendspinServer`, `SendspinClient`, `NsdAdvertiser`, `truncateUtf8`.
- Server rules: `Origin` header → 403; a second connection while one is up → closed with 1013 `busy` (no multi-server arbitration); WS ping every 15 s, max frame 4 MiB.

The owner in the app is `SendspinReceiver`, run by `SpinroomPlaybackService` (`:core:audio`, S05).

## Behaviour notes
- Bursts of 8 `client/time`; fast bursts (100 ms apart) until converged (≥ 3 samples, σ ≤ 2 ms), then every 5 s.
- Protocol errors (AEAD failure, bad frames, cleartext after transport mode, audio < 13 B, handshake timeout 30 s, no `server/activate` in 30 s) close silently with `OutgoingFrame.Close`.
- `server/activate` admissibility per spec; unpaired playback with `unpairedAccess = false` → goodbye `pairing_required`.
- Pairing PSK flow: `client/pair-init` + `client/pair-finalize` on a pairing activation (only when the matched PSK is the pairing PSK), record saved on `server/pair-finalize`, 2 min attempt timeout.
