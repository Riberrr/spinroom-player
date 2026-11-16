# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: SFix03a–g, SFix04a–d, SFix05–07 (15–16.11.2026, PR #26–#42). Ostatni tag: v1.0.1._

## Działa
- `:core:sendspin`: protokół (Noise `KKpsk2`, parowanie, zegar) + transport (serwer 8928, mDNS, klient 8927).
- `:core:audio`: silnik FLAC/Opus/PCM + `SpinroomPlaybackService` (Media3, pierwszoplanowa). Tożsamość Sendspin w DataStore.
- **Na TV 10.0.0.6** (API 30): usługa w tle, MA 2.10.8 sam się łączy, akcje powiadomienia idą do MA.
- `:core:ma-api` + `:core:data`: klient WS MA, repozytoria, DataStore; Ustawienia z logowaniem.
- Ekrany: Teraz odtwarzane (wzorzec), Start, Biblioteka, Szczegóły (album/wykonawca/playlista), Źródła (drzewo `music/browse`), Szukaj (klawiatura, filtry, głos). Powłoka: menu, mini-odtwarzacz.
- Wygaszacz po 60 s bezczynności i autostart po włączeniu TV (`BootReceiver`, przełącznik w Ustawieniach; sprawdzone rebootem).
- Kalibracja opóźnienia dźwięku w Ustawieniach (−500…+500 ms, na żywo; sprawdzone na TV) + sygnał testowy dla grup MA.
- Ekran „O aplikacji”, CHANGELOG, README (Obtainium), metadane fastlane pl/en.
- Menu pod przytrzymaniem OK (Odtwórz następny, Do kolejki, Ulubione) na Start, Bibliotece, Szczegółach, Szukaj, Źródłach; wiersz „Ulubione” na Start (`FavoritesApi`).
- Release z R8 uruchomiony na TV: startuje, łączy się z MA. Coil z pamięcią podręczną, baseline profile.
- CI zielone: debug, release (R8), lint.

## Nie sprawdzone
- Granie z telefonu na TV, wygaszony ekran, pilot — wymaga zgody na „Telewizor” w MA (Odtwarzacze → Telewizor → Pairing → Setup).
- Wszystkie ekrany z prawdziwymi danymi i zrzuty vs makiety; płynność Biblioteki 2000+; głos na TV.
- Fixtures MA nie nagrane — ⚠️ w `docs/MA-API.md` (imageproxy, `queue_updated`, ulubione spoza biblioteki, `start_item`).

## W toku
- Nic otwartego. Zostaje tag v1.0.2 i test na TV 10.0.0.6: Teraz odtwarzane, mini-odtwarzacz, granie z MA, pilot (patrz koniec `docs/sessions/SFix03-plan.md`).

## Następne
- Tag v1.0.0 — czeka na 4 sekrety SIGNING_* w GitHub (bez nich release.yml kończy się błędem). Przed instalacją v1.0 odinstalować v0.x z TV. Do uporządkowania: `:core:design` zależy od `:core:data` (S14b); brak toastu i odświeżenia po zmianie ulubionych.
- Braki względem makiet: Biblioteka (Ulubione, sortowanie, filtr źródeł, Ł/Ś po Z), Szczegóły (wiersz „Źródło”, podświetlenie grającego), Źródła (chipy-filtry).
- Ekran tokenu parowania `SP:0…`, nazwa odtwarzacza. Do decyzji: „Teraz gra” w menu bocznym.

## Pułapki
- Klawisze multimediów trafiają do aplikacji, która ostatnio grała.
- Start/stop odbiornika blokuje — jeden wątek `Spinroom-service`. `SessionBridge` jedynym kolektorem `outgoing`.
- Przypięte (compileSdk 36): OkHttp 5.1.0, Coil 3.3.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0.
- `docs/DECISIONS.md` — konflikty przy każdym równoległym PR, rozwiązywać sumą linii.
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 → 1280 dp. Bit +x dla `gradlew`/`scripts/*.sh`.
- Autostart wymaga jednego ręcznego uruchomienia po instalacji; Android 15+ blokuje start usługi z BOOT_COMPLETED.
- Debug i release naraz: port Sendspin 8928 — SFix03a dodał wybór wolnego portu; debug na emulatorze nadal ANR-uje przy starcie (SFix04d). MA nadaje własnemu graczowi id `up`+hex (nie klucz Sendspin) — własny wpis po `device_info`. Emulator: Wi-Fi po restarcie `cmd wifi connect-network AndroidWifi open`; VPN na hoście blokują jego sieć.
- Sygnał testowy kalibracji przez MA niesprawdzony (TV nie był zalogowany).
- Przytrzymanie OK na pilocie i usuwanie ulubionych spoza biblioteki niesprawdzone.
