# S08 — Teraz odtwarzane (ekran-wzorzec)
**Model:** Sonnet · **Zależy od:** S06, S07 · **Gałąź:** `s08-now-playing`

## Cel
Pierwszy pełny ekran i wzorzec, który kopiują wszystkie kolejne ekrany.

## Przeczytaj
`design/screens/6-teraz-odtwarzane.png`, „Wynik” w `S06-ma-api.md` i `S07-ui-shell.md`, README `:core:design` i `:core:data`.

## Zadania
1. `:feature:nowplaying`: `UiState`, `ViewModel` (Hilt) na `PlayerRepository`, ekran: okładka (Coil), rozmyte tło (API 31+, niżej przyciemnienie), tytuł, postęp liczony lokalnie między zdarzeniami, sterowanie (losowo, poprzedni, play/pauza, następny, powtarzanie, ulubione), podgląd kolejki i pełna kolejka.
2. Podłącz mini-odtwarzacz z powłoki do tego samego stanu.
3. Wybór odtwarzacza: własny odtwarzacz TV, a dopóki go nie ma — wybór z listy odtwarzaczy MA (tylko debug).
4. W README modułu sekcja **„Wzorzec ekranu”**: układ plików, nazewnictwo, jak testować VM. Kolejne sesje kopiują ten wzorzec.
5. Testy VM na fixtures.

## Gotowe, gdy
Z TV sterujesz dowolnym odtwarzaczem MA; zrzut porównany z makietą; testy zielone.

## Nie rób
Innych ekranów; zmian w `:core:design` poza drobnymi poprawkami opisanymi w PR.

## Wynik
- `:feature:nowplaying`: pełny ekran wg `6-teraz-odtwarzane.png` (okładka Coil, rozmyte tło API 31+, tytuł, postęp liczony lokalnie, sterowanie: losowo/poprzedni/play-pauza/następny/powtarzanie/ulubione, „Następne w kolejce” + pełna kolejka z przeskokiem do utworu, chip odtwarzacza i głośności).
- API: `NowPlayingScreen`, `NowPlayingMiniPlayer(onOpen)` (podłączony w `SpinroomShell`, ukryty gdy brak utworu), `activityNowPlayingViewModel()` (jedna instancja VM na activity dla ekranu i mini-playera), `NowPlayingContent` (bezstanowy, z @Preview). Wzorzec ekranu opisany w README modułu.
- Wybór odtwarzacza: `settings.playerId`; w buildzie debug chip u góry otwiera listę graczy MA i zapisuje wybór. Bez wybranego gracza — stan pusty. Własny odtwarzacz TV (Sendspin) ma tylko ustawić `playerId` (S05/S14c).
- Testy: 14 testów VM na fake backendzie (mapowanie kolejki, postęp lokalny, optymistyczny play/pauza, ulubione z rollbackiem, wybór gracza). `check.sh` zielone. Aplikacja startuje na emulatorze TV bez crasha.
- NIE sprawdzone: wygląd z prawdziwymi danymi i porównanie zrzutu z makietą (brak hasła/tokenu MA w sesji; ścieżka do ekranu jest tylko przez mini-player, który pojawia się dopiero gdy coś gra) oraz sterowanie żywym serwerem. ⚠️ nadal niepotwierdzone: URL `imageproxy`, kształt `queue_updated`, usuwanie ulubionych (używam `itemId`, dla nie-bibliotecznych pozycji może wymagać id z biblioteki).
- Pułapki: `ConnectionManager.start()` woła teraz `MaPlayerBackend` (więc app łączy się od startu, o ile VM jest tworzony — mini-player w shellu tworzy go zawsze). Coil obniżony do 3.3.0 (3.6.3 wymaga compileSdk 37); dodano `coil-network-okhttp` na końcu sekcji libraries. Brak przycisku „Teraz gra” w menu bocznym — wejście przez mini-player (do decyzji koordynatora). Nie ma jeszcze przewijania (seek) paska postępu.
