# :feature:library

Screen "Biblioteka" (route `library`): tabs Wykonawcy / Albumy / Utwory / Playlisty, paged cover grid, letter jump rail.
Package `io.github.riberrr.spinroom.feature.library`. Structure follows "Wzorzec ekranu" in `:feature:nowplaying`.

## Public API
- `LibraryScreen(modifier, onOpenItem: (String) -> Unit = {})` — stateful; `onOpenItem` gets an MA URI (`library://album/12`), `:app` routes it to `Routes.details(uri)`.
- `LibraryContent(state, status, itemCount, itemAt, actions)` — stateless, has `@Preview`s.
- `LibraryBackend` — seam for tests: `pager(tab)`, `count(tab)`, `itemAt(tab, offset)`, `serverAddress`.

## Behaviour
- Lists come from `music/<type>/library_items` sorted by name through `CountedLibrarySource`: the total is fetched first, so Paging runs with
  placeholders and the grid can `scrollToItem(n)` to any index (pages of 50 load around it). Not `LibraryRepository.pager` (no placeholders).
- Letter jump (`#`, A D G K N R T Z): binary search with `itemAt` (1-item requests, ~log2(n) calls) for the first name >= letter, then scroll.
  Accented letters (Ł, Ś…) are compared by code point, so they land after Z — approximate.
- Thumbnails are requested at 200×200 px (Coil `size`). Item count shows in the selected tab's label.
- Not yet: "Ulubione" tab, sort/source filters from the mockup (not in S10 scope).
