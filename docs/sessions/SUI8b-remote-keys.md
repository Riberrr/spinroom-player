# SUI8b — Ustawienia: przyciski pilota
**Model:** Sonnet · **Gałąź:** `sui8b-remote-keys` · **Moduły:** `:feature:settings`, `:app` (tylko `RemoteKeyDispatcher` / `KeyBindings`), `:core:data` (nowe pliki). Zależy od: SUI2, SUI8a.
**Makieta:** `png/09B-przyciski.png`.

## Zadania
1. **Lista przypisań:** klawisz → akcja → zasięg („w aplikacji” lub „wszędzie”). Domyślne:
   - `CH+` następny utwór / następna stacja;
   - `CH−` poprzedni utwór / poprzednia stacja;
   - przytrzymanie ⏯ wysuwa odtwarzacz;
   - przytrzymanie `OK` dodaje do ulubionych;
   - czerwony przycisk otwiera Radio.
2. **„Dodaj skrót”:** wybierz akcję → „Naciśnij przycisk na pilocie…” → aplikacja rozpoznaje `keyCode` (także długie przytrzymanie). Ostrzeżenie, gdy klawisz ma znaną funkcję TV. Home, Wstecz i głośności nie pozwalaj nadpisać.
3. **Akcje do wyboru:** następny / poprzedni, odtwórz / pauza, wysuń odtwarzacz, dodaj do ulubionych, otwórz Radio / Playlisty / Szukaj, ulubiona stacja nr 1–9.
4. `KeyBindingsRepository` (DataStore) zasila `RemoteKeyDispatcher` z SUI2.
5. Przełącznik „Działaj poza aplikacją” zapisuje preferencję i pokazuje opis „Wymaga zgody w ustawieniach telewizora”. Samą usługę zrobi SUI9; do tego czasu przełącznik jest nieaktywny, z dopiskiem „wkrótce”.
6. Testy: serializacja przypisań, kolizje, blokada klawiszy systemowych.

## Gotowe, gdy
Na emulatorze: dodany skrót (np. zielony przycisk → Szukaj) działa po `adb shell input keyevent 184`. PR otwarty.

## Wynik
**Działa (JVM + build):** `KeyAssignments`/`KeyRules`/`KeyBindingsRepository`/`KeyGestureDetector`/`KeyCaptureGate` w `:core:data` (testy: serializacja, kolizje, blokady, tap/hold), `AssignedKeyBindings` + rozszerzony `RemoteKeyDispatcher` w `:app` (testy), sekcja „Przyciski pilota” w `:feature:settings` (lista, edycja zasięgu/usuwanie, Dodaj skrót z ekranem „Naciśnij przycisk…”, ostrzeżenia, Przywróć domyślne). `check.sh` przechodzi.
**NIE sprawdzone na urządzeniu:** emulator-5554 był przeciążony (system_server ~100% CPU, app ginie z `ForegroundServiceDidNotStartInTimeException` zanim ekran się pokaże, także po `kill-all`), a instalacja na TV została zablokowana przez uprawnienia. Scenariusz „zielony → Szukaj, `adb shell input keyevent 184`” i wygląd ekranu trzeba obejrzeć ręcznie. Uwaga: wariant debug ma id `io.github.riberrr.spinroom.debug` (pakiet bez `.debug` to starszy build).
**API:** `KeyBindingsRepository.assignments/globalEnabled/update/reset`; `RemoteHandler` +`playPause/toggleFavorite/openRoute/playFavoriteStation`; `ShellViewModel.requestRoute/playStationNumber/navigate`.
**Pułapki:** overlay uczenia klawisza nie jest `Dialog` (Dialog omija `dispatchKeyEvent`); `KeyCaptureGate` wyłącza skróty tylko w trakcie uczenia. Ulubiona stacja nr N bierze kolejność z `ShellViewModel` (kolejność MA), nie z `FavoriteStationsRepository` (SUI4) — do przełączenia razem z CH±.
**Dalej:** SUI9 (usługa „poza aplikacją”) odczyta `global` i `globalEnabled`; włączyć przełącznik.
