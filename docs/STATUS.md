# Status

_Prowadzi koordynator po każdym scaleniu. Przebudowa UI SUI0–SUI9, SFix-UI1–17 scalone. Ostatni tag: v1.1.0; v1.2.0 w przygotowaniu (CHANGELOG gotowy, tag po smoke teście na emulatorach 5554 i 5556). Plan: `docs/plans/PLAN-1.2.0.md`._

## Działa
- `:core:sendspin`: protokół (Noise `KKpsk2`, parowanie, zegar) + transport (serwer, mDNS, klient).
- `:core:audio`: FLAC/Opus/PCM + `SpinroomPlaybackService` (Media3). Własny gracz TV w MA.
- `:core:ma-api` + `:core:data`: klient WS MA, repozytoria, DataStore; Ustawienia z logowaniem.
- Ekrany: Start, Biblioteka, Szczegóły, Źródła, Szukaj, Radio, Playlisty, Teraz odtwarzane, Ustawienia (przypisanie klawiszy, kalibracja opóźnienia).
- Powłoka (1.2.0): stały panel odtwarzacza u góry (rozwija się nakładką, strzałka w górę), menu boczne z pastylką odtwarzacza i listą odtwarzaczy, menu zawsze do widoku głównego.
- Radio: „Moje stacje” = wszystkie stacje z MA (A–Z, porcje po 100), ulubione na górze; Radio Browser przez filtry.
- Szukaj: powrót kursora po Wstecz; okładki bez klatki z inicjałami przy zmianie utworu.
- Wygaszacz, autostart po włączeniu TV, release z R8 (tag `vX.Y.Z` → `release.yml`). CI: debug, release, lint.

## Nie sprawdzone
- Zmiany 1.2.0 tylko na emulatorach (API 31 i 34); TV 10.0.0.6 nie ruszany do polecenia użytkownika.
- Dociąganie stron w „Moje stacje” (testowa biblioteka ma ~10 stacji); pełny ekran odtwarzacza po zmianie okładki.
- Wizualne zgodność z makietą panelu u góry; fokus po powrocie z pełnego ekranu (krótki błysk panelu możliwy).
- Fixtures MA nie nagrane (⚠️ w `docs/MA-API.md`); CH± poza aplikacją wymaga usługi dostępności.

## Następne
- Smoke test v1.2.0 na obu emulatorach, tag, potem instalacja na TV (na polecenie użytkownika).
- Usunąć chip „Emulator · połączono” w `HomeScreen` (`:feature:home`) — zdublowany przez pastylkę w menu.
- Zapamiętywanie trybu listy w Radiu; braki względem makiet: Biblioteka (sortowanie, filtr źródeł), Źródła (chipy).

## Pułapki
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 (4K) / 240 (1080p) → 1280 dp. Bit +x dla `gradlew`/`scripts/*.sh`.
- Klawisze multimediów trafiają do aplikacji, która ostatnio grała. `SessionBridge` jedynym kolektorem `outgoing`.
- Menu boczne zakłada, że pastylka jest pierwszą pozycją fokusu (z ekranu bez menu wejście ląduje na pastylce).
- Lint lokalnie pada na Windows-path w `local.properties` (`sdk.dir=C\:/...`); CI jest wyrocznią.
- `docs/DECISIONS.md` i pliki sesji — konflikty przy równoległych PR, rozwiązywać sumą linii.
- Przypięte wersje (compileSdk 36): OkHttp 5.1.0, Coil 3.3.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0.
- Debug i release naraz: port Sendspin 8928 (wybór wolnego portu); MA nadaje graczowi id `up`+hex.
