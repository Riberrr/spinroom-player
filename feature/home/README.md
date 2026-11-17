# :feature:home

Screen "Start" (variant 03B-1). Package `io.github.riberrr.spinroom.feature.home`. Follows the screen pattern from `:feature:nowplaying/README.md`.
Depends on `:core:design`, `:core:data` (+ Coil 3 for covers).

## Public API
- `HomeScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails(itemUri), onOpenSettings, onOpenRadio, onOpenPlaylists, viewModel)` — stateful, route `home`.
  `onOpenRadio` / `onOpenPlaylists` open the Radio and Playlists routes (wired in `SpinroomNavHost`).
- `HomeContent(state, actions)` — stateless UI with `@Preview`s; `HomeActions` bundles the callbacks.
- `HomeViewModel` (Hilt) over `HomeBackend` (interface + `MaHomeBackend` + `HomeModule`).
- `HomeRow` / `DEFAULT_ROW_ORDER` / `HomeUiState.visibleRows` — row order (TODO(SUI8a): read it from the appearance preferences).

## Behaviour
- Header "Dzień dobry" (MA gives no user name) + glass connection chip.
- Rows in `rowOrder`: favorite stations (square `StationTile`, name only), favorite playlists (`PlaylistMosaic` + name), recently played (only when non-empty).
  Stations and playlists end with a "Wszystkie … →" tile. An empty favorites row shows a hint ("Dodaj stację do ulubionych").
- Stations and playlists are sorted by name (case-insensitive). Playlists are favorites only (`favorite=true`).
- Initial focus: first favorite station (else its "all" tile); OK plays the station on `settings.playerId`. Hold OK = shared context menu (`ItemMenuHost`).
- Backdrop: `ContentBackdrop` follows the focused tile (cover URL, or the station color without a logo).
- The old top cards (continue listening, Radio, YouTube Music) and the favorites / YouTube rows are gone; the mini player belongs to the shell.
- Loads when `Connected` and on retry; each row fails independently, error only if all calls fail; quiet reload on resume and on `media_item_*` events.
- States: no server → "Połącz się z serwerem"; connecting → "Łączę z serwerem…"; failed → error with retry.
- Focus of the last tile is kept in `rememberSaveable` and restored after Back.
