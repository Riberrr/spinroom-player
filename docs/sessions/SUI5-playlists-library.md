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
- **Działa:** Playlisty (siatka z mozaiką od MA, sortowanie „Ostatnio grane” = `last_played_desc` / A–Z, filtr źródła przyciskiem cyklicznym, tło `ContentBackdrop`). Biblioteka: przełącznik Siatka/Lista (zapamiętany w `ViewModePrefs`), lista + szklany podgląd (okładka, meta, Odtwórz/Losowo/♡, 8 pierwszych utworów albumu/playlisty), panel `FilterPanel` (Gatunek, Dekada, Źródło, Tylko ulubione, „Wyczyść filtry”), litera z klawiatury skacze i ustawia fokus. Zrzuty z emulatora: playlisty, lista + podgląd, panel filtrów (po tych zrzutach zmieniłem tylko `maxLines` chipów).
- **API:** `:core:data` — `LibraryFilter`, `PlaylistSort`, `FilteredLibrary` (pager/items/genres/sources), `FilteringPagingSource`, `ViewModePrefs`, `PlayItem.play(uri, shuffle)`; `:feature:library` — `FilterPanel`, `FilterPanelActions`, `FilterChip` (publiczne, dla SUI6; do przeniesienia do `:core:design` w następnej fali); `:feature:playlists` — `PlaylistsScreen(…, viewModel)` (sygnatura jak wcześniej + opcjonalny VM), nadal bez trasy w `:app` (SUI2).
- **Odstępstwa:** w Playlistach OK = Odtwórz, a „Odtwórz / Losowo / Szczegóły” są pod kafelkiem dostępne przez ↓ (spec „→ Losowo, ↓ szczegóły” blokowałoby przejście do następnej kolumny). `←` w pierwszej kolumnie/na wierszu listy otwiera filtry, więc menu boczne jest wtedy osiągalne tylko z rzędu zakładek.
- **Pułapki:** dekada filtrowana po stronie klienta po `year` (MA nie ma takiego argumentu) i z aktywnym filtrem lista jest bez placeholderów, więc skok do litery i licznik w zakładce są wyłączone. Lista gatunków z MA jest długa (kilkaset pozycji) i niesortowana po popularności. Przy 960 dp (emulator) rząd chipów jest ciasny. Zakładka „Ulubione” z makiety = filtr „Tylko ulubione”.
- **Dalej:** SUI2 dopina trasę Playlist i menu; SUI6 może użyć `FilterPanel`. Mozaika 2×2 z okładek utworów wymagałaby dodatkowych zapytań (MA zwraca własną kompozytową okładkę, więc nie była potrzebna).
