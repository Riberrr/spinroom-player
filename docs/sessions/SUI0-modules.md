# SUI0 — Nowe moduły Radio i Playlisty
**Model:** Haiku · **Gałąź:** `sui0-modules` · **Moduły:** nowe `:feature:radio`, `:feature:playlists`; `settings.gradle.kts`, `app/build.gradle.kts` (koordynator pozwala na tę sesję).

## Zadania
1. Utwórz `:feature:radio` i `:feature:playlists`, kopiując strukturę najmniejszego modułu `:feature:*` (build.gradle.kts, README.md, pakiet `io.github.riberrr.spinroom.feature.radio` / `.playlists`).
2. Stuby ekranów jak w S00b: `RadioScreen(...)`, `PlaylistsScreen(...)` z tekstem „Radio” / „Playlisty” ze `strings.xml`. Parametry nawigacji takie jak w `HomeScreen`.
3. Dopisz moduły do `settings.gradle.kts` i zależności w `app/build.gradle.kts` (na końcu sekcji). Nie podpinaj tras (zrobi to SUI2).

## Gotowe, gdy
`bash scripts/check.sh` czyste, PR otwarty. Bez testów na emulatorze.

## Wynik
- Działa: `:feature:radio` (`RadioScreen`) i `:feature:playlists` (`PlaylistsScreen`) jako stuby na tle `SpinroomColors.Bg`, teksty „Radio” / „Playlisty” z `strings.xml`. Oba w `settings.gradle.kts`; `app/build.gradle.kts` ma na nie zależności. Każdy ma `build.gradle.kts` i README.
- Publiczne API: `RadioScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails, onOpenSettings)` i `PlaylistsScreen(...)` z tymi samymi parametrami co `HomeScreen`. Trasy nie są podpięte w `:app` (SUI2).
- Weryfikacja: `assembleDebug assembleRelease testDebugUnitTest` zielone. `lintDebug` pada wyłącznie przez lokalny `local.properties` (reguła PropertyEscape na ścieżce Windows). Nie wynika z kodu; plik nie jest commitowany.
- Pułapka: `ANDROID_HOME` nie wystarcza, gdy lint czyta `local.properties`; trzeba go usunąć albo zmienić ścieżkę na czystym środowisku.
- Emulator nie używany (sesja bez testów wyglądu).
- Dalej: SUI2 wypełnia ekrany i podpina trasy.
