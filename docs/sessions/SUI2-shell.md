# SUI2 — Szkielet: menu boczne, trasy, wysuwany odtwarzacz, obsługa pilota
**Model:** Opus · **Gałąź:** `sui2-shell` · **Moduły:** `:app` (nawigacja, powłoka), `:core:design` (komponenty menu i panelu). Zależy od: SUI0, SUI1.
**Makiety:** `png/01-menu.png`, `png/09A-mini-player.png`.

## Zadania
1. **Menu boczne (01):** wąskie jak dziś (same ikony). Po `←` rozwija się z podpisami i listą „Przypięte”: 5 pierwszych ulubionych (stacje i playlisty) z repozytorium ulubionych z S14b. Pozycje: Start, Radio, Playlisty, Biblioteka, Szukaj, a na dole Ustawienia. Styl szkła z SUI1.
2. Trasy do `RadioScreen` i `PlaylistsScreen` (stuby z SUI0) w `SpinroomNavHost`. Źródła zostają dostępne, np. przez link „Wszystkie źródła” na ekranie Radio albo z Ustawień.
3. **Wysuwany odtwarzacz (09A):** dolny pasek na każdym ekranie poza pełnym odtwarzaczem. Gdy fokus wejdzie na pasek, ten rozwija się w panel (~20 em, rozmyte tło z okładki):
   - lewa część: okładka, ⏮ ⏯ ⏭, ♡ i postęp;
   - prawa część: 3 pozycje kolejki, a przy radiu 3–4 ulubione stacje.

   `Wstecz` zwija panel i **przywraca fokus** tam, gdzie był. Panel zwija się sam po ~8 s bez ruchu.
4. **`RemoteKeyDispatcher`** w `:app`: jedno miejsce obsługi klawiszy pilota w aplikacji. Na razie:
   - przytrzymanie Play/Pause wysuwa odtwarzacz;
   - `CH+` / `CH−` to następny / poprzedni utwór, a gdy gra radio, następna / poprzednia ulubiona stacja (kolejność = kolejność ulubionych).

   Mapowanie ma być wymienne: interfejs `KeyBindings` z wartościami domyślnymi, który SUI8b podmieni na ustawienia.
5. Testy JVM dispatchera: mapowanie klawisz → akcja, radio vs muzyka.

## Gotowe, gdy
Na emulatorze (2–3 zrzuty): menu rozwija się i zwija, przypięte działają, panel odtwarzacza otwiera się z Biblioteki, a `Wstecz` wraca na ten sam kafel. `check.sh` czyste, PR otwarty.

## Wynik
_(uzupełnia sesja)_
