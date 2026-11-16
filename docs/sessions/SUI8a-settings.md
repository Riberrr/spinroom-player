# SUI8a — Ustawienia: kategorie, nazwa odtwarzacza, Wygląd, Ekran główny
**Model:** Sonnet · **Gałąź:** `sui8a-settings` · **Moduły:** `:feature:settings`, `:core:data` (nowe pliki). Zależy od: SUI1.
**Makiety:** `png/09B-nazwa-odtwarzacza.png`, `png/10D-wyglad.png`, `png/09B-przyciski.png` (tylko układ kategorii).

## Zadania
1. **Kategorie:** Ustawienia w dwóch kolumnach, kategorie po lewej, treść po prawej. Kategorie: Połączenie, Odtwarzanie, Wygląd, Przyciski pilota, Ekran główny, O aplikacji. Przenieś obecne opcje do właściwych kategorii, niczego nie usuwaj. „Przyciski pilota” na razie jako zaślepka (zrobi SUI8b).
2. **Nazwa odtwarzacza** (w Połączeniu): okno z polem, podpowiedziami (Salon, Sypialnia, Kuchnia, Gabinet, Pokój dziecięcy, „Napisz własną…”) i przyciskami Zapisz / Anuluj. Sprawdź w `docs/SENDSPIN.md` i `docs/MA-API.md`, jak nazwa trafia do MA (nazwa w `client/hello` Sendspin i/lub konfiguracja gracza w MA). MA ma pokazać nową nazwę bez ponownego parowania. Opisz mechanizm w „Wynik”.
3. **Wygląd (10D):**
   - tło odtwarzacza: rozmyta okładka / kolor z okładki / jednolite;
   - motyw: Grafit, Granat, Czerń OLED;
   - akcent: 5 kolorów;
   - wygaszacz ekranu: domyślnie wyłączony;
   - podgląd zmienia się od razu.

   Zapis przez `AppearanceSettings` z SUI1.
4. **Ekran główny:** kolejność rzędów Startu (Ulubione stacje, Ulubione playlisty, Ostatnio odtwarzane) i włączanie / wyłączanie każdego rzędu.
5. Testy: walidacja nazwy (pusta, za długa, same białe znaki), zapis preferencji.

## Gotowe, gdy
- Na emulatorze zrzuty: Wygląd po zmianie akcentu oraz okno nazwy odtwarzacza.
- Po zmianie nazwy MA pokazuje nową nazwę. Wystarczy sprawdzić listę graczy przez API albo w logu, bez otwierania MA w przeglądarce.
- PR otwarty.

## Wynik
_(uzupełnia sesja)_
