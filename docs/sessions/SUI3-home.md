# SUI3 — Start (wariant 03B-1)
**Model:** Sonnet · **Gałąź:** `sui3-home` · **Moduły:** `:feature:home`. Zależy od: SUI1. **Nie zmieniaj `:core:design`** (w tej fali należy do SUI2). Braki zgłoś w „Wynik”.
**Makieta:** `png/03B-start.png`.

## Zadania
1. Nagłówek „Dzień dobry, <imię>” i pigułka stanu połączenia (jak dziś).
2. Rzędy:
   - **Ulubione stacje**: `StationTile`, kwadrat + nazwa, ~8 widocznych;
   - **Ulubione playlisty**: `PlaylistMosaic` + nazwa;
   - **Ostatnio odtwarzane**.

   Na końcu rzędu stacji i playlist kafel „Wszystkie…” prowadzi do Radio / Playlisty. Jeśli tras jeszcze nie ma, zostaw sam parametr nawigacji.
3. Fokus startowy na pierwszej ulubionej stacji, `OK` od razu odtwarza. Przytrzymanie `OK` otwiera menu kontekstowe z S14b. Dodaj w nim „Przesuń”, jeśli to proste; jeśli nie, zapisz w „Wynik”.
4. Kafle „Radio” i „YouTube Music” znikają ze Startu. Mini-odtwarzacz na dole zostaje (należy do powłoki).
5. Tło: `ContentBackdrop` z koloru zaznaczonego kafla.
6. Kolejność rzędów czytaj z preferencji. Jeśli SUI8a jeszcze ich nie dodała: stała kolejność i TODO w „Wynik”.

## Gotowe, gdy
- Zrzut z emulatora zgodny z makietą (układ, nie piksel w piksel).
- Test ViewModelu: sortowanie i puste stany (brak ulubionych → zachęta „Dodaj stację do ulubionych”).
- PR otwarty.

## Wynik
**Wynik (SUI3, gałąź `sui3-home`):**
- Start wg 03B-1: nagłówek + szklana pigułka połączenia, rzędy Ulubione stacje (kwadratowe `StationTile`), Ulubione playlisty (`PlaylistMosaic`), Ostatnio odtwarzane (tylko gdy są), kafel „Wszystkie … →” na końcu stacji i playlist, tło `ContentBackdrop` z zaznaczonego kafla.
- Fokus startowy na pierwszej ulubionej stacji; `OK` odtwarza na `settings.playerId`; przytrzymanie `OK` = wspólne menu kontekstowe (`ItemMenuHost`). Puste ulubione → podpowiedź „Dodaj stację/playlistę do ulubionych”.
- Usunięte: karty Radio / YouTube Music / Kontynuuj, rzędy Ulubione i Polecane (razem z ich wywołaniami API). Playlisty = tylko ulubione (`favorite=true`), stacje i playlisty sortowane po nazwie.
- API: `HomeScreen(..., onOpenRadio, onOpenPlaylists)` — `:app` ma je podpiąć po dodaniu tras (SUI2); `onOpenRadio` domyślnie = `onOpenSources`, `onOpenPlaylists` domyślnie nic nie robi. `HomeRow`, `DEFAULT_ROW_ORDER`, `HomeUiState.visibleRows`.
- TODO(SUI8a): kolejność rzędów czytana z preferencji (dziś stała `DEFAULT_ROW_ORDER`).
- Nie zrobione: „Przesuń” w menu kontekstowym (wymaga zmiany `ContextMenu` w `:core:design` i zapisu kolejności; poza zakresem tej fali). Imię w powitaniu: MA nie daje nazwy użytkownika, zostaje samo „Dzień dobry”.
- Brak w `:core:design`: stary `StationTile(name, provider, initials, …)` nadal istnieje obok nowego; po SUI2 można go usunąć. `PlaylistMosaic` dostaje jedną okładkę (MA nie zwraca czterech), więc mozaika wypełnia kafel jednym obrazem.
- Pułapki: na emulatorze `OK` na stacji odpala `SpinroomPlaybackService` i apka padła (`ForegroundServiceDidNotStartInTimeException`) — to `:core:audio`, nie Start. Lint w moim worktree padał na `local.properties` (PropertyEscape), więc `check.sh -x lintDebug`; build i testy zielone. Zrzuty: układ zgodny z makietą, ale kolejny shell (menu boczne) jest jeszcze stary.
