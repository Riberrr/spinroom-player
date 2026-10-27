# :feature:home

Screen "Start". Package `io.github.riberrr.spinroom.feature.home`. Follows the screen pattern from `:feature:nowplaying/README.md`.
Depends on `:core:design`, `:core:data` (+ Coil 3 for covers).

## Public API
- `HomeScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails(itemUri), viewModel)` — stateful, route `home`. Callbacks are wired in `SpinroomNavHost`.
- `HomeContent(state, actions)` — stateless UI with `@Preview`s; `HomeActions` bundles the callbacks.
- `HomeViewModel` (Hilt) over `HomeBackend` (interface + `MaHomeBackend` + `HomeModule`).

## Behaviour
- Top row: "Kontynuuj słuchanie" card (current item of the chosen player's queue, live; click = now playing; hidden when nothing plays),
  "Radio" card and "YouTube Music" card (both open `sources`; the latter only when YouTube Music recommendations exist).
- Rows: recently played (`music/recently_played_items`), playlists (library, by name), favorite stations (library radios, `favorite=true`),
  "Polecane z YouTube Music" (flattened `music/recommendations` folders whose provider contains `ytmusic`; row hidden otherwise).
- Tiles open details (`Routes.details(uri)`); station tiles play immediately with `play_media(option=replace)` on `settings.playerId` (no-op without a player).
- Loads when the connection becomes `Connected` and on retry; each row fails independently, error state only if all calls fail.
- Greeting is a plain "Dzień dobry" (MA gives no user name); the connection chip shows connected/offline.

## Not verified against a live server
Shape of `recently_played_items` / `recommendations` items, the `ytmusic` provider id, and library `order_by` values.
