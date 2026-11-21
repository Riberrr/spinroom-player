# SFix-UI14 — Powłoka: panel odtwarzacza u góry, menu boczne
**Model:** Opus high · **Gałąź:** `sfix-ui14-shell` · **Plan:** `docs/plans/PLAN-1.2.0.md`

## Zakres modułów
`:app`, `:core:design` (wyłącznie ta sesja). Sesje równoległe: `:feature:radio`+`:core:data` (nowe pliki), `:feature:search` — nie ruszaj.

## Błąd / życzenie (od użytkownika)
Punkty 1–4, 6, 7 z sekcji SESJA B planu (tytuł w dolnym panelu, przyciski prev/play/next w zminimalizowanym, menu → widok główny z czyszczeniem stosu, panel u góry stały ~56 dp z rozwijaniem nakładką i podpowiedzią klawisza, ikona+pastylka odtwarzacza w menu z listą odtwarzaczy, usunięcie 'Przypięte', 'Odtwarzacz' otwiera pełny ekran). Pełny opis w planie.

## Zadania
1. Zrealizuj punkty powyżej minimalnie, wg konwencji z CLAUDE.md (UI po polsku, kod i commity po angielsku).
2. Testy jednostkowe tam, gdzie logika; test na emulatorze (blokada: `SPINROOM_EMU_LOCK=C:/Users/lukas/AppData/Local/Temp/spinroom-emu-<nr>.lock bash scripts/emu-lock.sh acquire <SESSION_ID>`, zawsze `adb -s <nr>`; emulator-5554 (API 31)). Przed testem `wm density` (docelowo 480 na 4K / 240 na 1080p); jeśli inna, zapisz w "Wynik", nie zmieniaj ustawień. TV 10.0.0.6 nie ruszaj. Login MA z pamięci `ma-test-server`; nie zapisuj go w plikach ani logach.
3. Nie ruszaj modułów innych sesji. Zauważone inne błędy zapisz w "Wynik".
4. Na końcu: "Wynik" (5–10 linii), linia w `docs/DECISIONS.md`, README zmienionych modułów, PR (nie merguj), `git merge origin/main` przy konflikcie (bez rebase/force-push).

## Gotowe, gdy
Testy zielone (`bash scripts/check.sh`), PR otwarty, zachowanie sprawdzone na emulatorze.

## Wynik
_(uzupełnia sesja)_
