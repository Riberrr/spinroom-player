# SUI5 — Playlisty, Biblioteka lista + podgląd, filtry
**Model:** Sonnet · **Gałąź:** `sui5-playlists-library` · **Moduły:** `:feature:playlists`, `:feature:library`, `:core:data` (nowe pliki). Zależy od: SUI0, SUI1.
**Nie zmieniaj `:core:design`** w tej fali. Panel filtrów zrób w `:feature:library` jako publiczny komponent `FilterPanel` (SUI6 go użyje) i zapisz w „Wynik”, czy przenieść go później do `:core:design`.
**Makiety:** `png/05-playlisty.png`, `png/08-biblioteka-lista.png`, `png/09A-styl-biblioteka.png` (styl).

## Zadania
1. **Playlisty:** siatka `PlaylistMosaic`, sortowanie „Ostatnio grane” (domyślne) lub A–Z, filtr źródła. Pod zaznaczoną playlistą: „▶ Odtwórz” (`OK`) i „Losowo” (`→`); `↓` otwiera szczegóły.
2. **Biblioteka:**
   - przełącznik Siatka / Lista (zapamiętany);
   - lista: wiersze z miniaturą, tytułem, wykonawcą i rokiem;
   - po prawej szklany panel podglądu zaznaczonego albumu: okładka, metadane, Odtwórz, Losowo, ♡, pierwsze utwory;
   - `→` przenosi fokus do podglądu;
   - tło `ContentBackdrop` z okładki zaznaczonego albumu.
3. **Filtry (07):** panel wysuwany po `←`: Gatunek, Dekada, Źródło, „Tylko ulubione”. Wyniki aktualizują się od razu, chip „Wyczyść filtry”. Model `LibraryFilter` w `:core:data`, zapytania przez API MA (sprawdź w `docs/MA-API.md`, co da się filtrować po stronie serwera).
4. Klawiatura fizyczna w Bibliotece: litera skacze do pierwszej pozycji na tę literę.

## Gotowe, gdy
- Na emulatorze zrzuty: Playlisty, Biblioteka (lista + podgląd), Biblioteka z otwartym panelem filtrów.
- Testy filtra i sortowania.
- PR otwarty.

## Wynik
_(uzupełnia sesja)_
