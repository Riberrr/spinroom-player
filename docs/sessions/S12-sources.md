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
- `SourcesScreen()` (sygnatura bez zmian, NavHost nietknięty): lista providerów z `music/browse` (korzeń) po lewej, drzewo wybranego providera po prawej z okruszkami; zero kodu per wtyczka.
- Folder (`media_type=folder`) otwiera się; element grywalny → `play_media(replace)` na `settings.playerId`; serduszko = ulubione (optymistycznie, z cofnięciem).
- Back wychodzi folder wyżej, w korzeniu providera opuszcza ekran. Struktura jak w `:feature:nowplaying` (`SourcesBackend`, `SourcesViewModelTest` – 8 testów).
- Nowych komend API nie było trzeba (`BrowseApi.browse`, `PlayerQueueApi.playMedia` już są).
- Uwaga: brak zrzutu z emulatora i weryfikacji na żywym serwerze MA (kształt `music/browse` wzięty z docs/MA-API.md: `is_playable`, `path`, `media_type`); chipy-filtry z makiety pominięte (to zwykłe foldery w drzewie); format strumienia (AAC 128) pominięty — API go nie daje.
