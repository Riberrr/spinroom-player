# Przebudowa interfejsu (zatwierdzona 16.11.2026)

Wynik czterech rund makiet. Makiety w `design/concepts/` (otwierać w przeglądarce):
- `uklady-v3.html`: 01, 04A, 06, 09A, 09B, 10A, 10C, 10D
- `uklady-v4.html`: 03B-1, 04A z klawiaturą, 06 z ulubionymi, 09B z nazwą odtwarzacza, 10B z miniaturami, styl 09A w całej aplikacji
- `uklady-v2.html`: 05 Playlisty, 08 Lista z podglądem (bez zmian od rundy 2)
- `uklady.html` (runda 1) odrzucona w całości, zostaje tylko jako historia.

Kierunek: zostaje obecny język wizualny (wąskie menu z ikonami, karty, bursztynowy fokus, mini-odtwarzacz na dole). Nie budujemy nowej metafory, tylko poprawiamy ergonomię i dodajemy jeden wspólny styl „szkła” z rozmytym tłem.

## Wspólny styl (09A we wszystkich ekranach)
- **Tło z treści**: rozmyty kolor z okładki lub logo zaznaczonego elementu, przyciemniony do ~60%. Zmiana płynna, z opóźnieniem ~0,3 s.
- **Szkło**: karty, menu i mini-odtwarzacz są półprzezroczyste (biel 6–12% na ciemnym tle) zamiast pełnego `surface`.
- **Fokus bez zmian**: biała obwódka i akcent z motywu.
- **Kształty**: stacje radiowe i muzyka mają **kwadratowe** miniatury o tym samym rytmie kafli (zamiast inicjałów na kolorze, gdy brak logo).
- **Wyłącznik**: Ustawienia → Wygląd → „Jednolite tło” wyłącza rozmycie wszędzie.
- **Wydajność**: rozmyta kopia obrazka liczona raz przy zmianie (mała bitmapa), nie na żywo. `Modifier.blur` działa dopiero od API 31, a minSdk to 26.

## Ekrany
| Nr | Ekran | Co ma być |
| --- | --- | --- |
| 01 | Menu boczne | Wąskie jak dziś. Po `←` rozwija się z podpisami i listą „Przypięte” (5 ulubionych). Osobne pozycje: Start, Radio, Playlisty, Biblioteka, Szukaj, Ustawienia. |
| 03B-1 | Start | Mini-odtwarzacz na dole (bez kafla „Teraz gra” z 03A). Rzędy: Ulubione stacje (kwadrat + nazwa, 8 w rzędzie) → Ulubione playlisty (okładka z 4 albumów) → Ostatnio odtwarzane. Fokus startuje na 1. stacji. „Wszystkie…” na końcu rzędu. Kolejność rzędów w Ustawieniach → Ekran główny. |
| 04A | Radio | Kafle ulubionych z numerem (kolejność = CH+/CH−, zmiana przytrzymaniem OK). Szukanie tylko w radiu, filtry: język (domyślnie polski, zapamiętany), gatunek, kraj. Po wejściu w szukanie ulubione się chowają, klawiatura wysuwa się z lewej, wyniki w dużych wierszach po prawej (logo, nazwa, gatunek · miasto · język, utwór teraz, serduszko). |
| 05 | Playlisty | Własny ekran, sortowanie „Ostatnio grane”, okładka z 4 albumów. Pod zaznaczoną „Odtwórz” i „Losowo”. |
| 06 | Szukaj | Klasyczna klawiatura ekranowa jak dziś. Klawiatura fizyczna pisze od razu (bez wchodzenia w pole). Kategorie: Wszystko, Radio, Playlisty, Albumy, Wykonawcy, Utwory. Filtry (gatunek, dekada, źródło) z punktu 07. Zaznaczony wynik ma „Odtwórz” i „Do ulubionych”, wypełnione serce = już w ulubionych, komunikat z „Cofnij”. |
| 08 | Biblioteka | Widok listy z podglądem zaznaczonego albumu po prawej (utwory, Odtwórz, Losowo, ♡). Przełącznik Siatka / Lista. Filtry z 07 także tutaj. |
| 09A | Wysuwany odtwarzacz | Dolny pasek rozwija się w panel po najechaniu fokusem albo po przytrzymaniu przypisanego przycisku z dowolnego ekranu. Lewa część: okładka, sterowanie. Prawa: kolejka (muzyka) albo ulubione stacje (radio). Wstecz zamyka i oddaje fokus tam, gdzie był. |
| 09B | Ustawienia | Kategorie: Połączenie (serwer, **nazwa odtwarzacza** z podpowiedziami), Odtwarzanie, Wygląd, Przyciski pilota, Ekran główny, O aplikacji. Przyciski pilota: lista „klawisz → akcja → zasięg”, dodawanie skrótu przez „naciśnij przycisk”, ostrzeżenie o kolizji z funkcją TV, przełącznik „Działaj poza aplikacją”. |
| 10A | Odtwarzacz: muzyka | Proporcje 40:60, rozmyte tło z okładki, sterowanie i fragment kolejki (4 pozycje). CH+/CH− = następny / poprzedni utwór. |
| 10B | Odtwarzacz: radio | Ten sam układ, bez paska postępu (kraj, język, jakość). W miejscu kolejki ulubione stacje: numer + kwadratowa miniatura + nazwa + utwór teraz. ⏮ ⏭ i CH± = poprzednia / następna ulubiona stacja. |
| 10C | Poza aplikacją | Gdy aplikacja jest w tle, CH+/CH− przełączają utwór lub stację, a w rogu na 3 s pojawia się mała karta, która nie przejmuje fokusu. |
| 10D | Wygląd | Tło odtwarzacza: rozmyta okładka / kolor z okładki / jednolite. Motyw: Grafit, Granat, Czerń (OLED). Kolor akcentu: 5 do wyboru, domyślnie bursztyn. Wygaszacz z okładką **wyłączony**, bo odtwarzacz zostaje na ekranie. |

## Ryzyka techniczne
- **CH+/CH− w tle (10C)**: Android TV nie przekazuje tych klawiszy aplikacji w tle. Jedyna droga to `AccessibilityService` z filtrowaniem klawiszy, którą użytkownik włącza w ustawieniach TV. Google Play ogranicza takie użycie, więc wymaga deklaracji albo dystrybucji przez APK z GitHuba. Play / pauza / następny działają już przez MediaSession, bez zgód.
- **Karta nad inną aplikacją (10C)**: `SYSTEM_ALERT_WINDOW`, na części Google TV ustawienie jest ukryte. Plan B: powiadomienie.
- **Nadpisywanie klawiszy TV (09B)**: w aplikacji przez `onKeyDown` / `onKeyLongPress`. Klawiszy Home i części klawiszy producenta nie da się przechwycić w żadnym trybie.
- **Wygaszacz (S14a)**: domyślnie wyłączony. Ekran ma nie gasnąć, gdy odtwarzacz jest na wierzchu (`keepScreenOn`).

## Podział na sesje
Aktualny plan, fale, modele i prompt koordynatora: `docs/UI-SESSIONS.md`. Pliki sesji: `docs/sessions/SUI*.md`. PNG makiet dla sesji: `design/concepts/png/` (wyrenderowane z HTML przez `#solo-N`). Tabela niżej to pierwszy szkic, zastąpiony przez `docs/UI-SESSIONS.md`.

| Sesja | Zakres | Zależy od |
| --- | --- | --- |
| SUI1 | `:core:design`: tokeny szkła, rozmyte tło z bitmapy, motywy i akcent, wspólne kafle (kwadratowa stacja, okładka z 4 albumów, wiersz wyniku z sercem) | — |
| SUI2 | `:app`: nowe menu boczne (01), trasy Radio i Playlisty, globalny mini-odtwarzacz rozwijany (09A) | SUI1 |
| SUI3 | `:feature:home`: Start 03B-1 | SUI1 |
| SUI4 | Ekran Radio (04A) w nowym module `:feature:radio`, repozytorium ulubionych stacji z kolejnością | SUI1, SUI2 |
| SUI5 | Playlisty (05) i Biblioteka lista + podgląd + filtry (08, 07) | SUI1 |
| SUI6 | Szukaj (06): klawiatura fizyczna, kategorie, filtry, dodawanie do ulubionych | SUI1 |
| SUI7 | `:feature:nowplaying`: 10A i 10B, CH± w aplikacji | SUI1, SUI4 (kolejność ulubionych) |
| SUI8 | `:feature:settings`: kategorie, nazwa odtwarzacza, Wygląd (10D), Przyciski pilota (09B) | SUI1 |
| SUI9 | Sterowanie poza aplikacją (10C): AccessibilityService i karta w rogu, za przełącznikiem z SUI8 | SUI7, SUI8 |

SUI3, SUI5, SUI6 i SUI8 mogą iść równolegle po SUI1. SUI9 na końcu, bo niesie ryzyko dla Google Play.
