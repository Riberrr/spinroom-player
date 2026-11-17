# :feature:search

Screen "Szukaj". Package `io.github.riberrr.spinroom.feature.search`. Depends on `:core:design`, `:core:data` (+ Coil 3).

## Public API
- `SearchScreen(modifier, onOpenDetails: (uri) -> Unit, viewModel)` — route `search`. Artist/album/playlist rows call `onOpenDetails(uri)`; tracks and radio play at once on the selected player.
- `SearchContent(state, actions)` — stateless UI with `@Preview`s.

Behaviour (see also SUI6 below): on-screen keyboard (35 letters incl. Polish, "123" layer for digits/symbols), query debounced 400 ms (`flatMapLatest`-style
`collectLatest`), blank query never hits the server. One `music/search` call covers every provider; rows show a source badge
(`SearchViewModel.sourceLabel`). Tabs filter by type; "Wszystko" adds a best-result card (first artist, else album, else track).
Voice: mic button and the remote's search key (`Key.Search`) start `RecognizerIntent` (pl-PL); with no recognizer on the TV a notice is shown.
Structure follows the S08 pattern (State / Backend / ViewModel / Screen). Tests: `SearchViewModelTest` (fake backend).
Offline: a timeout from `MaClient` (no connection) ends in the failed state "Brak połączenia z serwerem" (retry), not a spinner; results box is not focusable without results; filter chips are a `LazyRow`.

## SUI6 (search redesign)
- Categories (tabs): Wszystko, Radio, Playlisty, Albumy, Wykonawcy, Utwory; "Filtry · N" opens `FilterPanel` from `:feature:library` (client-side: favorites only, decade, source). Active filters are chips with ✕. "Najlepszy wynik" card removed.
- Hardware keyboard: `mapPhysicalKey` (PhysicalKeys.kt) turns key-down into text / backspace / submit; handled by a root `onPreviewKeyEvent`, D-pad keys (code point 0) pass through. Hint under the on-screen keyboard.
- Favorites: selected row shows "Odtwórz" + heart; long OK toggles too; 5 s undo toast (`FavoriteToast`). `SearchBackend.setFavorite` uses `FavoritesApi`; `isFavorite` is looked up lazily on selection because provider hits arrive with `favorite=false`. Radio order from SUI4 is not wired yet.
- Right-most keyboard keys have `focusProperties.right = first result` (the geometric search would stop on the list container).
- Backdrop: `ContentBackdrop` from the selected row's image (stations without logo: seed color).
