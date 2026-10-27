# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S02, S04, S06 (27.10.2026)._

## Działa
- Szkielet: `:app`, `:core:*`, `:feature:*` (zaślepki), Hilt, powłoka UI (menu, nawigacja, mini-odtwarzacz), komponenty TV w `:core:design`.
- `:core:sendspin` (S02): pełny protokół bez sieci — handshake, Noise `KKpsk2` (własny na prymitywach noise-java), parowanie, ramki binarne, filtr zegara (< 1 ms). API: `SendspinSession`, `CredentialStore`. 61 testów.
- `:core:audio` (S04): `SpinroomAudioEngine` — FLAC/Opus (MediaCodec), PCM, harmonogram z synchronizacją, AudioTrack, głośność. Emulator: 0 zgubionych ramek, ±90 µs. 31 testów.
- `:core:ma-api` + `:core:data` (S06): `MaClient` (WS, reconnect), Library/Browse/PlayerQueue/Auth API, repozytoria (Paging), `SettingsStore` (DataStore). Ekran Ustawień: adres + logowanie.
- Dokumentacja: `docs/SENDSPIN.md`, `docs/MA-API.md`. CI zielone.

## Nie sprawdzone
- Nic nie było uruchomione na prawdziwym TV ani na żywym serwerze MA.
- Czy MA 2.10.8 mówi Noise. Dźwięk nie był słuchany (tylko statystyki).
- Fixtures MA nie nagrane — pozycje ⚠️ w `docs/MA-API.md` (zdarzenia, imageproxy, partial).

## W toku
- S03 sieć + mDNS (Opus), S08 Teraz odtwarzane — ekran-wzorzec (Sonnet).

## Następne
- S05 usługa + pilot ← S03, S04 → **v0.1.0** (przed nim: klucz podpisu w GitHub Secrets).
- S09/S10/S12/S13 ← S08.
- `ConnectionManager.start()` musi wołać aplikacja przy starcie (dziś tylko Ustawienia).

## Pułapki
- `SendspinSession`: `events` i `outgoing` mają po jednym kolektorze; jedno połączenie naraz.
- `ClockSource.localNowUs()` na bazie `System.nanoTime()`. FLAC systemowy od API 27.
- OkHttp 5.1.0, lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0 — nowsze wymagają compileSdk 37.
- `docs/DECISIONS.md` i `libs.versions.toml` dają konflikty przy równoległych PR — rozwiązywać sumą linii.
- `Text` poza `Surface` jest czarny. Emulator: gęstość 480 → 1280 dp.
- Bit wykonywalności `gradlew`/`scripts/*.sh` na Windowsie. AGP 9 ma wbudowany Kotlin.
