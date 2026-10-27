# S10 — Biblioteka
**Model:** Sonnet · **Zależy od:** S08 · **Gałąź:** `s10-library`

## Przeczytaj
`design/screens/2-biblioteka.png`, sekcja „Wzorzec ekranu” w `feature/nowplaying/README.md`, README `:core:design` i `:core:data`.

## Zadania
- Zakładki Wykonawcy / Albumy / Utwory / Playlisty, siatka okładek ze stronicowaniem (Paging 3).
- Szybki skok po literze; miniatury ~200 px (Coil `size`).
- Nowe komendy API i metody repozytoriów dopisuj w **nowych plikach** (`<Temat>Api.kt`, `<Temat>Repository.kt`), nie edytuj wspólnych — inne sesje pracują równolegle.
- Testy VM na fixtures; README modułu `:feature:library`.

## Gotowe, gdy
Płynne przewijanie przy 2000+ albumów (sprawdź na emulatorze z profilem GPU). Zrzut z emulatora porównany z makietą, `check.sh` zielony, PR otwarty.

## Nie rób
Zmian w innych modułach `:feature:*`.

## Wynik
- `:feature:library`: ekran „Biblioteka” (zakładki Wykonawcy/Albumy/Utwory/Playlisty, siatka okładek 148 dp, miniatury 200 px, pasek skoku `# A D G K N R T Z`, stany pusty/błąd, README modułu). Struktura jak `:feature:nowplaying` (State/Backend/ViewModel/Screen).
- API: `LibraryScreen(modifier, onOpenItem: (String) -> Unit = {})`; `onOpenItem` dostaje URI MA. W `:app` jedna linia w `SpinroomNavHost`: `navController.navigate(Routes.details(uri))` (poza zakresem modułu, konieczna do nawigacji).
- Własne źródło stron `CountedLibrarySource` (z placeholderami, bo `LibraryRepository.pager` ma je wyłączone i nie da się skoczyć do indeksu); skok po literze = wyszukiwanie binarne po `itemAt` (~log2 n zapytań po 1 element).
- Testy: 6 testów VM/źródła na fake backendzie (liczniki, skok do litery, mapowanie wiersza, placeholdery). `check.sh` zielone.
- NIE sprawdzone: wygląd i płynność przy 2000+ albumów na emulatorze/TV (brak tokenu MA w sesji — nie proszę o hasło), zrzut vs makieta; URL `imageproxy` nadal niepotwierdzony.
- Pułapki: sortowanie liter po kodzie znaku (Ł/Ś po Z); brak zakładki „Ulubione”, sortowania i filtra źródeł z makiety; wiersz utworu pokazuje wykonawcę, nie album.
