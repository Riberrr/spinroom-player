# S13 — Szukaj
**Model:** Sonnet · **Zależy od:** S08 · **Gałąź:** `s13-search`

## Przeczytaj
`design/screens/5-szukaj.png`, sekcja „Wzorzec ekranu” w `feature/nowplaying/README.md`, README `:core:design` i `:core:data`.

## Zadania
- Klawiatura ekranowa jak na makiecie, wyszukiwanie z opóźnieniem (debounce), wyniki pogrupowane wg typu i źródła.
- Wyszukiwanie głosowe: `RecognizerIntent` + przycisk mikrofonu/`KEYCODE_SEARCH` z pilota.
- Nowe komendy API i metody repozytoriów dopisuj w **nowych plikach** (`<Temat>Api.kt`, `<Temat>Repository.kt`), nie edytuj wspólnych — inne sesje pracują równolegle.
- Testy VM na fixtures; README modułu `:feature:search`.

## Gotowe, gdy
Wyniki ze wszystkich providerów; głos działa na TV z mikrofonem w pilocie. Zrzut z emulatora porównany z makietą, `check.sh` zielony, PR otwarty.

## Nie rób
Zmian w innych modułach `:feature:*`.

## Wynik
Ekran gotowy: klawiatura ekranowa, debounce 400 ms, jedno `music/search` dla wszystkich providerów, wyniki grupowane wg typu z plakietką źródła, zakładki filtrów, „najlepszy wynik”, głos (`RecognizerIntent` + `Key.Search`).
API: `SearchScreen(onOpenDetails)`; `SpinroomNavHost` zmieniony tylko o callback do details. Utwór/radio odtwarza się od razu na wybranym odtwarzaczu (`playMedia`).
Testy VM na fake backendzie (6), `check.sh` zielony. **Nie zweryfikowano**: zrzut z emulatora vs makieta i głos na prawdziwym TV (brak emulatora w tej sesji).
Pułapki: źródło = domena `provider` (np. `ytmusic--id`), wyniki biblioteki mają provider `library` → „Biblioteka”; klawiatura ma dodatkowe q,v,x względem makiety. Dalej: ewentualnie przycisk „Odtwórz” obok otwarcia albumu.
