# :feature:radio

Screen "Radio" (SUI4). Package `io.github.riberrr.spinroom.feature.radio`. Depends on `:core:design`, `:core:data`.

## Public API
- `RadioScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails, onOpenSettings, viewModel)` — callbacks kept for the shell's signature (unused: stations play in place). Routed in `:app` as `radio`.
- `RadioContent(state, actions)` — stateless UI (previews: browse, search).

## What it does
- Favorite stations as numbered square tiles (number = `CH±` order). Hold OK → "Przesuń" (←/→ move, OK or Back finish) / "Usuń z ulubionych". Order lives in `FavoriteStationsRepository` (`:core:data`).
- Browse list (2 columns of `ResultRow`) for **one** filter (Język / Gatunek / Kraj chip, or Popularne); the server cannot combine filters. Chosen filter is remembered, default language Polish. Hold OK on a row = add/remove favorite.
- Search stations (field "Szukaj stacji…"): favorites hide, `OnScreenKeyboard` slides in from the left, results on the right; a physical keyboard types immediately; Back closes. Name search only, filters hidden while searching.
- Search results: deduplicated (name/uri), ordered favorites → Polish → rest; one "Język: wszystkie/polski" toggle chip (other filters impossible: results carry no genre/country).

- Default list = **"Moje stacje"**: every radio station saved in MA (favorites or not), A–Z, loaded 100 at a time (`RadioLibraryRepository`, `:core:data`); next page loads when focus is within 12 rows of the end. Chips / "Popularne" switch to Radio Browser; an empty library falls back to the saved filter.

## Not available from the API
- The track playing now per station and the station's genre/city (Radio Browser items carry neither through MA) — rows show the active filter value instead.
