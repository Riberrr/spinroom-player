# SFix03c — Układ Szczegółów, Teraz odtwarzane i mini-odtwarzacza
**Model:** Sonnet · **Gałąź:** `fix-player-layout` · **Pliki:** `:feature:details` (`DetailsScreen.kt`), `:feature:nowplaying` (`NowPlayingScreen.kt`), `core/design/.../MiniPlayerBar.kt`

## Błędy (emulator 1280×720 dp, wydanie 1.0.1)
1. Szczegóły playlisty: przycisk „Dodaj do kolejki” łamie się w pionowy słupek („Dod/aj/do/kolej/ki”), a rząd przycisków się nie mieści. Długi tytuł („Hity 2026 - Muz…”) jest ucięty i nie da się go przeczytać.
2. Teraz odtwarzane: rząd przycisków jest ucięty przy prawej krawędzi i nie widać przycisku „następny” ani dalszych. Karta „Następne w kolejce” łamie tekst „↓ pokaż całą” litera po literze.
3. Radio (strumień na żywo): mini-odtwarzacz i Teraz odtwarzane pokazują „0:54 / 0:00” i pusty pasek postępu. Przy czasie całkowitym 0 albo braku `duration` ukryj pasek i czasy (ewentualnie znacznik „Na żywo”).

## Zadania
1. Przyciski: `maxLines = 1`, bez łamania słów; przy braku miejsca skrócony tekst albo przyciski z samą ikoną. Rząd ma się mieścić w 1280 dp przy menu zwiniętym i rozwiniętym.
2. Tytuł w szczegółach: 2 linie albo marquee na fokusie.
3. Pasek postępu dla live: wspólna funkcja (np. `isLive(duration)`) w `:core:design` lub `:core:data` z testem JVM.
4. Sprawdź `@Preview` w 1280×720 (podgląd zamiast emulatora).

## Gotowe, gdy
Podglądy pokazują poprawny układ, test `isLive` zielony, `bash scripts/check.sh` czyste. PR otwarty.

## Wynik
_(uzupełnia sesja)_
