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
_(uzupełnia sesja)_
