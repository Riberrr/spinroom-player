# S01a — Streszczenie Sendspin 1.0
**Model:** Sonnet · **Zależy od:** S00b · **Gałąź:** `s01a-sendspin-digest`

## Cel
Raz przeczytać specyfikację Sendspin i zostawić streszczenie, z którego S02–S05 pracują bez sięgania do sieci.

## Przeczytaj
`docs/PROJECT.md` (sekcja „Kanał odtwarzacza”), specyfikacja https://github.com/Sendspin/spec (przez `gh api`; zapisuj surowe pliki do scratchpada i czytaj fragmentami), strona https://www.music-assistant.io/player-support/sendspin. Dla porównania architektura SendspinDroid (MIT) — tylko ogólny przegląd.

## Zadania
1. `docs/SENDSPIN.md` (≤ 250 linii): role, transport (porty, ścieżki, mDNS + rekordy TXT, kto się do kogo łączy), kolejność handshake, lista wiadomości z polami i przykładowym JSON, układ binarnej ramki audio, synchronizacja zegara (algorytm), kodeki i ich nagłówki, szyfrowanie Noise `KKpsk2` (skąd klucze i PSK, **jak działa parowanie**), błędy i ponowne łączenie, różnice 1.0 vs preview.
2. Sekcja „Decyzje dla Spinroom”: które role implementujemy, priorytet kodeków, biblioteka Noise dla Kotlina/Javy (dostępna w Maven Central), czy TV jest serwerem WS, klientem, czy oboje.
3. Kontrakty dla S02 i S04 (pracują równolegle): w `core/sendspin/src/main/java/.../core/sendspin/api/` same typy danych i interfejsy, np. `AudioChunk`, `StreamFormat`, `ClockSource`, `PlayerCommand`, `TrackMetadata`. Bez implementacji.
4. Fixtures: przykładowe wiadomości ze specyfikacji jako pliki w `core/sendspin/src/test/resources/fixtures/`.

## Gotowe, gdy
Plik ≤ 250 linii, kontrakty się kompilują (`check.sh`), otwarte pytania wypisane w „Wynik”.

## Nie rób
Implementacji protokołu, sieci ani audio.

## Wynik
**Co działa:** `docs/SENDSPIN.md` (111 linii) — streszczenie specyfikacji `main` Sendspin (po `1.0.0-rc1`): role, mDNS/porty, handshake, wiadomości, ramki binarne, filtr zegara, kodeki, Noise `KKpsk2`, parowanie, decyzje. Kontrakty w `core/sendspin/.../api/` (`Clock.kt`, `Stream.kt`, `Player.kt`, `Metadata.kt`) kompilują się (`check.sh` OK). Fixtures: 30 plików w `core/sendspin/src/test/resources/fixtures/` (JSON + `.hex` dla ramek binarnych).
**API:** S02 implementuje `ClockSource`, `PlayerStream` (Flow `PlayerEvent`), `PlayerStateReporter`, `SendspinSessionState`; S04 konsumuje je i dostarcza `PlayerState`/`PlayerCapabilities`. Coroutines zmienione na `api(...)` w `build.gradle.kts` modułu.
**Ważne:** spec `main` wymaga Noise (nie ma już zwykłego WS jak w `docs/PROJECT.md`); `sendspin-jvm` i spec rc1 są nieaktualne. MA 2.10.8 ma opcję "Allow legacy clients" (bez szyfrowania) — nie używamy.
**Otwarte pytania:** (1) czy MA 2.10.8 mówi już Noise — sprawdzić nagraniem handshake; (2) czy `org.signal.forks:noise-java:0.1.1` obsługuje `KKpsk2` (test w S02, plan B: własna nakładka); (3) UI parowania w MA (PIN vs token `SP:0…`) i czy `unpaired_access` wystarczy na start; (4) klucz prywatny — Keystore vs DataStore; (5) wymagane klucze TXT mDNS po stronie MA.
