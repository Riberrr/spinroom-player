# SFix-UI7 — Styl A „Szkło i poświata”
**Gałąź:** `claude/fix-ui7-style-a-8c4ef1`

## Błąd
Po teście v1.1.0 na TV: menu/belka/kafelki bez okładki wyglądają płasko (czarne tło, jednolite kolory). Wariant A: szkło, akcent, gradienty.

## Zadania
Menu szklane + akcent + węższe; menu schowane w pełnym odtwarzaczu; pozycja „Odtwarzacz” przy Ustawieniach; styl PlayerDock; gradientowe tło bez okładki; kafelki z gradientem i akronimem.

## Wynik
- `StationLogic.kt`: `stableHash`, `gradientColors(key)` (8 stonowanych par), `stationAcronym(name, max=3)`, `backdropGradient(seed)`; testy w `GradientLogicTest`.
- Nowy `AcronymFill` (gradient 135°, akronim ~28 % krycia przycięty w prawym dolnym rogu + akronim na środku) w `StationTile`, `PlaylistMosaic(name=…)`, `EntryThumb`, miniaturach Search/Radio/Home.
- `ContentBackdrop` bez okładki: gradient z koloru seed (deterministyczny, bez migania) zamiast jednolitego koloru/czerni; z okładką bez zmian.
- `SideMenu`: szkło w kolorze tła + hairline, zwinięte 72 dp (`RAIL_WIDTH`, było 88), aktywna pozycja w akcencie (kapsuła 16 % + ikona i tekst).
- Pozycja „Odtwarzacz” (`SpinroomIcons.Player`) nad zębatką: **ukryta, gdy nic nie gra** (i w pełnym odtwarzaczu); OK przenosi fokus na `PlayerDock` (rozwija go).
- Pełny odtwarzacz (`NOW_PLAYING`): menu wsuwa się za lewą krawędź i nie jest komponowane (brak fokusu); Lewo na krawędzi tam nic nie otwiera, wraca po Back (wyjście z odtwarzacza). 
- `PlayerDock`: pasek jako zaokrąglona szklana kapsuła (akcentowy fokus), panel z zaokrąglonymi górnymi rogami.
- Pułapki: lint lokalnie pada na `local.properties` (PropertyEscape, fałszywy alarm w worktree); na emulatorze Sendspin jest Idle, więc nie sprawdzono wizualnie docka i pozycji „Odtwarzacz” podczas grania.
