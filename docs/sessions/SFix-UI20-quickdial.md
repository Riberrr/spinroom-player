# SFix-UI20 — Zmiana numeru ulubionej stacji cyfrą z pilota
**Model:** Sonnet · **Gałąź:** `sfix-ui20-quickdial`

## Błąd
Numery ulubionych stacji (szybkie wybieranie cyframi) nie dają się zmienić. Chcę przytrzymać cyfrę na pilocie na wybranej stacji w ulubionych (ekran Radio), żeby nadać jej ten numer.

## Zadania
1. Przytrzymanie cyfry (przypisanej do STATION n) na fokusie kafla ulubionej stacji nadaje jej numer n.
2. Zajęty numer: stacje zamieniają się miejscami (numer = pozycja, więc nikt nie zostaje bez numeru).
3. Zapis w `FavoriteStationsOrder`, toast „Stacja X: numer N”, test jednostkowy.

## Gotowe, gdy
Testy zielone, PR otwarty.

## Wynik
- Numer stacji = pozycja w `FavoriteStationsOrder` (index+1), więc „odpięcie” nie istnieje: przypisanie to **zamiana miejsc** (`StationOrder.assign`). Numer większy niż liczba ulubionych: toast „Nie ma stacji z numerem N”, bez zmiany.
- Nowe API: `StationOrder.assign`, `FavoriteStationsRepository.assignNumber`, `StationNumberHub` (`:core:data`, most dispatcher ↔ Radio: `focused`, `onAssign`).
- Ruszone `:app` (minimalnie, bo dispatcher połyka cyfry zanim dojdą do Compose): `RemoteHandler.canAssignStationNumber/assignStationNumber` (domyślnie false), `RemoteKeyDispatcher`, `MainActivity`. Gdy fokus jest na kaflu ulubionej, krótka cyfra gra stację dopiero po puszczeniu klawisza (jak klawisze z akcją hold), przytrzymanie przypisuje numer.
- Cyfry nie mają domyślnych przypisań (użytkownik ustawia je w Ustawieniach → klawisze); hold działa dla każdego klawisza przypisanego do STATION 1–9.
- Testy: `FavoriteStationsOrderTest` (assign), `RemoteKeyDispatcherTest` (hold/short). Emulator: ekran Radio działa; hold cyfry nie sprawdzony end-to-end (brak przypisania cyfr na emulatorze).
