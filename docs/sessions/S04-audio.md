# S04 — Silnik audio
**Model:** Opus · **Zależy od:** S01a · **Gałąź:** `s04-audio`

## Cel
Odtwarzanie ramek audio ze znacznikami czasu: dekodowanie, bufor, AudioTrack, kompensacja opóźnienia.

## Przeczytaj
`docs/SENDSPIN.md` (sekcje kodeki i synchronizacja), kontrakty w `core/sendspin/.../api/`, `core/audio/README.md`.

## Zadania
1. Dekodery FLAC / Opus / PCM przez `MediaCodec` (sprawdź obsługę na API 26+; fallback opisz w „Wynik”).
2. Bufor czasowy: ramka grana o czasie serwera przeliczonym przez `ClockSource` + stałe opóźnienie urządzenia; gubienie spóźnionych ramek, cisza przy dziurach.
3. Wyjście `AudioTrack` z pomiarem opóźnienia (`getTimestamp`), głośność, wycisz, ustawienie stałego opóźnienia (do kalibracji w S14c).
4. Publiczne API: `AudioEngine.start(format)`, `submit(AudioChunk)`, `stop()`, `state: StateFlow`.
5. Testy JVM harmonogramu bufora z fałszywym zegarem; ekran debug (tylko w buildzie debug) grający plik FLAC z `assets` z wygenerowanymi znacznikami czasu.

## Gotowe, gdy
Testy zielone; na emulatorze ekran debug gra próbkę bez trzasków; README opisuje API.

## Nie rób
Sieci, protokołu, MediaSessionService (to S05).

## Wynik
**Co działa:** `:core:audio` — `SpinroomAudioEngine` (implementuje `AudioEngine`): FLAC/Opus przez `MediaCodec`, PCM w Kotlinie, bufor czasowy + `PlaybackScheduler` (cisza do terminu, gubienie spóźnionych, korekta ±1 ramka/200 w strefie 100 µs, resync >10 ms), `AudioTrack` z pomiarem opóźnienia, głośność `(v/100)^1.5`, wycisz, stałe opóźnienie. 31 testów JVM (harmonogram z fałszywym zegarem, dzielenie ramek FLAC, estymator zegara wyjścia, silnik z fałszywym wyjściem). Na emulatorze TV (API 31) ekran debug gra próbkę FLAC w pętli: 0 zgubionych ramek, 0 resynców, błąd synchronizacji ~±90 µs, przewijanie i stop działają (sprawdzone statystykami, nie uchem).
**API:** `AudioEngine.start/submit/clear/stop/release`, `handle(PlayerEvent)`, `setVolume/setMuted/setOutputDelayMs`, `state`, `stats`, `playerState` (dla `PlayerStateReporter.report`), `AudioCapabilities.probe()` → `PlayerCapabilities`. Szczegóły: `core/audio/README.md`.
**Fallback:** systemowy dekoder FLAC jest od API 27 — na API 26 `AudioCapabilities` ogłasza tylko Opus + PCM (spec wymaga flac lub pcm, więc OK).
**Dalej (S05):** zbierać `PlayerStream.events` do `engine.handle`, `playerState` → `reporter.report`, `setAvailable` po `clock.converged`; S14c kalibruje `setOutputDelayMs`.
**Pułapki:** `ClockSource.localNowUs()` musi mieć bazę `System.nanoTime()`. Emulator zwraca `getTimestamp` z innym licznikiem ramek (wtedy używamy `playbackHeadPosition`). Dekoder FLAC milknie po skoku numeru ramki — flush przy wykryciu. Zmiana częstotliwości w locie odtwarza AudioTrack (ucina ~150 ms). `core:sendspin` jest teraz `api(...)` w `core:audio`.
