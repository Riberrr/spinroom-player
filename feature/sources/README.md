# :feature:sources

Screen "Źródła" — providers installed on the MA server (left) and their `music/browse` tree with breadcrumbs (right). Package `io.github.riberrr.spinroom.feature.sources`.
Depends on `:core:design`, `:core:data` (+ Coil 3 for covers). `SourcesScreen()` has no navigation callbacks; route wiring is unchanged in `:app`.

## Public API
- `SourcesScreen(modifier, viewModel = hiltViewModel())` — stateful, route `sources`.
- `SourcesContent(state, actions)` — stateless UI with a 1280×720 `@Preview`.

## Behaviour
- Root `music/browse` (no path) = provider list; focusing/clicking a provider browses its path. No per-plugin code: Radio Browser, TuneIn, YouTube Music, local files all come from the same tree.
- Folder row (`media_type=folder` or not playable with a path) → opens, adds a breadcrumb. Playable row → `play_media(option=replace)` on `settings.playerId`; hint line shows what was sent (or "choose a player" when none).
- Breadcrumbs: earlier ones are focusable and jump up; Back goes one folder up, at the provider root it leaves the screen.
- Heart button (second focus stop in a row, right of the row) toggles favorite for non-folder items; optimistic with rollback.
- Pattern is the one from `:feature:nowplaying` (`SourcesBackend` seam, VM tests with a fake, see `SourcesViewModelTest`).
