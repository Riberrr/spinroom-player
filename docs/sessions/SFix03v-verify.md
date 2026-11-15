# SFix03v — Test końcowy na emulatorze
**Model:** Sonnet (prowadzi) + podagent Haiku do zrzutów · **Gałąź:** brak (raport w tym pliku) · Po scaleniu SFix03a–g.

## Cel
Potwierdzić poprawki SFix03 na `emulator-5554` przy najniższym zużyciu tokenów i zebrać nowe błędy.

## Przygotowanie
1. `git pull` main, `./gradlew :app:assembleDebug -q`, `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Zaloguj się w Ustawieniach (`10.0.1.50:8095`, konto `Tv`, hasło poda użytkownik).
3. Zrzuty: `bash scripts/emu-shot.sh <nazwa> [klawisze…]`. Klawisze: U D L R C(OK) B(Wstecz) H(długie OK); zrzut 1280×720 trafia do katalogu tymczasowego.

## Sposób pracy (oszczędnie)
- Sesja Sonnet uruchamia **podagenta Haiku** z listą kontrolną poniżej i poleceniem: „przejdź kroki, przy każdym zrób zrzut, odpowiedz tylko OK/BŁĄD + jedno zdanie na krok”. Do kontekstu Sonnet trafia sam raport, bez obrazów.
- Sonnet sam ogląda tylko zrzuty kroków z wynikiem BŁĄD.
- Nie uruchamiaj odtwarzania (zagra w przeglądarce lub na TV użytkownika).

## Lista kontrolna
1. Oba warianty uruchomione naraz → brak crasha (`adb logcat -d -b crash | tail -5` puste).
2. Bez połączenia (Wyloguj): Start, Biblioteka, Źródła, Szukaj pokazują „Połącz się z serwerem”, a nie pustkę ani „Wczytuję…”.
3. Na każdym ekranie ← otwiera menu na bieżącej pozycji, a → wraca do poprzedniego elementu treści.
4. Start: kafelek playlisty → szczegóły → Wstecz → fokus na tym samym kafelku.
5. Ulubione: Biblioteka → długie OK na playliście → „Dodaj do ulubionych” → Start pokazuje ją w „Ulubione”. Usunięcie znika bez restartu (na koniec przywróć stan).
6. Ulubione stacje: logo i sensowny podpis (nie „library”).
7. Źródła: wejście do folderu → fokus na pierwszej pozycji; Wstecz → fokus na folderze; brak „..”; „4 pozycje”.
8. Szczegóły playlisty: przyciski w jednej linii. Teraz odtwarzane: widoczny przycisk „następny”. Radio bez „0:00”.
9. Szukaj: ← z klawisza „a” od razu otwiera menu; chipy przewijają się.
10. Ustawienia: po zalogowaniu fokus na „Wyloguj”; pole adresu ma klawiaturę URL.

## Gotowe, gdy
„Wynik” zawiera tabelę krok → OK/BŁĄD oraz listę nowych błędów (kandydaci na SFix04). Kodu nie zmieniaj.

## Wynik
Test na `emulator-5554`, wariant release (build z main 2c4c960, `install -r`, użytkownik Tv pozostał zalogowany). Emulator wymagał cold bootu i ręcznego Wi-Fi (VPN na hoście blokował sieć). Aplikacja reaguje na klawisze wolno (~2–3 s), co utrudniało sekwencje.

| # | Krok | Wynik |
| --- | --- | --- |
| 1 | Oba warianty naraz | **BŁĄD** — wariant debug crashuje: `java.net.BindException: Address already in use` (port Sendspin 8928 współdzielony z release); powtarza się w pętli restartów. |
| 2 | Bez połączenia: „Połącz się z serwerem" | **NIE TESTOWANO** — wymagałoby Wyloguj, a nie wolno wpisywać haseł. Przy starcie bez sieci Start pokazał „Łączę z serwerem…" (nie pustkę). |
| 3 | ← menu / → powrót | OK na Start i Szukaj (z „a"). Źródła: ← z listy folderów idzie do listy wtyczek, dopiero drugie ← do menu. Biblioteka/Ustawienia nie sprawdzone dokładnie. |
| 4 | Start: playlista → szczegóły → Wstecz | OK (fokus wraca na ten sam kafelek) |
| 5 | Ulubione | OK — „Dodaj do ulubionych" w Bibliotece → wiersz „Ulubione" na Start; „Usuń z ulubionych" znika bez restartu. Stan przywrócony. |
| 6 | Ulubione stacje | OK w większości — logo + „Radio Browser"; „Radio RMF" ma tylko ikonę nutki. |
| 7 | Źródła: folder | OK — fokus na 1. pozycji, Wstecz → fokus na folderze, brak „..", „9 pozycji"/„3 pozycje". |
| 8 | Szczegóły / Teraz odtwarzane / radio | OK — przyciski w jednej linii (3. „Dodaj do" wygląda na ucięty), jest „następny", radio bez „0:00". |
| 9 | Szukaj | OK — ← z „a" otwiera menu, chipy przewijają się (widać „Radio"). |
| 10 | Ustawienia | Adres: klawiatura URL (inputType 0x11) OK. **BŁĄD:** fokus po wejściu ląduje na „Poprzedni" w mini-playerze, nie na „Wyloguj". |

### Nowe błędy (kandydaci SFix04)
1. **Crash debug przy równoległym uruchomieniu z release** — `BindException` na porcie 8928; Sendspin powinien łapać wyjątek i wybrać wolny port lub wyłączyć się łagodnie (osobno znany ANR debug: `startForegroundService` bez `startForeground`).
2. **Ustawienia: fokus startowy** na mini-playerze zamiast „Wyloguj".
3. **Ustawienia: lista odtwarzaczy** ma duplikaty/stare wpisy („Telewizor" ×2, „Web (Chrome on Windows)", „2020/2021 UHD Android TV"); nie widać, który to to TV.
4. **Biblioteka:** po powrocie (Wstecz z Now Playing) wraca na „Albumy · 0"; przy pustej zakładce ↓ przeskakuje do mini-playera, a → z ostatniego chipa też. „Albumy · 0" przy bibliotece z utworami — do sprawdzenia.
5. **Źródła:** po powrocie do listy folderów → trafia w pierwszą pozycję, nie w ostatnio aktywną („Playlists").
6. **Now Playing (radio):** „pokaż całą kolejkę (1)" obok „Kolejka jest pusta" — niespójne.
7. **Drobne UI:** „Radio RMF" bez logo; etykieta „Dodaj do…" ucięta na szczegółach playlisty.
8. **Mini-player:** długie OK na play uruchamia odtwarzanie (pomyłka testu — Radio ZET na PC_CHROME na kilka sekund, spauzowane; przypadkowo zmieniony wybrany odtwarzacz przywrócony na PC_CHROME).

### Uwagi środowiskowe
`ping` w emulatorze nie działa (NAT). Po restarcie emulatora: `cmd wifi connect-network AndroidWifi open`. Brak `local.properties` w worktree → build z `ANDROID_HOME=C:/Users/lukas/AppData/Local/Android/Sdk`.
