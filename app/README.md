# :app

Entry point. `MainActivity` starts `SpinroomPlaybackService` (S05) and hosts Compose (`SpinroomTheme`). Hilt (`SpinroomApp`, `@AndroidEntryPoint`) and `SpinroomNavHost` (routes in `Routes`; Start = home, `details/{itemUri}` URL-encoded) wired to the `:feature:*` stubs. `WithStubNav` button row is temporary.
- Manifest: TV-only (`LEANBACK_LAUNCHER`, leanback required, no touchscreen), banner + adaptive icon.
- Version: `-PappVersion=x.y.z` (from CI tag) → versionName, versionCode = x·10000+y·100+z.
- Release signing: `keystore.properties` locally or `SIGNING_*` env vars in CI; otherwise the debug key (CI makes a new one per run, so such builds cannot update each other).
- Debug build installs side by side as `io.github.riberrr.spinroom.debug`.
- `SpinroomApp` implements `PlaybackHost` (`:core:audio`): Sendspin credentials from `:core:data`, version, notification icon; on the first MA activation it sets `settings.playerId` to this TV's Sendspin client id if no player was chosen. The S03 debug starter (`app/src/debug`) is gone.
- Network security config allows cleartext (SFix02): MA in the home network is plain `http`/`ws` at a user-typed address.
- `SpinroomShell`: Up/Down never leave the screen content for the side menu (SFix02); Left opens the menu.

## Shell (SUI2)
- `SpinroomShell`: menu Start/Radio/Playlisty/Biblioteka/Szukaj + Ustawienia, "Przypięte" = first 5 favorite stations then playlists (`ShellViewModel`; station click plays, playlist opens details). Routes `radio`, `playlists` (stubs from SUI0). Sources reachable from Start only.
- `ShellDock` = `PlayerDock` bound to the activity-scoped `NowPlayingViewModel` (right list: next 3 queue items, or 4 favorite stations while radio plays).
- `RemoteKeyDispatcher(handler, bindings)` — keys → `RemoteAction` via `KeyBindings`; `MainActivity` feeds it `AssignedKeyBindings(KeyBindingsRepository.assignments)` (settings → "Przyciski pilota", SUI8b; defaults in `KeyAssignments.DEFAULTS`). Key with only a hold shortcut still passes its short press; key with both runs the short one on release. Shortcuts are paused while `KeyCaptureGate` is active. Screens opened by shortcuts go through `ShellViewModel.requestRoute`.

## Klawisze poza aplikacją (SUI9)
- `GlobalKeysService` (AccessibilityService, `res/xml/global_keys_service.xml`): filtruje tylko klawisze, nie czyta okien. Decyzja „przechwyć / przepuść” to czysta `GlobalKeyPolicy.interceptedAction` + `GlobalKeyFilter` (spójne połknięcie powtórzeń i key-up), testy w `GlobalKeyPolicyTest`.
- Przechwytuje krótki klawisz z zasięgiem „wszędzie”, gdy: włączony przełącznik (`KeyBindingsRepository.globalEnabled`), `PlaybackActivity.active` z `:core:audio` (gra lub wstrzymane), `AppForeground.visible == false`. Reszta klawiszy (Home, Wstecz, głośność, przytrzymania, nieprzypisane) przechodzi bez zmian. Globalnie: następny/poprzedni (w radiu = ulubiona stacja jak w aplikacji), ⏯, stacja nr N.
- `GlobalKeyController` wykonuje komendy MA bez aktywności (`PlayerQueueApi`, `FavoriteStationsRepository`). `CornerCard` (~3 s, makieta 10C): `TYPE_ACCESSIBILITY_OVERLAY`, bez fokusu i dotyku, bez dodatkowej zgody; gdy system odmówi, powiadomienie.
- Włączanie: Ustawienia → Przyciski pilota → „Działaj poza aplikacją” zapisuje preferencję i otwiera systemowe ustawienia dostępności (instrukcja pod przełącznikiem); stan usługi odświeża się po powrocie (ON_RESUME).
- **Google Play:** usługa ułatwień dostępu wymaga deklaracji użycia w Play Console i jest sprawdzana pod kątem prywatności; sterowanie pilotem to nie jej zwykły cel, więc odmowa jest realnym ryzykiem. Build do sklepu bez usługi: `./gradlew assembleRelease -PglobalKeys=false` (usługa znika z manifestu, przełącznik z ustawień, reszta aplikacji bez zmian). Sideload/GitHub: domyślnie włączona.
- Test ręczny: `adb shell settings put secure enabled_accessibility_services <applicationId>/io.github.riberrr.spinroom.GlobalKeysService`, `adb shell input keyevent 3` (Home), `adb shell input keyevent 166` (CH+).
