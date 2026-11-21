# SFix-UI17 — Odtwarzacz: płynna zmiana okładki
**Model:** Sonnet medium · **Gałąź:** `sfix-ui17-cover` · **Plan:** `docs/plans/PLAN-1.2.0.md`

## Zakres modułów
`:feature:nowplaying` (start po scaleniu SFix-UI14).

## Błąd / życzenie (od użytkownika)
Punkt 8: przy zmianie utworu (zwł. radio) między okładką A i B pojawia się trzecia, systemowa okładka z inicjałami na jednolitym tle. Ma jej nie być: okładka A zostaje, dopóki nowa się nie załaduje (bez placeholdera); MA przy utworze B zwraca domyślną okładkę stacji, więc nie trzeba timera. Awatar z inicjałami tylko gdy stacja nie ma żadnego logo.

## Zadania
1. Zrealizuj punkty powyżej minimalnie, wg konwencji z CLAUDE.md (UI po polsku, kod i commity po angielsku).
2. Testy jednostkowe tam, gdzie logika; test na emulatorze (blokada: `SPINROOM_EMU_LOCK=C:/Users/lukas/AppData/Local/Temp/spinroom-emu-<nr>.lock bash scripts/emu-lock.sh acquire <SESSION_ID>`, zawsze `adb -s <nr>`; emulator-5554 (API 31)). Przed testem `wm density` (docelowo 480 na 4K / 240 na 1080p); jeśli inna, zapisz w "Wynik", nie zmieniaj ustawień. TV 10.0.0.6 nie ruszaj. Login MA z pamięci `ma-test-server`; nie zapisuj go w plikach ani logach.
3. Nie ruszaj modułów innych sesji. Zauważone inne błędy zapisz w "Wynik".
4. Na końcu: "Wynik" (5–10 linii), linia w `docs/DECISIONS.md`, README zmienionych modułów, PR (nie merguj), `git merge origin/main` przy konflikcie (bez rebase/force-push).

## Gotowe, gdy
Testy zielone (`bash scripts/check.sh`), PR otwarty, zachowanie sprawdzone na emulatorze.

## Wynik
_(uzupełnia sesja)_
