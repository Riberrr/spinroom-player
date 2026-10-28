# :feature:details

Details screen for an album, artist or playlist (`DetailsScreen(itemUri, onOpenItem)`), route `details/{itemUri}`. Layout follows `design/screens/4-szczegoly.png`. Structure copies `:feature:nowplaying` (see its "Wzorzec ekranu").

- `DetailsState.kt` — `DetailsUiState`, `TrackRow`, `AlbumRow`, kind/status/message enums.
- `DetailsBackend.kt` — small interface over `:core:data` (`LibraryApi.itemByUri`, `LibraryRepository` tracks/albums, `PlayerRepository.api.playMedia`) + Hilt binding. Fake it in tests.
- `DetailsViewModel.kt` — `load(uri)` (idempotent), `onPlay(shuffle, track)`, `onAddToQueue()`, `onToggleFavorite()` (optimistic, rolls back), `retry()`.
- `DetailsScreen.kt` — stateless `DetailsContent` (with @Preview) + `DetailsScreen` wrapper.

Behaviour: the kind comes from the item's `media_type`. Album/playlist play sends the item URI (MA keeps context); artist play and "play from track" send the track list with `start_item`. Artist = albums row (opens the album via `onOpenItem`) + top tracks. No player selected → toast "Najpierw wybierz odtwarzacz".

Tests: `./gradlew :feature:details:testDebugUnitTest -q` (VM on a fake backend).
