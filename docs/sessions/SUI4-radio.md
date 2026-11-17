# SUI4 — Ekran Radio (04A)
**Model:** Sonnet · **Gałąź:** `sui4-radio` · **Moduły:** `:feature:radio`, `:core:data` (nowe pliki), `:core:ma-api` (nowe pliki, jeśli trzeba). Zależy od: SUI1, SUI2.
**Makiety:** `png/04A-radio.png`, `png/04A-radio-szukaj.png`.

## Zadania
1. **Ulubione stacje:** kafle z numerem 1…n; numer to kolejność dla `CH±`. Przytrzymanie `OK` otwiera „Przesuń” (strzałkami) i „Usuń”. Kolejność trzymaj w nowym `FavoriteStationsOrder` (DataStore), połączonym z ulubionymi z MA; nowa stacja trafia na koniec. To repozytorium jest źródłem kolejności dla SUI2 (dispatcher) i SUI7, więc opisz jego API w README.
2. **Przeglądanie:** chipy Język (domyślnie polski, zapamiętany), Gatunek i Kraj, a pod nimi lista stacji w `ResultRow`: logo, nazwa, gatunek · miasto · język, utwór grany teraz (jeśli jest), serce.
3. **Szukanie tylko w radiu:** pole „Szukaj stacji…”.
   - Po wejściu w pole ulubione się chowają, klawiatura ekranowa (ta sama co w Szukaj) wysuwa się z lewej, a wyniki są w dużych wierszach po prawej.
   - `→` z klawiatury przechodzi do wyników, `←` wraca do liter, `Wstecz` zamyka szukanie.
   - Klawiatura fizyczna pisze od razu.
4. Sprawdź w `docs/MA-API.md`, jakie filtry daje MA (Radio Browser: kraj, język, tag). Filtr, którego API nie obsługuje, ukryj i zapisz w „Wynik”. Nie filtruj dużych list po stronie aplikacji.
5. Testy: kolejność ulubionych (dodaj, usuń, przesuń), mapowanie filtrów na zapytanie API.

## Gotowe, gdy
Na emulatorze: zrzut ekranu Radio i zrzut szukania „jaz” z wynikami. PR otwarty.

## Wynik
- Działa na emulatorze (zrzuty: Radio, szukanie „jaz”, przesuwanie stacji): ulubione 1…n, „Przesuń/Usuń” po przytrzymaniu OK, chipy Język/Gatunek/Kraj + Popularne, lista 2-kolumnowa, szukanie tylko w radiu z klawiaturą ekranową i fizyczną.
- API danych: `FavoriteStationsRepository` w `:core:data` (opis w jego README) — `favorites` (kolejność), `move/remove/add`, `step` dla CH±. SUI2 (`ShellViewModel`) nadal ma własną listę w kolejności MA — do przepięcia na to repozytorium (koordynator/SUI7).
- Filtry MA: `music/browse radiobrowser://category/{language|country|tag}/<małe litery>` lub `popularity/popular`; jeden wymiar naraz, cała lista w jednej odpowiedzi (600–1000 stacji, bez stronicowania). Dlatego aktywny jest jeden chip, wybór innego zastępuje poprzedni, „wszystkie” = Popularne. Łączenia filtrów nie robimy po stronie aplikacji. Szukanie = `music/search` (nazwa, limit 30), bez filtrów — chipy chowają się w trybie szukania.
- Niedostępne z API: utwór grany teraz i gatunek/miasto stacji (podtytuł wiersza = wartość aktywnego filtra).
- Nowe: `RadioApi` (`:core:ma-api`), `OnScreenKeyboard` w `:core:design` (Szukaj może go przejąć zamiast własnej kopii).
- Pułapki: serce na liście = przytrzymanie OK (osobny cel fokusu w `ResultRow` nie istnieje). `lintDebug` w worktree failuje na `local.properties` (PropertyEscape, plik poza repo) — nie na kodzie; build, release i testy przechodzą. Chip „Popularne” przy wąskim ekranie bywa przycięty.
