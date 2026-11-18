# SFix-UI5 — Radio i Szukaj: fokus D-pada
**Gałąź:** `claude/fix-ui5-radio-focus-4d8584` · **Moduły:** `:feature:radio`, `:feature:search` (+ drobny dodatek w `:core:design`)

## Błąd
Po teście v1.1.0 na TV: (1) Radio — D-padem nie da się zejść z pola „Szukaj stacji…”/chipów do siatki stacji pod spodem; (2) Szukaj (i wyszukiwanie w Radiu) — nie da się przejść z klawiatury/pola na listę wyników.

## Przyczyna
`SpinroomShell` opakowuje ekran w `Column(...).focusRestorer()`. `focusRestorer` dziedziczy `onEnter` na każdy cel fokusu poniżej, który nie ma bliższej grupy. Gdy fokus ląduje na pozycji listy spoza grupy (z pola, chipa, klawisza), `requestFocus` jest przekierowywane na „ostatnio sfokusowane dziecko” (pierwszy ulubiony kafel). Dlatego geometryczne ↓ i `focusProperties { down/right = … }` kończyły na kaflu 1 (albo wcale). Z kafla ulubionych działało, bo przekierowanie jest jednorazowe.

## Zadania (zrobione)
1. `core/design/FocusJump.kt`: `Modifier.focusJump(key, target)` — klawisz przenosi fokus do `target` (null = nic, więc pusta lista nie przechwytuje); żądanie powtórzone po klatce, żeby ominąć przekierowanie `focusRestorer`.
2. Radio: ↓ z pola/chipów (tryb zwykły) i z chipa „Język” (tryb wyszukiwania) → pierwsza stacja; w trybie wyszukiwania → z ostatniej kolumny klawiatury i ↓ z dolnego rzędu → pierwszy wynik. ← z wyników → klawiatura (geometrycznie, sprawdzone).
3. Szukaj: to samo dla klawiatury (prawa kolumna, dolny rząd) i chipów kategorii; pusta lista nie bierze fokusa (`null` target).
4. `OnScreenKeyboard` (`:core:design`): nowy opcjonalny parametr `resultsTarget`.

## Wynik
- Sprawdzone na emulatorze (uiautomator): Radio ulubione → pole → siatka (2 rzędy) i z powrotem ↑; Radio→Szukaj „jaz”: → z ostatniej kolumny na pierwszy wynik, ← wraca; Szukaj „jaz”: → z kolumny 7, ↓ z dolnego rzędu, ↓ z chipów kategorii → pierwszy wynik; ← z przycisków i wierszy wraca do klawiatury.
- Brak testu JVM: to zachowanie fokusu Compose (bez infrastruktury testów UI w repo); logika kolejności nie jest czysta.
- Pułapka dla przyszłych ekranów: lista pod wierszem kontrolek nie dostanie fokusu przez `focusProperties`/geometrię; użyj `focusJump`. Docelowo można zdjąć `focusRestorer` z `SpinroomShell` (poza zakresem; z nim rośnie ryzyko tego samego błędu na innych ekranach).
- Nie ruszone: `SpinroomShell`, obsługa Play/Pause (SFix-UI4).
