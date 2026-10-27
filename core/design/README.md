# :core:design

Brand tokens and shared TV UI (package `io.github.riberrr.spinroom.core.design`). Exposes Compose + tv-material as `api`.
All components are stateless; texts are passed in (Polish, from `strings.xml` of the caller). Previews included.

- `SpinroomColors` — colors from docs/BRAND.md. `SpinroomTheme { }` — dark scheme + Inter typography (`SpinroomTypography`, `InterFontFamily`; Inter OFL licence in `INTER-LICENSE.txt`). Sets `LocalContentColor` to text-primary.
- `SpinroomIcons` — `Home, Library, Sources, Search, Settings, Music, Play, Pause, Previous, Next` (ImageVector; use `Icon(..., tint)`).
- `FocusSurface(onClick, modifier, shape, containerColor, focusedContainerColor, focusedScale = 1.05f) { content }` — base focusable with accent outline; build custom clickable items from it.
- `CoverTile(title, subtitle, onClick, modifier, size = 148.dp, cover = placeholder)` — square cover (slot: put Coil `AsyncImage` there), scale 1.08 + outline on focus. `CoverPlaceholder(modifier, tint)`.
- `SectionRow(title, modifier) { item { } / items(..) }` — title + `LazyRow`, 48 dp side padding.
- `StationTile(name, provider, initials, onClick, modifier, badgeColor)` — radio/source chip.
- `SideMenu(items: List<SideMenuItem(id,label,icon)>, selectedId, onSelect, bottomItems, firstItemFocusRequester, onFocusChange)` — 88 dp icon rail, expands to 240 dp with labels while focused.
- `MiniPlayerBar(state: MiniPlayerState(title, artist, isPlaying, positionMs, durationMs), onPrevious, onPlayPause, onNext, onOpenNowPlaying, nowPlayingLabel, cover)`; `ProgressBar(positionMs, durationMs)`.
- `EmptyState(title, message?, actionLabel?, onAction?)`, `ErrorState(message, title, retryLabel, onRetry?)` — centered; the action button takes initial focus.

Type scale (TV, min 14 sp): display 48/40/32, headline 32/28/24, title 22/18/16, body 18/16/14, label 16/14/14.
Layout assumes a 1280×720 dp screen; components use weights so they also hold at 960 dp.
