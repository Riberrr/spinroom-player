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
- Ustawienia w dwóch kolumnach (`SettingsCategory`): Połączenie (serwer, logowanie, nazwa odtwarzacza, lista graczy), Odtwarzanie (start z TV, opóźnienie), Wygląd, Przyciski pilota (zaślepka dla SUI8b), Ekran główny, O aplikacji. Fokus na kategorii pokazuje jej treść.
- Wygląd: tło odtwarzacza (blur/color/solid, nowe `Appearance.playerBackground`), motyw, 5 akcentów, wygaszacz (domyślnie wyłączony), podgląd. `MainActivity` czyta `AppearanceSettings` (motyw i akcent zmieniają się od razu w całej aplikacji); wygaszacz włącza się tylko przy fladze.
- Ekran główny: `HomeLayoutSettings` (`HomeRows` stations/playlists/recent, kolejność + widoczność). **Do zrobienia przez Start:** `HomeViewModel/HomeScreen` mają czytać `HomeLayout`; NowPlaying ma czytać `playerBackground` (nie robiłem, poza zakresem).
- Nazwa odtwarzacza: walidacja `PlayerName.validate` (1–40 znaków po trim). Mechanizm: zapis w `PlayerNameStore`; `PlaybackHost.playerName()` → `SendspinReceiver` wysyła ją w `client/hello.name` i mDNS `name`; `MainActivity` restartuje usługę po zmianie. Dodatkowo próba `config/players/save` (tylko admin MA; konto TV dostaje „no permission”, to tylko log).
- Sprawdzone na emulatorze: log `started as "Kuchnia"` i mDNS `Kuchnia`. **Niesprawdzone:** czy MA pokazuje nową nazwę, bo MA nie łączy się z emulatorem (gracz TV w MA to inne urządzenie). Do sprawdzenia na TV 10.0.0.6; MA może zachować starą nazwę dla już zarejestrowanego gracza.
- Pułapki: lint na lokalnym `local.properties` z `sdk.dir` zgłasza PropertyEscape (nie dotyczy repo); debug ma applicationId `.debug`; menu boczne ma teraz ~10 pozycji do „Ustawień”.
- Zmiany poza zakresem (minimalne, do przejrzenia): `:app` (`MainActivity`, `SpinroomApp`), `:core:audio` (`PlaybackHost.playerName`, `SendspinReceiver.customName`, `SpinroomPlaybackService.restart`).
