# S07 — Motyw, Inter, nawigacja, mini-odtwarzacz
**Model:** Sonnet · **Zależy od:** S00b · **Gałąź:** `s07-ui-shell`

## Cel
Wspólne komponenty TV i powłoka aplikacji, z których korzystają wszystkie ekrany.

## Przeczytaj
`design/screens/1-start.png` (menu boczne, kafelki, mini-odtwarzacz), `docs/BRAND.md` (sekcja kolorów i ikony), `core/design/README.md`.

## Zadania
1. Font Inter (OFL) w `core/design/src/main/res/font/` + tekst licencji; skala typografii pod TV (czytelna z 3 m).
2. Komponenty w `:core:design` (bezstanowe, z `@Preview`): kafelek okładki z fokusem (skala + obwódka akcentu), wiersz sekcji z tytułem, kafelek stacji, menu boczne (ikony, rozwija się w lewo na krawędzi), pasek mini-odtwarzacza, stan pusty, stan błędu.
3. Powłoka w `:app`: menu boczne + `SpinroomNavHost` + mini-odtwarzacz na dole (na razie stan przykładowy; podłączy S08). Wstecz zamyka menu.
4. README `:core:design`: lista komponentów i ich parametry (to czytają kolejne sesje zamiast kodu).

## Gotowe, gdy
`check.sh` zielony; zrzut z emulatora TV 1080p porównany z makietą; D-pad: lewo na krawędzi otwiera menu.

## Nie rób
Danych, API, logiki odtwarzania.

## Wynik
- Gotowe: Inter 400/500/600/700 + licencja OFL, typografia TV, ikony, komponenty w `:core:design` (CoverTile, SectionRow, StationTile, SideMenu, MiniPlayerBar, EmptyState, ErrorState, FocusSurface) z `@Preview`; API w `core/design/README.md`.
- Powłoka w `:app` (`SpinroomShell`): menu boczne + `SpinroomNavHost` + mini-odtwarzacz (stan przykładowy, S08 podłącza prawdziwy; ukryty na „Teraz gra”). Lewo na krawędzi ekranu otwiera menu, Wstecz w menu wraca do treści. Tymczasowy pasek `WithStubNav` usunięty.
- Sprawdzone: `check.sh` zielony; emulator TV 1080p (gęstość 480 → 1280 dp) wygląda jak makieta `1-start.png` (menu, mini-odtwarzacz); D-pad lewo/prawo działa.
- Pułapki: `Text` poza `Surface` jest czarny — `SpinroomTheme` ustawia `LocalContentColor`. Przy starcie fokus ląduje w menu (stuby nie mają fokusowalnych elementów) — ekrany powinny same żądać fokusu. Emulator domyślnie ma 960 dp, nie 1280 dp. Ikony to ścieżki Material (Apache-2.0), bez biblioteki icons-extended.
- Dalej: S08 podłącza stan mini-odtwarzacza; ekrany używają komponentów z README.
