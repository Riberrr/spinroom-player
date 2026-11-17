# :core:design

Brand tokens and shared TV UI (package `io.github.riberrr.spinroom.core.design`). Exposes Compose + tv-material as `api`.
All components are stateless; texts are passed in (Polish, from `strings.xml` of the caller). Previews included.

- `SpinroomColors` — colors from docs/BRAND.md. `SpinroomTheme { }` — dark scheme + Inter typography (`SpinroomTypography`, `InterFontFamily`; Inter OFL licence in `INTER-LICENSE.txt`). Sets `LocalContentColor` to text-primary.
- `SpinroomIcons` — `Home, Library, Sources, Search, Settings, Music, Play, Pause, Previous, Next` (ImageVector; use `Icon(..., tint)`).
- `FocusSurface(onClick, modifier, shape, containerColor, focusedContainerColor, focusedScale = 1.05f) { content }` — base focusable with accent outline; build custom clickable items from it.
- `CoverTile(title, subtitle, onClick, modifier, size = 148.dp, cover = placeholder)` — square cover (slot: put Coil `AsyncImage` there), scale 1.08 + outline on focus. `CoverPlaceholder(modifier, tint)`.
- `SectionRow(title, modifier) { item { } / items(..) }` — title + `LazyRow`, 48 dp side padding.
- `StationTile(name, provider, initials, onClick, modifier, badgeColor)` — radio/source chip.
- `SideMenu(items: List<SideMenuItem(id,label,icon)>, selectedId, onSelect, bottomItems, enterFocusRequester, onFocusChange)` — 88 dp icon rail, expands to 240 dp with labels while focused as an overlay (layout slot stays 88 dp). Focus entering the menu always lands on `selectedId` (focusGroup + onEnter).
- `MiniPlayerBar(state: MiniPlayerState(title, artist, isPlaying, positionMs, durationMs), onPrevious, onPlayPause, onNext, onOpenNowPlaying, nowPlayingLabel, cover)`; `ProgressBar(positionMs, durationMs)`.
- `EmptyState(title, message?, actionLabel?, onAction?)`, `ErrorState(message, title, retryLabel, onRetry?)` — centered; the action button takes initial focus.

Type scale (TV, min 14 sp): display 48/40/32, headline 32/28/24, title 22/18/16, body 18/16/14, label 16/14/14.
Layout assumes a 1280×720 dp screen; components use weights so they also hold at 960 dp.

## Context menu (S14b)
`FocusSurface/CoverTile/StationTile(onLongClick)` = hold OK. `ItemMenuHost { screen }` (wrap once per screen) provides `LocalItemMenu` — tiles call `LocalItemMenu.current(uri, title)` — and shows `ContextMenu` (Odtwórz następny / Do kolejki / Ulubione). Logic: `ItemMenuViewModel` in `:core:data` (plays on `settings.playerId`; favorite flag via `music/item_by_uri`). Menu strings live in this module.

## Style foundation (SUI1)
- `SpinroomTheme(theme, accent, solidBackground) { }` — ids from `Appearance` (`:core:data`): themes `grafit` (default), `granat`, `oled`; accents `amber` (default), `mint`, `sky`, `coral`, `lavender`. `SpinroomColors.Bg/Surface/SurfaceHigh/Outline/Accent` are **composable getters** (follow the theme); `Glass` (white 6%), `GlassHigh` (12%), `GlassBorder` are constants. `LocalSolidBackground` mirrors the flag.
- `GlassSurface(modifier, shape, high)` — non-focusable translucent card/menu/bar. For clickable glass use `FocusSurface(containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh)`.
- `ContentBackdrop(imageUrl, modifier, seedColor, solidBackground)` — place first in a full-screen `Box`, content after it. Blurred (32 px bitmap, Coil `TinyBlurTransformation`, cached per URL) + 60% dim, 300 ms crossfade, changes debounced 250 ms (`settleAfterFirst`). Pass the focused item's cover URL (or `seedColor` e.g. `stationColor(name)`). Needs Coil's image loader (the app's).
- `StationTile(name, subtitle, onClick, size, logoUrl, logo)` — square; no logo → `stationInitials(name)` on `stationColor(name)`. (The older `StationTile(name, provider, initials, …)` chip remains until Home migrates.)
- `PlaylistMosaic(imageUrls)` 2×2 (<4 URLs → first one, none → placeholder). `ResultRow(title, subtitle, onClick, favorite: Boolean?, thumbnail)` on glass. `FavoriteHeart(filled)`; icons `SpinroomIcons.HeartFilled/HeartOutline`.

## Shell (SUI2)
- `SideMenu(…, pinned: List<PinnedEntry>, pinnedTitle, onSelectPinned)` — expanded menu shows a "Przypięte" list (thumb + title + subtitle); more compact rows so 5 pinned + 5 items + settings fit 540 dp. Icons `SpinroomIcons.Radio`, `Playlists`.
- `PlayerDock(state, coverUrl, isRadio, favorite, entries, entriesTitle, onPrevious/onPlayPause/onNext/onFavorite/onOpenNowPlaying/onEntryClick, onCollapse, barFocus, labels, onExpandedChange)` — 80 dp glass bar; focus entering it unfolds a 240 dp panel over the content (blurred cover backdrop, controls left, `DockEntry` list right). Owns "expanded" = focus inside; `onCollapse` (Back, `DOCK_IDLE_MS` = 8 s idle) must move focus out. `DockEntry`, `DockLabels`.
- `EntryThumb(name, imageUrl, isStation)` — small square thumbnail with station initials / cover placeholder fallback.

## SUI4
- `OnScreenKeyboard(symbols, labels: KeyboardLabels, onType, onBackspace, onToggleSymbols, modifier, firstKey)` — the 7-column Polish keyboard on glass keys (same layout as Szukaj). Search (SUI3) can adopt it instead of its private copy.
