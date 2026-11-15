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
PR #32 (gałąź `fix-search-settings`). Emulator (release, instalacja `-r` nad istniejącą, bez logowania): **punkt 1 i 3 potwierdzone**: jedno ← z „a” otwiera menu (Szukaj podświetlone), bez niewidocznego celu po prawej; offline po wpisaniu litery → „Coś poszło nie tak / Brak połączenia z serwerem / Spróbuj ponownie”. Punkt 2: chipy mieszczą się w kolumnie, ostatni chip ucięty przez przewijanie (LazyRow). **Niezweryfikowane na emulatorze**: punkt 4 (wymaga logowania, którego nie wykonywałem), punkt 5 (klawiatura Uri nie sprawdzona), punkt 6 (lista odtwarzaczy pusta: serwer MA nieosiągalny z emulatora, „Łączenie…”). `assembleDebug assembleRelease testDebugUnitTest` zielone; `lintDebug` lokalnie pada tylko na `local.properties` (PropertyEscape, ścieżka SDK), nie na kodzie.
- Punkt 3 (offline): timeout z `MaClient` (CancellationException przy aktywnej korutynie) → stan `failed`, tekst „Brak połączenia z serwerem”. Test JVM `offlineTimeoutEndsInFailedStateNotSpinner`.
- Punkt 1 (← z „a”): wyniki nie są fokusowalne bez wyników (`focusProperties { canFocus = state.hasShown }`). Niezweryfikowane; przyczyna nie potwierdzona na urządzeniu.
- Punkt 2 (chipy): `LazyRow` z `contentPadding(end = 32.dp)`. Niezweryfikowane wizualnie.
- Punkt 4 (logowanie): po przejściu na zalogowany fokus na „Wyloguj” (`logoutFocus`, tylko na przejściu).
- Punkt 5 (adres): `KeyboardType.Uri` przez parametr `SettingsTextField.keyboardType`; login i hasło bez zmian.
- Punkt 6 (odtwarzacze): druga linia `playerDetail()` = model · adres z `device_info`; brak MA-owych danych = brak linii. Zmiana w `:core:ma-api` (`Player.deviceInfo`, `PlayerDeviceInfo`); jeśli MA poda inny kształt `device_info`, dekodowanie gracza może się nie udać. Sprawdzić na prawdziwym MA.
Co dalej: weryfikacja punktów 1, 2, 4–6 na emulatorze po przydziale. @Preview nie dodane.
Pułapki: `local.properties` na Windows wymaga `C\:/` dla lint; `:app:packageDebug` raz padł (IncrementalSplitterRunnable), przy ponowieniu przeszedł.
