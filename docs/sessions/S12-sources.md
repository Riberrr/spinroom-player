# S12 — Źródła (wtyczki)
**Model:** Sonnet · **Zależy od:** S08 · **Gałąź:** `s12-sources`

## Przeczytaj
`design/screens/3-zrodla.png`, sekcja „Wzorzec ekranu” w `feature/nowplaying/README.md`, README `:core:design` i `:core:data`.

## Zadania
- Lista providerów z serwera i przeglądanie drzewa `music/browse` z okruszkami (breadcrumbs).
- Odtwarzanie stacji radia i elementów z YouTube Music na odtwarzaczu TV.
- Nowe komendy API i metody repozytoriów dopisuj w **nowych plikach** (`<Temat>Api.kt`, `<Temat>Repository.kt`), nie edytuj wspólnych — inne sesje pracują równolegle.
- Testy VM na fixtures; README modułu `:feature:sources`.

## Gotowe, gdy
Radio i YouTube Music widoczne bez osobnego kodu dla każdej wtyczki. Zrzut z emulatora porównany z makietą, `check.sh` zielony, PR otwarty.

## Nie rób
Zmian w innych modułach `:feature:*`.

## Wynik
_(uzupełnia sesja)_
