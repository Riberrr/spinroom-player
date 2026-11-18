# SFix-UI6 — Radio: zwijany pasek ulubionych, Szukaj: kompaktowy przycisk Odtwórz
**Model:** Sonnet · **Gałąź:** `claude/fix-ui6-radio-search-layout-f68b32`

## Błąd
Zgłoszenie po teście na TV:
1. Radio: pasek ulubionych (~296 dp) zabiera miejsce liście stacji, gdy fokus jest już niżej.
2. Szukaj: pomarańczowy „▶ Odtwórz” zasłania nazwę wyniku (np. „Blasmusik…”).

## Zadania
1. `:feature:radio`: gdy fokus jest na polu szukania / chipach / siatce, kafle ulubionych płynnie (200 ms) zmniejszają się do małych ikon z numerem; powrót fokusu na górę przywraca pełny rozmiar.
2. `:feature:search`: przycisk Odtwórz tylko z ikoną ▶; serce zostaje; na zaznaczonym wierszu znaczek źródła i serce ulubionego znikają (są przyciski).

## Gotowe, gdy
Emulator: D-pad góra/dół/prawo działa, fokus nie ginie; PR otwarty.

## Wynik
- Radio: `Favorites(compact)` animuje `size` istniejącego `StationTile` (148→52 dp) i wysokość pudełka (podpisy ucięte), więc to ten sam fokusowalny węzeł — fokus się nie gubi. Stan `lowerFocused` = `hasFocus` na kolumnie z `Controls` + `StationList`. Przy otwartym menu bocznym pasek się rozwija (fokus poza treścią).
- Własny `Shape` (`FocusRoomClip`) zamiast `clipToBounds`, bo ten ucinał obwódkę fokusu.
- Szukaj: `ActionButton(null, primary)` = sama ikona, z `contentDescription`.
- Sprawdzone na emulatorze: ↓ z ulubionych zwija, ↑ rozwija, siatka przewija się, wiersz Szukaj + ▶ + serce (→).
- Pułapka: debug ma `applicationIdSuffix=.debug`; `monkey` odpala wersję release.
