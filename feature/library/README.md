# :feature:library

Screen "Biblioteka" (route `library`): tabs Wykonawcy / Albumy / Utwory / Playlisty, paged cover grid, letter jump rail.
Package `io.github.riberrr.spinroom.feature.library`. Structure follows "Wzorzec ekranu" in `:feature:nowplaying`.

## Public API
- `LibraryScreen(modifier, onOpenItem: (String) -> Unit = {})` — stateful; `onOpenItem` gets an MA URI (`library://album/12`), `:app` routes it to `Routes.details(uri)`.
- `LibraryContent(state, status, itemCount, itemAt, actions)` — stateless, has `@Preview`s.
- `LibraryBackend` — seam for tests: `pager(tab)`, `count(tab)`, `itemAt(tab, offset)`, `serverAddress`, `connection`.

## Behaviour
- Lists come from `music/<type>/library_items` sorted by name through `CountedLibrarySource`: the total is fetched first, so Paging runs with
  placeholders and the grid can `scrollToItem(n)` to any index (pages of 50 load around it). Not `LibraryRepository.pager` (no placeholders).
- Letter jump (`#`, A D G K N R T Z): binary search with `itemAt` (1-item requests, ~log2(n) calls) for the first name >= letter, then scroll.
  Accented letters (Ł, Ś…) are compared by code point, so they land after Z — approximate.
- Thumbnails are requested at 200×200 px (Coil `size`). Item count shows in the selected tab's label.
- Connection gates the screen (`libraryStatus`): no server or not logged in → „Połącz się z serwerem”; connecting/reconnecting with an empty grid → „Łączę z serwerem…”; failed → error state. A list already on screen stays while reconnecting.
- Pager and counts run only once the server is connected and authenticated; counts for all tabs reload each time that happens (they fail silently while offline).
- Letter rail is shown only when the grid has items.
- Not yet: "Ulubione" tab, sort/source filters from the mockup (not in S10 scope).

## SUI5: list + preview, filters
- Chips „Siatka / Lista” (remembered in `ViewModePrefs`), „Filtry · n” and „Wyczyść filtry”. List mode = glass rows (thumbnail, title, artist, year) + `PreviewPane` (cover, metadata, Odtwórz / Losowo / ♡, first 8 tracks of an album or playlist loaded 250 ms after selection settles). `ContentBackdrop` follows the focused cover in both modes.
- `FilterPanel` (public, `FilterPanelActions`, `FilterChip`) slides in on `←` from the first grid column or any list row; Back closes it. Genre / Dekada / Źródło / Tylko ulubione; results update at once. Candidate to move to `:core:design` when SUI6 uses it.
- Filter model: `LibraryFilter` in `:core:data`. Active filter ⇒ plain (non-counted) pager, letter rail hidden, letter jump disabled.
- Physical keyboard: unmodified A–Z jumps to the first item of that letter and focuses it (`letterOf`).
