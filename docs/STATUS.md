# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S03, S08 (27.10.2026)._

## Działa
- Szkielet: `:app`, `:core:*`, `:feature:*`, Hilt, powłoka UI (menu, nawigacja, mini-odtwarzacz), komponenty TV w `:core:design`.
- `:core:sendspin`: protokół (Noise `KKpsk2`, parowanie, zegar) + transport (S03): serwer Ktor `0.0.0.0:8928/sendspin`, mDNS `_sendspin._tcp`, klient zapasowy 8927.
- **Na TV 10.0.0.6** („2020/2021 UHD Android TV”): MA 2.10.8 sam znajduje odtwarzacz, łączy się, handshake Noise przechodzi, zegar zbiega.
- `:core:audio`: silnik FLAC/Opus/PCM z synchronizacją (emulator: 0 zgubionych ramek, ±90 µs).
- `:core:ma-api` + `:core:data`: klient WS MA, repozytoria, DataStore; ekran Ustawień z logowaniem.
- `:feature:nowplaying` (S08, wzorzec ekranów — README modułu): pełny ekran + mini-odtwarzacz na wspólnym VM; w debug wybór gracza MA.
- CI zielone.

## Nie sprawdzone
- Dźwięk z MA na TV — niesparowany odtwarzacz dostaje puste `activities` (potrzebna zgoda w MA albo token `SP:0…`).
- Ekran „Teraz odtwarzane” z prawdziwymi danymi i sterowanie żywym serwerem.
- Fixtures MA nie nagrane — ⚠️ w `docs/MA-API.md` (imageproxy, `queue_updated`, ulubione).

## W toku
- S05 usługa + pilot (Opus) → **v0.1.0**. S09 Start, S10 Biblioteka (Sonnet).

## Następne
- S12 Źródła, S13 Szukaj ← S08. S14a ← S05+S08, S14c ← S05, S11 ← S10.
- Przed v0.1.0: klucz podpisu w GitHub Secrets (instrukcja z S05).
- Do decyzji: brak pozycji „Teraz gra” w menu bocznym (wejście przez mini-odtwarzacz).

## Pułapki
- `SessionBridge` jedynym kolektorem `session.outgoing`; drugie połączenie zamykane 1013. `SendspinServer.stop()` blokuje ~1 s.
- `app/src/debug` ma tymczasowy start odtwarzacza (S05 usuwa).
- `ConnectionManager.start()` woła `MaPlayerBackend` (mini-player tworzy VM zawsze).
- Przypięte z powodu compileSdk 36: OkHttp 5.1.0, Coil 3.3.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0.
- `docs/DECISIONS.md`/`libs.versions.toml` — konflikty przy równoległych PR, rozwiązywać sumą linii.
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 → 1280 dp. Bit +x dla `gradlew`/`scripts/*.sh`.
