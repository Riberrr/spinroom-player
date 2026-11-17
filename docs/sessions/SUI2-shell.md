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
**Działa:** menu boczne (ikony → po `←` etykiety + „Przypięte”: 5 ulubionych stacji/playlist), trasy `radio`/`playlists`, dolny `PlayerDock` (pasek → panel 240 dp przy wejściu fokusem; `Wstecz` lub 8 s bez ruchu zwija i wraca na ten sam kafel), `RemoteKeyDispatcher` (hold Play/Pause = panel, CH+/CH− = utwór lub ulubiona stacja w radiu).
**API:** `PlayerDock`, `DockEntry`, `DockLabels`, `EntryThumb`, `SideMenu(pinned…)` w `:core:design`; `RemoteKeyDispatcher`, `KeyBindings`, `RemoteHandler`, `ShellViewModel` w `:app`. `NowPlayingUiState` +`isRadio/mediaUri/mediaName` (drobna zmiana w `:feature:nowplaying`).
**Pułapki:** emulator = 960×540 dp, wszystko liczone pod to; zmiana `canFocus` fokusowanego paska dawała pętlę i OOM — pasek zostaje pod panelem; Play/Pause w emulatorze testowany `adb shell input keyevent --longpress 85`; na emulatorze brak własnego odtwarzacza w MA, więc dock sprawdzony na tymczasowym fake stanie (usunięty), nie na prawdziwym odtwarzaniu; CH+/CH− i stacje w docku nieprzetestowane na żywo (tylko JVM). „Źródła” usunięte z menu, dostępne ze Startu.
**Dalej:** SUI8b podmienia `KeyBindings`; ekran Radio/Playlisty (SUI4/5) powinny dodać link „Wszystkie źródła”.
