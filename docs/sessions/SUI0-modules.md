# SUI0 — Nowe moduły Radio i Playlisty
**Model:** Haiku · **Gałąź:** `sui0-modules` · **Moduły:** nowe `:feature:radio`, `:feature:playlists`; `settings.gradle.kts`, `app/build.gradle.kts` (koordynator pozwala na tę sesję).

## Zadania
1. Utwórz `:feature:radio` i `:feature:playlists`, kopiując strukturę najmniejszego modułu `:feature:*` (build.gradle.kts, README.md, pakiet `io.github.riberrr.spinroom.feature.radio` / `.playlists`).
2. Stuby ekranów jak w S00b: `RadioScreen(...)`, `PlaylistsScreen(...)` z tekstem „Radio” / „Playlisty” ze `strings.xml`. Parametry nawigacji takie jak w `HomeScreen`.
3. Dopisz moduły do `settings.gradle.kts` i zależności w `app/build.gradle.kts` (na końcu sekcji). Nie podpinaj tras (zrobi to SUI2).

## Gotowe, gdy
`bash scripts/check.sh` czyste, PR otwarty. Bez testów na emulatorze.

## Wynik
_(uzupełnia sesja)_
