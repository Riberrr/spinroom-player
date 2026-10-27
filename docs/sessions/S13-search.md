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
_(uzupełnia sesja)_
