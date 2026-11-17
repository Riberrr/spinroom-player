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
- **Działa:** klawiatura ekranowa jak dotąd + klawiatura fizyczna (znaki i Spacja trafiają do pola bez fokusu, Backspace usuwa, Enter wyszukuje od razu; podpowiedź „Masz klawiaturę? Po prostu pisz”). Kategorie: Wszystko, Radio, Playlisty, Albumy, Wykonawcy, Utwory; chip „Filtry · N” otwiera `FilterPanel` z SUI5 (Tylko ulubione, Dekada, Źródło z bieżących wyników), aktywne filtry jako chipy z ✕ (OK usuwa). Tło `ContentBackdrop` z okładki/logo zaznaczonego wyniku, szkło, kwadratowe miniatury stacji (inicjały na kolorze).
- **Ulubione:** zaznaczony wiersz pokazuje „▶ Odtwórz” i serce (→ z wiersza); wypełnione serce = już w ulubionych, ponowne OK usuwa; długie OK na wierszu robi to samo; na dole komunikat „Dodano/Usunięto z ulubionych: …” z „Cofnij” (5 s, osiągalne ↓). Błąd serwera cofa zmianę i pokazuje komunikat.
- **API:** `SearchViewModel` (`onToggleFavorite`, `onUndo`, `onSelect`, `onPhysicalInput`, `onToggleSource/Decade/FavoritesOnly`, `onPanel`), `mapPhysicalKey` (`PhysicalKeys.kt`, czyste, testowane), `SearchBackend.setFavorite/isFavorite` (przez `FavoritesApi`).
- **Odstępstwa:** SUI4 niescalone → tylko ulubione MA, bez kolejności stacji. Karta „Najlepszy wynik” usunięta (makieta jej nie ma); w Szukaj długie OK to ulubione, nie menu „Do kolejki”. Serce w wierszu jest samą ikoną (przycisk z tekstem „Do ulubionych” nie mieści się obok tytułu przy 1280 dp). Filtr „Gatunek” pominięty (wyniki bez gatunku).
- **Pułapki:** wyniki z providerów mają `favorite=false`; prawdziwy stan dociągamy po zaznaczeniu wiersza, więc serce pojawia się z krótkim opóźnieniem. Z klawiatury ekranowej → z prawej kolumny idzie jawnie do pierwszego wyniku (`focusProperties.right`) — bez tego fokus lądował na niewidocznym kontenerze listy. Zrzuty z emulatora bywają o jedną klatkę spóźnione: po ostatnim klawiszu wyślij jeszcze `0` (KEYCODE_UNKNOWN). Pakiet debug to `...spinroom.debug` (osobny od release).
- **Dalej:** po scaleniu SUI4 dopiąć kolejność stacji dla nowych ulubionych; przenieść `FilterPanel` do `:core:design`.
