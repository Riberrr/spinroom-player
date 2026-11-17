# SFix — Poprawka z testów na TV (szablon)
**Model:** Sonnet (Opus dla dźwięku/synchronizacji) · **Gałąź:** `fix-<temat>`

Koordynator kopiuje ten plik jako `SFix-<temat>.md` i wpisuje opis błędu od użytkownika.

## Błąd
Radio → „Szukaj stacji…” → „jaz”: brak filtrów (język/gatunek/kraj) i podtytułów (gatunek · miasto) względem makiety 04A; duplikaty (np. „- 0 N - Jazz on Radio” dwa razy); obce stacje przed polskimi.

## Zadania
1. Odtwórz błąd: test jednostkowy albo `adb logcat -d -s Spinroom:* AndroidRuntime:E | tail -80`.
2. Minimalna poprawka w module, którego dotyczy; test, który by ten błąd złapał.
3. Nie poprawiaj „przy okazji” innych rzeczy — zapisz je w „Wynik”.

## Gotowe, gdy
Test zielony, PR otwarty; koordynator wydaje wersję patch (`v0.x.y+1`).

## Wynik
- Wyniki szukania: deduplikacja po nazwie (bez wielkości liter) i uri; kolejność: ulubione → polskie → reszta (`orderSearchResults`, `RadioSearch.kt`, test `RadioSearchTest`). Polskie = stacje z listy Radio Browser „język: polski” (pobierana raz).
- Nowy chip „Język: wszystkie / polski” nad wynikami (przełącza pokazywanie samych polskich). Polskie stacje mają podtytuł „polski”.
- Niemożliwe z danych MA: gatunek i miasto stacji (elementy radiobrowser z MA ich nie niosą), więc brak podtytułu „Gatunek · Miasto · PL”, chipów Gatunek/Kraj w szukaniu (serwer nie łączy filtrów, a wyniki nie mają kraju/gatunku), oraz utworu granego teraz.
- Nie sprawdzono wizualnie na emulatorze (tylko testy JVM).
