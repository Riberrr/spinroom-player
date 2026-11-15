# SFix04c — Teraz odtwarzane, Szczegóły, Start: drobne poprawki UI
**Model:** Haiku · **Gałąź:** `fix-ui-polish` · **Moduły:** `:feature:nowplaying`, `:feature:details`, `:feature:home` (tylko kafelek stacji), `:core:design` (tylko jeśli konieczne). Źródło: raport SFix03v (błędy 6 i 7).

## Błędy (emulator, release)
1. Teraz odtwarzane (radio): obok siebie „pokaż całą kolejkę (1)” i „Kolejka jest pusta”. Ma być spójnie: przy braku kolejki bez linku do niej, albo licznik zgodny z kolejką.
2. Start, ulubione stacje: „Radio RMF” ma tylko ikonę nutki zamiast logo (inne stacje mają logo). Sprawdź, czy brak obrazu w MA (wtedy OK, ikona zastępcza) czy błąd wybierania obrazka w kodzie (`image`/`images`).
3. Szczegóły playlisty: etykieta przycisku „Dodaj do…” jest ucięta. Skróć do „Dodaj do” lub zmniejsz padding/pozwól na szerszy przycisk (bez łamania w dwie linie, 1280×720).

## Zadania
Punkty 1–3, lokalne, minimalne. Test JVM tylko jeśli punkt 1 ma logikę. Reszta: `@Preview`/rozumowanie; emulator tylko jeśli konieczny (poproś koordynatora o przydział). Nie dotykaj innych ekranów.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554: jedna sesja naraz. Testuj wariant release, `-r`, bez odinstalowania (wyloguje użytkownika Tv). Nie wpisuj danych logowania. Nie uruchamiaj odtwarzania.

## Gotowe, gdy
`bash scripts/check.sh` czyste (lint może paść na `local.properties` — środowisko), PR otwarty, nie scalony.

## Wynik
_(uzupełnia sesja)_
