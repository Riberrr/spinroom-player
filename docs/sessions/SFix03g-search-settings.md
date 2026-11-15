# SFix03g — Szukaj i Ustawienia: fokus, chipy, klawiatura
**Model:** Haiku · **Gałąź:** `fix-search-settings` · **Moduły:** `:feature:search` (`SearchScreen.kt`), `:feature:settings` (`SettingsScreen.kt`, `SettingsTextField.kt`). Zależy od SFix03b (przeczytaj jego „Wynik”).

## Błędy (emulator)
1. Szukaj: ← z klawisza „a” (lewa krawędź klawiatury) przenosi fokus na niewidoczny element po prawej. Widać tylko pomarańczową kreskę paska przewijania wyników. Menu otwiera się dopiero za drugim ←. Kolumna wyników nie może być fokusowalna, gdy nie ma wyników.
2. Szukaj: rząd chipów filtrów jest ucięty przy prawej krawędzi i nachodzi na niego pasek przewijania. Chipy mają się przewijać poziomo (`LazyRow`) z odstępem od paska.
3. Szukaj bez połączenia: po wpisaniu litery „Szukam…” w nieskończoność. Ma być „Brak połączenia z serwerem”.
4. Ustawienia: po „Zaloguj” fokus ma trafić na „Wyloguj” (albo pierwszy element sekcji „Odtwarzacz”), a nie uciekać.
5. Ustawienia: pole „Adres serwera” otwiera zwykłą klawiaturę tekstową. Ustaw `KeyboardType.Uri`.
6. Ustawienia: lista odtwarzaczy ma dwa identyczne „Telewizor”. Dodaj drugą linię (model lub adres z danych gracza MA), jeśli MA ją podaje.

## Zadania
Punkty 1–6, każdy osobno i lokalnie. Test JVM tylko dla logiki stanu offline w `SearchViewModel`. Reszta: `@Preview`. Bez emulatora, chyba że punkt 1 nie da się sprawdzić inaczej (wtedy poproś koordynatora).

## Gotowe, gdy
`bash scripts/check.sh` czyste, PR otwarty.

## Wynik
_(uzupełnia sesja)_
