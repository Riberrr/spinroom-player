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
_(uzupełnia sesja)_
