# :feature:playlists

Screen "Playlisty" (SUI5). Package `io.github.riberrr.spinroom.feature.playlists`. Not yet routed in `:app` (SUI2 wires it).

## Public API
- `PlaylistsScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails, onOpenSettings, viewModel)` — stateful; `onOpenDetails` gets a MA URI (`library://playlist/3`).
- `PlaylistsContent(state, status, itemCount, itemAt, actions)` — stateless, has a `@Preview`.
- `PlaylistsBackend` — seam for tests (`pager(sort, provider)`, `sources()`, `play(uri, shuffle)`).

## Behaviour
- Grid of `PlaylistMosaic` tiles, blurred `ContentBackdrop` of the focused cover. Sort chip: „Ostatnio grane” (`order_by=last_played_desc`, default) / A–Z; source chip cycles all sources → each provider (`providers/manifests`).
- OK on a tile = Odtwórz (replaces the queue on the player from settings). Under the focused tile: „Odtwórz”, „Losowo”, „Szczegóły” (↓ from the tile).
- Cover is the playlist's own image (a 2×2 mosaic of track covers would need extra calls per playlist: not done); no image → placeholder.
