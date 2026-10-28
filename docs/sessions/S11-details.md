# S11 — Szczegóły albumu, wykonawcy, playlisty
**Model:** Sonnet · **Zależy od:** S10 · **Gałąź:** `s11-details`

## Przeczytaj
`design/screens/4-szczegoly.png`, sekcja „Wzorzec ekranu” w `feature/nowplaying/README.md`, README `:core:design` i `:core:data`.

## Zadania
- Duża okładka, przyciski Odtwórz / Losowo / Dodaj do kolejki, lista utworów; wykonawca: albumy i popularne utwory.
- Otwierane z Start, Biblioteki i Szukaj przez trasę `itemUri` z S00b.
- Nowe komendy API i metody repozytoriów dopisuj w **nowych plikach** (`<Temat>Api.kt`, `<Temat>Repository.kt`), nie edytuj wspólnych — inne sesje pracują równolegle.
- Testy VM na fixtures; README modułu `:feature:details`.

## Gotowe, gdy
Wszystkie trzy typy szczegółów działają z każdego ekranu. Zrzut z emulatora porównany z makietą, `check.sh` zielony, PR otwarty.

## Nie rób
Zmian w innych modułach `:feature:*`.

## Wynik
- `:feature:details`: ekran wg `4-szczegoly.png` dla albumu, wykonawcy (albumy + popularne utwory) i playlisty; duża okładka, Odtwórz / Losowo / Dodaj do kolejki / ulubione, lista utworów z czasem, klik w utwór = gra od niego.
- API: `DetailsScreen(itemUri, modifier, onOpenItem: (String) -> Unit = {})`; `onOpenItem` otwiera album z widoku wykonawcy. W `:app` zmieniona jedna linia w `SpinroomNavHost` (przekazanie `onOpenItem`). Trasa `Routes.details(uri)` bez zmian, więc Start/Biblioteka działają; Szukaj (S13) wystarczy, że wywoła tę samą trasę.
- Gra przez `PlayerQueueApi.playMedia` na `settings.playerId` (album/playlista = jeden URI, wykonawca/utwór = lista utworów + `start_item`); bez wybranego odtwarzacza toast.
- Testy: 12 testów VM na fake backendzie (typy, ładowanie, błąd+retry, play/shuffle/queue, brak gracza, ulubione z rollbackiem). `check.sh` zielone.
- NIE sprawdzone: wygląd z prawdziwymi danymi i zrzut vs makieta (brak tokenu MA w sesji), działanie `itemByUri` na `library://…` dla nie-bibliotecznych elementów, `start_item` jako URI utworu, `imageproxy` nadal niepotwierdzony.
- Pułapki: brak wiersza „Źródło” z makiety i podświetlenia grającego utworu; ulubione dla pozycji spoza biblioteki mogą wymagać id z biblioteki (jak w nowplaying); ikony serca/losowo zdefiniowane lokalnie.
