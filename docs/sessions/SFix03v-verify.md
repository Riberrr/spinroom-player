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
_(uzupełnia sesja)_
