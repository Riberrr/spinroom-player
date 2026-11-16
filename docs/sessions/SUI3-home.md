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
_(uzupełnia sesja)_
