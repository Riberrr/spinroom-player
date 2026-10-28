# S14c — Kalibracja opóźnienia dźwięku
**Model:** Opus · **Zależy od:** S05 · **Gałąź:** `s14c-latency`

## Przeczytaj
`docs/SENDSPIN.md` (synchronizacja), README `:core:audio`, „Wynik” w `S04-audio.md` i `S05-service.md`.

## Zadania
1. Ekran w ustawieniach: suwak opóźnienia (−500…+500 ms) z podglądem na żywo, zapis w DataStore, przekazanie do `AudioEngine`.
2. Asystent: sygnał testowy grany jednocześnie z innym głośnikiem w grupie MA, użytkownik dopasowuje suwak.
3. Opisz w „Wynik”, jak to się ma do opóźnienia ustawianego po stronie MA.

## Gotowe, gdy
Na TV z soundbarem dźwięk zgrany z innym głośnikiem MA. v0.3.0 koordynator taguje, gdy S14a, S14b i S14c są scalone.

## Wynik
**Co działa:** Ustawienia → „Opóźnienie dźwięku” (podekran w `:feature:settings/latency`, bez nowej trasy). Suwak −500…+500 ms, ←/→ = 10 ms, zapis w `LatencyStore` (DataStore `settings`, klucz `audio_calibration_ms`), usługa odtwarzania stosuje zmianę od razu (`PlaybackHost.audioCalibrationMs` → `SendspinReceiver` → `AudioEngine.setCalibrationMs`; zmiana = natychmiastowe wyrównanie, nie dryf). Na TV (10.0.0.6): suwak, Wyzeruj, Back i zapis po restarcie sprawdzone, log `sendspin: calibration: N ms` przy każdym kroku. Testy JVM: silnik (kalibracja przesuwa termin, lead time, clamp), `LatencyStore`, sygnał WAV, serwer HTTP (200/206/404), wykrywanie grupy.
**Asystent:** gdy TV jest w grupie MA (`synced_to`, gracz-grupa z TV w `group_members` albo TV jako lider), przycisk „Graj sygnał testowy”: TV sam serwuje wygenerowany WAV (piknięcie co 1 s, co 4. wyższe, 3 min) na losowym porcie `http://<IP TV>:<port>/spinroom-sync.wav` i wysyła `play_media(option=play)` do kolejki grupy; wyjście z ekranu = `stop` + zamknięcie serwera. **Niesprawdzone na żywo:** aplikacja na TV nie była zalogowana do MA, a loginu/hasła nie wpisuję — trzeba sprawdzić, że MA 2.10.8 przyjmuje URL i gra go w grupie.
**A opóźnienie po stronie MA:** MA ma dla odtwarzacza Sendspin ustawienie „Static playback delay (ms)” (0–5000) = protokołowe `output_delay_ms` (`set_output_delay`); klient je raportuje, a MA zapisuje je w konfiguracji. Nasza kalibracja to osobny lokalny dodatek, **niewysyłany** do MA: efektywnie gramy `ts − (output_delay + kalibracja)`. Zalety: działa bez logowania do panelu MA i pozwala na wartości ujemne (MA tylko ≥ 0, czyli tylko „graj wcześniej”). Zasada: duże, stałe opóźnienie toru (soundbar) można ustawić w MA, drobne strojenie — w TV; obie wartości się sumują.
**Zmiana poza zakresem (mała):** `SpinroomShell` łapał ← w fazie preview, więc żaden element ekranu nie dostawał ←; teraz `onKeyEvent` (bubble) — ← na krawędzi nadal otwiera menu (sprawdzone na Szukaj i Ustawieniach). Ustawienia mają teraz `verticalScroll` (przycisk był pod krawędzią ekranu). `Player` w `:core:ma-api` dostał `synced_to`, `group_members`.
**Pułapki:** dodatnia kalibracja podnosi `required_lead_time_ms`; ujemna trzyma dźwięk dłużej w buforze (1 MB starcza z zapasem). MA musi móc połączyć się z TV (tak jak dla Sendspin). Gdy TV nie jest w grupie, przycisk sygnału jest ukryty i jest podpowiedź.
