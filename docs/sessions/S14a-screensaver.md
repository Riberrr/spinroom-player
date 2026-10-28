# S14a — Wygaszacz i autostart
**Model:** Sonnet · **Zależy od:** S05, S08 · **Gałąź:** `s14a-screensaver`

## Przeczytaj
README `:feature:nowplaying`, „Wynik” w `S05-service.md`.

## Zadania
1. Po 60 s bez naciśnięcia klawisza: wygaszacz z okładką i tytułem (powolny ruch, ochrona przed wypaleniem). Dowolny klawisz wraca.
2. Autostart: po `BOOT_COMPLETED` startuje usługa odtwarzacza (nie aktywność), żeby TV był widoczny w MA bez otwierania aplikacji. Przełącznik w ustawieniach.
3. Kanał/ikona na ekranie głównym TV — tylko jeśli to mała zmiana; inaczej opisz w „Wynik”.

## Gotowe, gdy
Po restarcie TV odtwarzacz jest widoczny w MA; wygaszacz włącza się i wyłącza poprawnie.

## Wynik
- **Wygaszacz:** po 60 s bez klawisza (i gdy jest utwór) okładka + tytuł + wykonawca na czerni, powolny dryf (Lissajous ±160×60 px, okres 110–170 s), alpha 0,85. Dowolny klawisz wraca i jest „zjadany” (`MainActivity.dispatchKeyEvent`). `ScreensaverLayer` w `:feature:nowplaying`; 3 testy `driftOffset`.
- **Autostart:** `BootReceiver` (`:app`, `RECEIVE_BOOT_COMPLETED`) startuje `SpinroomPlaybackService`, jeśli `AppSettings.autostart` (domyślnie true, `SettingsStore.setAutostart`). Przełącznik w Ustawieniach. **Sprawdzone na TV 10.0.0.6:** po `adb reboot` proces usługi startuje sam, bez aktywności.
- **Niesprawdzone:** wygląd wygaszacza na żywo; widoczność w MA po restarcie (nie patrzyłem w MA).
- **Pułapki:** aplikacja musi być uruchomiona ręcznie raz po instalacji (stopped state — brak BOOT_COMPLETED). Android 15+ zabrania startu FGS `mediaPlayback` z BOOT_COMPLETED — odbiornik łapie wyjątek i loguje; TV z API 30 działa. `adb shell am broadcast BOOT_COMPLETED` nie działa bez roota, testuj reboot.
- **Kanał/ikona na ekranie głównym TV:** nie robiłem (wymaga TvProvider, Play Next / kanały; to osobna, większa zmiana). Banner launchera już jest.
