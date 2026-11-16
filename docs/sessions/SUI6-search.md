# SUI6 — Szukaj: klawiatura fizyczna, kategorie, filtry, ulubione
**Model:** Sonnet · **Gałąź:** `sui6-search` · **Moduły:** `:feature:search`. Zależy od: SUI1, SUI5 (`FilterPanel`).
**Makiety:** `png/06-szukaj.png`, `png/06-szukaj-ulubione.png`.

## Zadania
1. **Klawiatura ekranowa** jak dziś: polskie znaki, Spacja, Usuń, 123, mikrofon.
2. **Klawiatura fizyczna** (USB, Bluetooth, pilot z klawiaturą): znaki trafiają do pola bez wchodzenia w nie, `Enter` zatwierdza, `Backspace` usuwa. Podpowiedź „Masz klawiaturę? Po prostu pisz”.
3. **Kategorie:** Wszystko, Radio, Playlisty, Albumy, Wykonawcy, Utwory. Przycisk „Filtry · N” otwiera `FilterPanel` z SUI5; aktywne filtry widać jako chipy z ✕.
4. **Ulubione:**
   - zaznaczony wynik ma przyciski „▶ Odtwórz” i „♡ Do ulubionych”;
   - wypełnione serce = wynik już jest w ulubionych, ponowne naciśnięcie go usuwa;
   - przytrzymanie `OK` robi to samo;
   - po zmianie na dole pojawia się komunikat z „Cofnij” (~5 s);
   - nowa stacja trafia na koniec kolejności z SUI4. Jeśli SUI4 nie jest jeszcze scalona, użyj samych ulubionych MA i zapisz to w „Wynik”.
5. Testy ViewModelu: dodawanie i usuwanie z ulubionych z cofnięciem, filtrowanie po kategorii, wpisywanie z klawiatury fizycznej (mapowanie KeyEvent → tekst).

## Gotowe, gdy
Na emulatorze zrzuty: wyniki „jazz” z zaznaczonym wynikiem i przyciskami oraz komunikat po dodaniu do ulubionych. PR otwarty.

## Wynik
_(uzupełnia sesja)_
