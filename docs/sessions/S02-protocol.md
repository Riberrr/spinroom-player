# S02 — Protokół Sendspin
**Model:** Opus · **Zależy od:** S01a · **Gałąź:** `s02-protocol`

## Cel
Cała logika protokołu w `:core:sendspin`, bez sieci i bez audio, w pełni przetestowana na JVM.

## Przeczytaj
`docs/SENDSPIN.md`, „Wynik” w `S01a-sendspin-digest.md`, kontrakty w `core/sendspin/.../api/`, fixtures.

## Zadania
1. Modele wiadomości (kotlinx.serialization) i parser ramek binarnych.
2. Maszyna stanów połączenia (role player, metadata, artwork, controller): wejście = odebrane ramki, wyjście = ramki do wysłania + zdarzenia. Niezależna od transportu.
3. Szyfrowanie Noise według decyzji z S01a (biblioteka dopisana na końcu katalogu wersji).
4. Estymator zegara (offset + dryf, filtr na jitter) implementujący `ClockSource`.
5. Publiczne API: `SendspinSession` (onText/onBinary, `Flow` ramek wychodzących), `Flow<AudioChunk>`, `Flow<TrackMetadata>`, wysyłanie `PlayerCommand`.
6. Testy JVM: każda wiadomość z fixtures, przejścia stanów, zerwanie i powrót, zegar z syntetycznym jitterem.

## Gotowe, gdy
`./gradlew :core:sendspin:testDebugUnitTest` zielony; README modułu opisuje publiczne API (to czyta S03 i S04).

## Nie rób
Socketów, mDNS, MediaCodec, AudioTrack. Nie zmieniaj typów w `api/` bez wpisu w „Wynik” (S04 z nich korzysta).

## Wynik
_(uzupełnia sesja)_
