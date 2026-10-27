# :core:design

Brand tokens and shared TV UI. Public API:
- `SpinroomColors` — colors from docs/BRAND.md.
- `SpinroomTheme { }` — tv-material dark scheme. Typography (Inter) arrives in S7.
Exposes Compose + tv-material as `api`, so feature modules only depend on this module for UI.
