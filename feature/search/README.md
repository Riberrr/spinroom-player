# :feature:search

Screen "Szukaj". Package `io.github.riberrr.spinroom.feature.search`. Depends on `:core:design`, `:core:data` (+ Coil 3).

## Public API
- `SearchScreen(modifier, onOpenDetails: (uri) -> Unit, viewModel)` — route `search`. Artist/album/playlist rows call `onOpenDetails(uri)`; tracks and radio play at once on the selected player.
- `SearchContent(state, actions)` — stateless UI with `@Preview`s.

Behaviour: on-screen keyboard (35 letters incl. Polish, "123" layer for digits/symbols), query debounced 400 ms (`flatMapLatest`-style
`collectLatest`), blank query never hits the server. One `music/search` call covers every provider; rows show a source badge
(`SearchViewModel.sourceLabel`). Tabs filter by type; "Wszystko" adds a best-result card (first artist, else album, else track).
Voice: mic button and the remote's search key (`Key.Search`) start `RecognizerIntent` (pl-PL); with no recognizer on the TV a notice is shown.
Structure follows the S08 pattern (State / Backend / ViewModel / Screen). Tests: `SearchViewModelTest` (fake backend).
