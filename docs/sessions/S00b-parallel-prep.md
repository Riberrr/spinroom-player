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
_(uzupełnia sesja)_
