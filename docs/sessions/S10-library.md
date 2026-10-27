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
_(uzupełnia sesja)_
