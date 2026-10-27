# :core:sendspin

Sendspin 1.0 protocol (roles: player, metadata, artwork, controller). Contracts for S02/S04 live in `api/` (S01a): `ClockSource`, `StreamFormat`, `AudioChunk`, `PlayerStream`, `PlayerStateReporter`, `PlayerCommand`, `TrackMetadata`, `GroupState`, `ControllerCommand`. Implementations come in S02 (protocol) and S04 (audio).
Planned: message models, connection state machine, Noise handshake, clock sync. Pure logic, tested with JVM unit tests on recorded samples. Spec digest: docs/SENDSPIN.md.
