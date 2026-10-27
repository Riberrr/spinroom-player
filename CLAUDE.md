# Spinroom Player

Android TV client for Music Assistant (unofficial). TV acts as its own MA player via Sendspin, browses the library via the MA WebSocket API. User-facing text and docs are in Polish; code, comments and commits in English.

## Start / end of every session
Sessions run in parallel (own worktree + branch each). Plan index: `docs/SESSIONS.md` (only the coordinator reads it).
- **Start:** read only your file `docs/sessions/<ID>-*.md` and the "Wynik" section of the sessions it depends on. Nothing else up front.
- **Work:** write only inside your session's scope. New API commands/repositories go in new files. Shared files (`settings.gradle.kts`, `libs.versions.toml`, `app/build.gradle.kts`, `docs/STATUS.md`, `docs/SESSIONS.md`) belong to the coordinator; if you must add a library, append it at the end of its section and say so in the PR.
- **End:** fill "Wynik" in your session file (5–10 lines: what works, public API, what's next, pitfalls), append one-line decisions to `docs/DECISIONS.md`, update the README of modules you changed, commit on `s<nr>-<topic>`, push, open a PR. Don't merge — the coordinator does.
- One session = one session file. Fixes after merge go to a new SFix session, not back into the old conversation.

## Commands
- Build + unit tests (errors only): `bash scripts/check.sh`
- Single module tests: `./gradlew :core:sendspin:testDebugUnitTest -q`
- Install on TV: `adb connect <TV-IP> && adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Logs: `adb logcat -d -s Spinroom:* AndroidRuntime:E | tail -80` (never unfiltered)
- CI failure: `gh run view --log-failed | tail -60` (never the full log)
- Local SDK: `local.properties` → `sdk.dir=C:/Users/lukas/AppData/Local/Android/Sdk` (forward slashes)

## Stack
Kotlin, AGP 9.0.1 with built-in Kotlin (do NOT apply `org.jetbrains.kotlin.android`), Kotlin 2.3.20, Gradle 9.1, JDK 17 target. minSdk 26, compile/targetSdk 36. Compose + `androidx.tv:tv-material` 1.1.0. Versions only in `gradle/libs.versions.toml`.
Planned (add when the session needs it, not earlier): OkHttp WS, Ktor CIO server, NsdManager, noise-java, Media3 MediaSessionService, Coil, Hilt, DataStore, kotlinx.serialization.

## Modules (read a module's README.md instead of its code)
| Module | Owns |
| --- | --- |
| `:app` | MainActivity, navigation, manifest, icon/banner, versioning, signing |
| `:core:design` | `SpinroomColors`, `SpinroomTheme`, shared TV components |
| `:core:sendspin` | Sendspin 1.0 protocol: messages, handshake, Noise, clock sync (pure logic, JVM tests) |
| `:core:audio` | FLAC/Opus decode, timed buffer, AudioTrack, MediaSessionService |
| `:core:ma-api` | MA WebSocket API client, auth, models, events as Flow |
| `:core:data` | repositories, DataStore, caching |
| `:feature:*` | one per screen; create it in the session that builds the screen |

Package root: `io.github.riberrr.spinroom`. Library modules copy `core/audio/build.gradle.kts` as a template.

## Reference docs (read only when the task needs them)
- `docs/SENDSPIN.md`, `docs/MA-API.md` — protocol/API digests (written in S1). Use these, don't re-fetch the specs.
- `docs/BRAND.md` — name, colors, icon, tone of UI text.
- `docs/PROJECT.md` — original app concept (scope, screens, remote keys).
- `design/screens/*.png` — mockups, 1280×720 = TV dp size. Open only the screen you're building.

## Token rules
- Never read `design/source/**` (1.7 MB bundle), build intermediates or `.gradle/**` — blocked in `.claude/settings.json`.
- Prefer tests over screenshots to verify logic. Screenshots only for visual checks.
- Broad research → Explore subagent that returns conclusions only.
- First finished screen (`:feature:nowplaying`, S8) is the pattern for all later screens: copy its structure.

## Conventions
- UI text: short Polish, no jargon ("Nie widzę serwera Music Assistant w sieci", not "mDNS discovery failed"). Strings in `res/values/strings.xml`.
- Colors only via `SpinroomColors` / theme, never raw hex in feature code.
- Every screen must be fully usable with D-pad: visible focus (accent `#F5A524`), Back always works.
- Log tag `Spinroom`. No secrets in code or logs (MA token goes to DataStore).
- Never use "Music Assistant" or its logo as our brand; "for Music Assistant" only as description.
