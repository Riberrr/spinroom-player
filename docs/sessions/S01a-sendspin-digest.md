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
_(uzupełnia sesja)_
