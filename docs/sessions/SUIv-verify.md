# SUIv — Przegląd całości na emulatorze
**Model:** Sonnet (niski effort) · **Gałąź:** `suiv-verify`, bez zmian w kodzie (zmienia się tylko ten plik). Zależy od: wszystkich sesji SUI.

## Zadania
1. Zainstaluj najnowszy `main` na emulatorze (z blokadą `emu-lock.sh`) i przejdź D-padem:
   1. Start;
   2. Radio, szukanie „jaz”;
   3. Playlisty;
   4. Biblioteka: lista i filtry;
   5. Szukaj: dodanie wyniku do ulubionych;
   6. wysunięcie odtwarzacza z Biblioteki;
   7. pełny odtwarzacz: utwór, potem stacja i `CH+`;
   8. Ustawienia: Wygląd (zmiana akcentu i tła), nazwa odtwarzacza, przyciski pilota.
2. Maksymalnie 10 zrzutów, każdy porównaj z odpowiednim PNG z `design/concepts/png/`.
3. Sprawdź:
   - fokus jest zawsze widoczny;
   - `Wstecz` działa wszędzie;
   - w logcat nie ma crashy;
   - „Jednolite tło” wyłącza rozmycie.

## Gotowe, gdy
„Wynik” zawiera listę różnic i błędów: jeden wiersz = jeden przyszły SFix (ekran, objaw, kroki). PR zawiera tylko ten plik.

## Wynik
Sprawdzone na emulatorze (cold boot, `main` @ 6cb3aa1, APK debug, logowanie OK, MA 2.10.12); w `AndroidRuntime:E` brak crashy. **TV 10.0.0.6:** APK zainstalowany, ale `screencap` na TV nie działa (`open mma dev failed`), a klasyfikator zablokował `am start` i `input keyevent` na TV — z TV nic nie sprawdzono. Zrzutów wyszło ok. 35 (limit 10 przekroczony przy wpisywaniu logowania i błądzeniu D-padem).

**Niesprawdzone (ręczny test lub SFix):** pełny odtwarzacz (utwór/stacja, CH+), dock/mini-player, hold Play/Pause, menu przytrzymania OK, nazwa odtwarzacza w MA (na liście odtwarzaczy widać tylko dwa wpisy „Telewizor”), Szukaj → dodanie do ulubionych, Ustawienia → Wygląd (akcent/tło, „Jednolite tło”), Przyciski pilota, granie. Usługi ułatwień dostępu NIE włączane; kroki dla użytkownika: Ustawienia TV → Dostępność → Usługi → Spinroom → Włącz.

| # | Ekran | Objaw | Kroki |
|---|---|---|---|
| 1 | Start | Brak „Wszystkie stacje →/playlisty →”, brak logo w szynie, chip statusu bez nazwy odtwarzacza („Połączono z serwerem” vs „Salon TV · połączono”), kafle stacji większe (5 w rzędzie zamiast 8), powitanie bez imienia | porównaj z `03B-start.png` |
| 2 | Start | Pusty kafel „Dodaj playlistę do ulubionych” + „Wszystkie playlisty →” zamiast siatki (może wynikać z braku ulubionych) | Start po zalogowaniu |
| 3 | Radio → Szukaj stacji | Brak filtrów (język/gatunek/kraj) i podtytułów (gatunek · miasto); wyniki „jaz” mają duplikaty („- 0 N - Jazz on Radio” x2) i obce stacje przed polskimi | Radio → „Szukaj stacji…” → „jaz”; porównaj z `04A-radio-szukaj.png` |
| 4 | Ustawienia | D-pad Lewo z listy kategorii nie przenosi do szyny (tylko Wstecz → Start) | Ustawienia → Lewo |
| 5 | Biblioteka → Albumy | „Albumy · 0 / Nic tu jeszcze nie ma”, choć Playlisty (12) zwracają dane | Biblioteka → Albumy |
| 6 | Szczegóły albumu | Lista utworów pusta („Nic tu jeszcze nie ma.”); Lewo z przycisków nie wraca do szyny; Back cofa przez stos poprzednich albumów, potem wychodzi z aplikacji; start aplikacji wznawia ostatni ekran szczegółów | otwórz album → inny album → Back |
| 7 | Biblioteka / Szczegóły | Tło jednolicie czarne, bez rozmytego koloru z okładki (styl SUI1 oczekuje tła z treści) | Biblioteka, Szczegóły |
| 8 | Biblioteka → Playlisty | Playlisty systemowe MA mają jako okładkę logo MA (zasada „nie używać logo MA”) | Biblioteka → Playlisty |
| 9 | Ustawienia → Połączenie | Enter w polu tekstowym przenosi fokus na „Nazwa odtwarzacza” i otwiera okno zamiast przejść do następnego pola | wpisz adres, Enter |
| 10 | Emulator | Zimny start debug na przeciążonym emulatorze → ANR `startForegroundService() did not then call startForeground()` w `SpinroomPlaybackService` (znana luka); po cold boot start OK | zimny start debug |

Bez uwag: rozwijana szyna z „Przypięte”, Radio (ulubione z numerami, szukanie klawiaturą ekranową), fokus widoczny na wszystkich oglądanych ekranach, Back działa na oglądanych ekranach.
