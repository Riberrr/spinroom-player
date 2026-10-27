# :app

Entry point. `MainActivity` hosts Compose (`SpinroomTheme`). Hilt (`SpinroomApp`, `@AndroidEntryPoint`) and `SpinroomNavHost` (routes in `Routes`; Start = home, `details/{itemUri}` URL-encoded) wired to the `:feature:*` stubs. `WithStubNav` button row is temporary.
- Manifest: TV-only (`LEANBACK_LAUNCHER`, leanback required, no touchscreen), banner + adaptive icon.
- Version: `-PappVersion=x.y.z` (from CI tag) → versionName, versionCode = x·10000+y·100+z.
- Release signing: `keystore.properties` locally or `SIGNING_*` env vars in CI; otherwise the debug key (CI makes a new one per run, so such builds cannot update each other).
- Debug build installs side by side as `io.github.riberrr.spinroom.debug`.
