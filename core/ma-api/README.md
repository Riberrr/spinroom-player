# :core:ma-api

Typed Music Assistant WebSocket client (`ws://<host>:8095/ws`). Pure Kotlin + OkHttp + kotlinx.serialization, JVM-testable with MockWebServer. Commands/fields: `docs/MA-API.md`.

## Public API (package `io.github.riberrr.spinroom.core.maapi`)
- `MaClient(okHttp, scope)` — one socket, `message_id` correlation, 15 s timeout per command, reconnect with backoff (1→30 s).
  - `connect(baseUrl, token?)` / `disconnect()`; `state: StateFlow<MaConnectionState>` (`Connecting`, `Connected(server, authenticated)`, `Reconnecting`, `Failed` = bad token / too-old server, no retry); `events: SharedFlow<MaEvent>`.
  - `command(name, args)` waits for the connection, merges `partial` chunks, throws `MaException(code)` (20 = auth required, 23 = bad token).
  - `MaClient.normalizeBaseUrl("10.0.1.50")` → `http://10.0.1.50:8095`.
- One file per command group — **add new groups as new files**, don't edit others: `LibraryApi` (library_items, count, get, album/artist/playlist tracks, favorites), `BrowseApi` (recently played, recommendations, browse, search), `PlayerQueueApi` (play_media, transport, shuffle/repeat, queue items, volume, players), `AuthApi` (login → long-lived token), `ImageUrls` (imageproxy URL).
- `MaModels.kt`: `MediaItem` (one tolerant class for all media types and ItemMapping), `Player`, `PlayerQueue`, `QueueItem`, `SearchResults`, `RecommendationFolder`, `MaEvent` (`asPlayer()`, `asQueue()`), `MaJson` (ignores unknown keys).
- `args(...)` / `decode()` / `decodeList()` (internal, `ApiSupport.kt`) — lists skip items that don't parse.

## Pitfalls
- The client never logs; keep it that way (args carry tokens/passwords).
- Unconfirmed until fixtures are recorded (⚠️ in MA-API.md): event shapes, imageproxy URL, `order_by` values, `partial` behaviour, shape of the `auth` result. Models are tolerant, so mismatches show up as empty fields, not crashes.
- OkHttp is pinned to 5.1.0: 5.5.x requires compileSdk 37 (AGP 9.0.1 supports 36).
