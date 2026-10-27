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
**Co działa:** cały protokół w `:core:sendspin` bez sieci: handshake `client/init`→Noise→`client/hello`→`server/activate`, Noise `KKpsk2` (własna implementacja na prymitywach noise-java — biblioteka nie ma `psk2`; zgodna z wektorem cacophony), wybór PSK (lt/pr/sn + Sentinel fallback), re-handshake w locie, parowanie Pairing PSK, `server/unpair`, reguły dopuszczalności `server/activate`, ramki binarne + fragmentacja, filtr Kalmana zegara (błąd < 1 ms przy jitterze 0,5–15 ms i dryfie 40 ppm), metadane/okładki planowane na `timestamp`. 61 testów JVM zielonych.
**Publiczne API:** `SendspinSession` (`onOpen/onText/onBinary/onClosed`, `outgoing: Flow<OutgoingFrame>`, `state`, `notices`, `clock`) implementuje `PlayerStream`, `PlayerStateReporter`, `SendspinSessionState`; `CredentialStore` + `InMemoryCredentialStore`; `PairingToken.encode`. Szczegóły w `core/sendspin/README.md`.
**Kontrakty `api/`:** bez zmian. Zamiast osobnego `Flow<AudioChunk>` audio idzie jako `PlayerEvent.Chunk` w `events` (jeden kolektor — S04).
**Dalej:** S03 — Ktor CIO na 8928 + mDNS, podpiąć `onOpen/onText/onBinary/onClosed` i zbierać `outgoing`; `receivedAtUs` brać zaraz po odebraniu. S05 — `CredentialStore` na DataStore/Keystore (klucz i pairing PSK generowane raz), ekran tokenu `SP:0…`.
**Pułapki:** `events` i `outgoing` mają po jednym kolektorze (Channel). Tylko jedno połączenie naraz — arbitraż wielu serwerów (`another_server`) nie jest zrobiony, S03 musi odrzucić drugie połączenie. Zegar resetuje się przy każdym nowym połączeniu. Nadal niesprawdzone na żywo: czy MA 2.10.8 mówi Noise (pytanie otwarte 1 z S01a).
