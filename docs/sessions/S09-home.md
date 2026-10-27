# S09 — Ekran Start
**Model:** Sonnet · **Zależy od:** S08 · **Gałąź:** `s09-home`

## Przeczytaj
`design/screens/1-start.png`, sekcja „Wzorzec ekranu” w `feature/nowplaying/README.md`, README `:core:design` i `:core:data`.

## Zadania
- Wiersze: kontynuuj słuchanie, ostatnio odtwarzane, Twoje playlisty, ulubione stacje, polecane z YouTube Music (tylko gdy provider jest zainstalowany).
- Kafelki otwierają szczegóły (trasa z S00b) albo od razu grają stację radia.
- Nowe komendy API i metody repozytoriów dopisuj w **nowych plikach** (`<Temat>Api.kt`, `<Temat>Repository.kt`), nie edytuj wspólnych — inne sesje pracują równolegle.
- Testy VM na fixtures; README modułu `:feature:home`.

## Gotowe, gdy
Wiersze wypełniają się danymi z serwera. Zrzut z emulatora porównany z makietą, `check.sh` zielony, PR otwarty.

## Nie rób
Zmian w innych modułach `:feature:*`.

## Wynik
- `:feature:home`: ekran Start wg `1-start.png` — karta „Kontynuuj słuchanie” (żywa kolejka wybranego odtwarzacza), karty Radio / YouTube Music, wiersze: ostatnio odtwarzane, playlisty, ulubione stacje, polecane z YouTube Music (tylko gdy provider `ytmusic` zwraca rekomendacje).
- API: `HomeScreen(modifier, onOpenNowPlaying, onOpenSources, onOpenDetails(uri), viewModel)`, `HomeContent(state, actions)` z @Preview; `HomeBackend` (fake w testach). `SpinroomNavHost`: podpięte 3 callbacki (jedyna zmiana w `:app`). Kafelki → `Routes.details(uri)`, stacje grają od razu (`play_media`, replace, na `settings.playerId`).
- Testy: 5 testów VM na fake backendzie (wypełnianie wierszy, wiersz YT tylko dla ytmusic, błąd + retry, karta „kontynuuj”, odtwarzanie stacji). `check.sh` zielone.
- NIE sprawdzone: zrzut z emulatora vs makieta oraz dane z żywego serwera (brak tokenu w sesji). Niepotwierdzone: kształt `recently_played_items`/`recommendations`, id providera `ytmusic`, `order_by=name`.
- Pułapki: brak imienia użytkownika w MA — powitanie „Dzień dobry” bez imienia; karty Radio/YouTube prowadzą do Źródeł (S11 może zrobić głębokie linki); kafelki bez `uri` są nieaktywne.
