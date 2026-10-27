# :core:data

Repositories over `:core:ma-api`, settings in DataStore, Hilt wiring (`di/DataModule.kt`, singletons). Package `io.github.riberrr.spinroom.core.data`.

- `SettingsStore` — `settings: Flow<AppSettings>` (`serverAddress`, `token`, `playerId`, `isConfigured`); `saveLogin`, `setPlayerId`, `clearToken`. Password is never stored.
- `ConnectionManager.start()` — idempotent; connects/disconnects the shared `MaClient` whenever settings change. **Someone must inject it and call `start()` early** (currently only `SettingsViewModel` does; the app should call it from `SpinroomApp`/`MainActivity`).
- `AuthRepository.login(address, user, password)` — login, creates a 1-year token, saves it. Throws `AuthApi.LoginFailed` or network exceptions.
- `LibraryRepository.pager(type, favorite?, search?, orderBy)` → `Flow<PagingData<MediaItem>>` (50/page, end = short page); plus count, album/artist/playlist details and tracks, favorites.
- `PlayerRepository` — `queue(id)`, `queueItems(id)`, `players()` as live Flows (snapshot + events); transport via `.api` (`PlayerQueueApi`).
- `BrowseRepository` (recently played, recommendations, browse), `SearchRepository`.
- New repositories: add a new file and a `@Provides` in a new Hilt module file.

Tests: `bash scripts/check.sh`. SettingsStore test uses an in-memory DataStore (the file-backed one fails its rename on Windows JVMs).
