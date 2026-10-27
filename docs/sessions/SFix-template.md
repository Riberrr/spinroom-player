# SFix — Poprawka z testów na TV (szablon)
**Model:** Sonnet (Opus dla dźwięku/synchronizacji) · **Gałąź:** `fix-<temat>`

Koordynator kopiuje ten plik jako `SFix-<temat>.md` i wpisuje opis błędu od użytkownika.

## Błąd
_(co się stało, na jakim ekranie, kroki, wersja APK)_

## Zadania
1. Odtwórz błąd: test jednostkowy albo `adb logcat -d -s Spinroom:* AndroidRuntime:E | tail -80`.
2. Minimalna poprawka w module, którego dotyczy; test, który by ten błąd złapał.
3. Nie poprawiaj „przy okazji” innych rzeczy — zapisz je w „Wynik”.

## Gotowe, gdy
Test zielony, PR otwarty; koordynator wydaje wersję patch (`v0.x.y+1`).

## Wynik
_(uzupełnia sesja)_
