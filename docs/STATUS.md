# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S05, S09, S10 (28.10.2026). Gotowe do tagu **v0.1.0**._

## Działa
- `:core:sendspin`: protokół (Noise `KKpsk2`, parowanie, zegar) + transport (serwer 8928, mDNS, klient 8927).
- `:core:audio`: silnik FLAC/Opus/PCM + `SpinroomPlaybackService` (Media3, pierwszoplanowa, Wi-Fi/wake lock, restart po zmianie sieci). Tożsamość Sendspin w DataStore.
- **Na TV 10.0.0.6** (API 30): usługa działa w tle, MA 2.10.8 sam się łączy (4 role), akcje powiadomienia idą do MA.
- `:core:ma-api` + `:core:data`: klient WS MA, repozytoria, DataStore; Ustawienia z logowaniem.
- Ekrany: Teraz odtwarzane (wzorzec, S08), Start (S09), Biblioteka (S10: zakładki, siatka, skok do litery). Powłoka: menu, mini-odtwarzacz.
- CI zielone.

## Nie sprawdzone
- Granie z telefonu na TV, wygaszony ekran, klawisze pilota — wymaga zgody na odtwarzacz „Telewizor” w MA (Ustawienia → Odtwarzacze → Telewizor → Pairing → Setup).
- Ekrany z prawdziwymi danymi; płynność Biblioteki przy 2000+ albumach; zrzuty vs makiety.
- Fixtures MA nie nagrane — ⚠️ w `docs/MA-API.md`. ⚠️ `settings.playerId` = client id Sendspin (założenie).

## W toku
- S11 Szczegóły, S12 Źródła, S13 Szukaj (Sonnet) → po nich **v0.2.0**.

## Następne
- S14a ← S05+S08, S14c ← S05. Ekran tokenu parowania `SP:0…`, nazwa odtwarzacza, `BOOT_COMPLETED`.
- Biblioteka: brak zakładki Ulubione, sortowania, filtra źródeł; Ł/Ś sortowane po Z.
- Do decyzji: pozycja „Teraz gra” w menu bocznym.

## Pułapki
- Klawisze multimediów trafiają do aplikacji, która ostatnio grała — nasza przejmuje je, gdy zagra `AudioTrack`.
- Start/stop odbiornika blokuje — jeden wątek `Spinroom-service`. `SessionBridge` jedynym kolektorem `outgoing`.
- Przypięte (compileSdk 36): OkHttp 5.1.0, Coil 3.3.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0.
- `docs/DECISIONS.md` — konflikty przy równoległych PR, rozwiązywać sumą linii.
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 → 1280 dp. Bit +x dla `gradlew`/`scripts/*.sh`.
