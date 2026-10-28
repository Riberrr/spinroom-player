# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S14a (28.10.2026). Tag **v0.2.0** wypchnięty, ale wydanie padło (release nie kompiluje `@Preview`) — SFix01 w toku, potem przesunąć tag._

## Działa
- `:core:sendspin`: protokół (Noise `KKpsk2`, parowanie, zegar) + transport (serwer 8928, mDNS, klient 8927).
- `:core:audio`: silnik FLAC/Opus/PCM + `SpinroomPlaybackService` (Media3, pierwszoplanowa). Tożsamość Sendspin w DataStore.
- **Na TV 10.0.0.6** (API 30): usługa w tle, MA 2.10.8 sam się łączy, akcje powiadomienia idą do MA.
- `:core:ma-api` + `:core:data`: klient WS MA, repozytoria, DataStore; Ustawienia z logowaniem.
- Ekrany: Teraz odtwarzane (wzorzec), Start, Biblioteka, Szczegóły (album/wykonawca/playlista), Źródła (drzewo `music/browse`), Szukaj (klawiatura, filtry, głos). Powłoka: menu, mini-odtwarzacz.
- Wygaszacz po 60 s bezczynności i autostart po włączeniu TV (`BootReceiver`, przełącznik w Ustawieniach; sprawdzone rebootem).
- CI zielone (tylko debug — release naprawia SFix01).

## Nie sprawdzone
- Granie z telefonu na TV, wygaszony ekran, pilot — wymaga zgody na „Telewizor” w MA (Odtwarzacze → Telewizor → Pairing → Setup).
- Wszystkie ekrany z prawdziwymi danymi i zrzuty vs makiety; płynność Biblioteki 2000+; głos na TV.
- Fixtures MA nie nagrane — ⚠️ w `docs/MA-API.md` (imageproxy, `queue_updated`, ulubione spoza biblioteki, `start_item`).

## W toku
- S14c kalibracja opóźnienia (Opus), SFix01 build release (Sonnet).

## Następne
- S14b ulubione + menu OK ← S09–S13 (sama) → **v0.3.0** razem z S14c.
- Braki względem makiet: Biblioteka (Ulubione, sortowanie, filtr źródeł, Ł/Ś po Z), Szczegóły (wiersz „Źródło”, podświetlenie grającego), Źródła (chipy-filtry).
- Ekran tokenu parowania `SP:0…`, nazwa odtwarzacza. Do decyzji: „Teraz gra” w menu bocznym.

## Pułapki
- Klawisze multimediów trafiają do aplikacji, która ostatnio grała.
- Start/stop odbiornika blokuje — jeden wątek `Spinroom-service`. `SessionBridge` jedynym kolektorem `outgoing`.
- Przypięte (compileSdk 36): OkHttp 5.1.0, Coil 3.3.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0.
- `docs/DECISIONS.md` — konflikty przy każdym równoległym PR, rozwiązywać sumą linii.
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 → 1280 dp. Bit +x dla `gradlew`/`scripts/*.sh`.
- Autostart wymaga jednego ręcznego uruchomienia po instalacji; Android 15+ blokuje start usługi z BOOT_COMPLETED.
