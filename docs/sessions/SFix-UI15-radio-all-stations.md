# SFix-UI15 — Radio: wszystkie stacje z MA
**Model:** Sonnet medium · **Gałąź:** `sfix-ui15-radio` · **Plan:** `docs/plans/PLAN-1.2.0.md`

## Zakres modułów
`:feature:radio` + nowe pliki w `:core:data`. Nie ruszaj `:app`, `:core:design`, `:feature:search`.

## Błąd / życzenie (od użytkownika)
Punkt 5: 'Wszystkie stacje' ma pokazywać wszystkie stacje radiowe zapisane w MA (nie tylko ulubione) + przeglądanie innych źródeł jak dziś. Ulubione na górze; duże listy porcjami (paging) lub grupowane A-Z; D-pad bez zacięć.

## Zadania
1. Zrealizuj punkty powyżej minimalnie, wg konwencji z CLAUDE.md (UI po polsku, kod i commity po angielsku).
2. Testy jednostkowe tam, gdzie logika; test na emulatorze (blokada: `SPINROOM_EMU_LOCK=C:/Users/lukas/AppData/Local/Temp/spinroom-emu-<nr>.lock bash scripts/emu-lock.sh acquire <SESSION_ID>`, zawsze `adb -s <nr>`; emulator-5556 (API 34, współdzielony z SFix-UI16 przez blokadę)). Przed testem `wm density` (docelowo 480 na 4K / 240 na 1080p); jeśli inna, zapisz w "Wynik", nie zmieniaj ustawień. TV 10.0.0.6 nie ruszaj. Login MA z pamięci `ma-test-server`; nie zapisuj go w plikach ani logach.
3. Nie ruszaj modułów innych sesji. Zauważone inne błędy zapisz w "Wynik".
4. Na końcu: "Wynik" (5–10 linii), linia w `docs/DECISIONS.md`, README zmienionych modułów, PR (nie merguj), `git merge origin/main` przy konflikcie (bez rebase/force-push).

## Gotowe, gdy
Testy zielone (`bash scripts/check.sh`), PR otwarty, zachowanie sprawdzone na emulatorze.

## Wynik
**Zrobione:** domyślna lista na ekranie Radio to teraz „Moje stacje" — wszystkie stacje radiowe z biblioteki MA (nie tylko ulubione), A–Z, porcjami po 100 (kolejna porcja gdy fokus jest ≤12 wierszy od końca). Ulubione zostają na górze; chipy Język/Gatunek/Kraj i „Popularne" przełączają na Radio Browser jak dotąd. Pusta biblioteka → zapisany filtr.
**API:** `RadioLibraryRepository.page(offset)` + `append` (nowy plik w `:core:data`, moduł Hilt `RadioLibraryModule`); `RadioUiState.libraryMode`, `RadioViewModel.onAllStations/onNearEnd`.
**Test:** `RadioLibraryTest` (dedup). Emulator-5556, density 240 @1080p: lista „Moje stacje" widoczna. Biblioteka testowa ma ~10 stacji, więc dociągania stron nie widać w praktyce (logika sprawdzona tylko kodem).
**Uwagi:** `scripts/check.sh` padł tylko na lint `PropertyEscape` w lokalnym `local.properties` (środowisko, nie kod); kompilacja i testy OK. Nie zapisuję wyboru trybu między startami (zawsze zaczyna od „Moje stacje").
