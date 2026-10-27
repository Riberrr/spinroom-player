# S00b — Przygotowanie do pracy równoległej
**Model:** Sonnet · **Zależy od:** S00 · **Gałąź:** `s00b-parallel-prep`

## Cel
Raz zmienić wszystkie pliki wspólne, żeby późniejsze sesje równoległe dopisywały tylko własne pliki i nie miały konfliktów.

## Przeczytaj (tylko to)
`gradle/libs.versions.toml`, `settings.gradle.kts`, `app/build.gradle.kts`, `core/audio/build.gradle.kts`, tabela w `docs/SESSIONS.md`.

## Zadania
1. Moduły `:feature:{nowplaying,home,library,details,sources,search,settings}`: build file (Compose, zależności `:core:design`, `:core:data`), README (3 linie), stub `@Composable fun <Nazwa>Screen(...)` z napisem-zaślepką. Dopisz do `settings.gradle.kts` i do `:app`.
2. Graf zależności: `:core:audio` → `:core:sendspin`; `:core:data` → `:core:ma-api`; `:feature:*` → `:core:design`, `:core:data`; `:app` → wszystko.
3. Katalog wersji: dopisz **najnowsze stabilne** wersje (sprawdź w Maven Central / Google Maven, nie zgaduj): OkHttp (+ mockwebserver), Ktor server CIO + websockets, kotlinx-serialization-json (+ plugin), Media3 session/common, Coil 3 compose, Hilt + KSP (zgodny z Kotlinem 2.3.20), DataStore preferences, Paging compose, Navigation compose, lifecycle-viewmodel-compose. Biblioteki Noise nie dodawaj (wybiera S01a).
4. Hilt: plugin + KSP, klasa `SpinroomApp` z `@HiltAndroidApp`, `MainActivity` z `@AndroidEntryPoint`.
5. Nawigacja: w `:app` `SpinroomNavHost` z trasami do wszystkich stubów (Start jako ekran startowy zamiast „Hello”). Trasy szczegółów z parametrem `itemUri`.
6. Zaktualizuj tabelę modułów w `CLAUDE.md`.

## Gotowe, gdy
`bash scripts/check.sh` zielony, D-padem da się przejść między stubami (emulator TV lub test Compose), PR otwarty.

## Nie rób
Żadnej logiki ekranów, sieci ani audio.

## Wynik
- Gotowe: 7 modułów `:feature:*` (stub `<Nazwa>Screen`, `DetailsScreen(itemUri)`), graf zależności, katalog wersji, Hilt+KSP w `:app`, `SpinroomNavHost` + `Routes`. `scripts/check.sh` zielony.
- API: `Routes.details(uri)` koduje URI; start = `Routes.HOME`. Pasek przycisków `WithStubNav` w `:app` jest tymczasowy (D-pad między stubami) — nie sprawdzano na emulatorze ani testem Compose.
- Pułapki: nowsze androidx (lifecycle 2.11, nav 2.10, hilt-nav 1.4) wymagają compileSdk 37 + AGP 9.1, więc przypięte do 2.10.0/2.9.8/1.3.0. Dodane do katalogu, ale jeszcze nieużywane: OkHttp 5.5.1, Ktor 3.6.0, serialization 1.12.0 (+plugin), Media3 1.11.1, Coil 3.6.3, DataStore 1.3.0, Paging 3.5.3. Hilt 2.60.1, KSP 2.3.12. Pliki wspólne edytowane tu zgodnie z zadaniem.
