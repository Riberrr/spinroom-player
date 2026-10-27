# :feature:nowplaying

Screen "Teraz odtwarzane" and the shell mini player, both on one shared state. Package `io.github.riberrr.spinroom.feature.nowplaying`.
Depends on `:core:design`, `:core:data` (+ Coil 3 for covers). Route wiring lives in `:app` (`SpinroomNavHost`).

## Public API
- `NowPlayingScreen()` — stateful screen (route `nowplaying`).
- `NowPlayingMiniPlayer(onOpenNowPlaying)` — `MiniPlayerBar` bound to the same state; draws nothing when no track.
- `activityNowPlayingViewModel()` — the single activity-scoped `NowPlayingViewModel` (screen + mini player share it).
- `NowPlayingContent(state, actions, debuggable)` — stateless UI, has `@Preview`s.

Behaviour: queue/player come live from MA events; progress is `elapsed_time` of the last event + local 1 s ticker (frozen when paused,
clamped to duration, re-synced on every event); play/pause, shuffle, repeat and favorite update optimistically. Player = `settings.playerId`;
in debuggable builds the top-right chip opens a list of MA players (choice saved to `SettingsStore`).
Blur backdrop on API 31+, stronger scrim below.

## Wzorzec ekranu (copy this for every new screen)
File layout in `feature/<name>/src/main/java/.../feature/<name>/`:
- `<Name>State.kt` — immutable `UiState` data class (+ small row/enums). Everything the UI renders, nothing else; derived values as getters.
- `<Name>Backend.kt` — a small interface of *only* what the VM needs from `:core:data`, the `@Inject` implementation wrapping repositories, and a
  `@Module abstract class` with `@Binds`. This is the seam for tests (no WebSocket needed).
- `<Name>ViewModel.kt` — `@HiltViewModel`, `StateFlow<UiState>`, `onXxx()` actions (`on` + verb). Public `@Inject constructor(backend)` delegates
  to an `internal` constructor that also takes clock/tick values for tests. Flows with `flatMapLatest`, `.catch { }` so a failed call never kills the screen.
- `<Name>Screen.kt` — `<Name>Screen(modifier, viewModel = hiltViewModel())` collects with `collectAsStateWithLifecycle` and calls the stateless
  `<Name>Content(state, actions)`; actions bundled in a `<Name>Actions` data class with no-op defaults; `@Preview` 1280×720 per state.
- `res/values/strings.xml` — Polish strings prefixed `<name>_`.
- Build file: copy this module's `build.gradle.kts` (hilt + ksp + hilt-navigation-compose + Compose via `:core:design`).
Screen rules: first focus requested explicitly (`FocusRequester` in `LaunchedEffect`), Back collapses overlays before leaving, empty/error via
`EmptyState`/`ErrorState`, only `SpinroomColors`.

How to test a VM (`src/test`): implement the backend interface as a fake with `MutableStateFlow`s and a command log; `Dispatchers.setMain(StandardTestDispatcher())`;
build the VM with the internal constructor and `{ testScheduler.currentTime }` as clock; drive with `runCurrent()` / `advanceTimeBy()`.
A VM with an endless ticker must have its `viewModelScope` cancelled at the end of the test (see `vmTest` helper) or `runTest` never finishes.
Tests build models by hand; replace with recorded fixtures once they exist.
