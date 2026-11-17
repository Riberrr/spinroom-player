# SUI7 — Odtwarzacz 40:60 (muzyka i radio)
**Model:** Sonnet · **Gałąź:** `sui7-now-playing` · **Moduły:** `:feature:nowplaying`. Zależy od: SUI1, SUI2, SUI4.
**Makiety:** `png/10A-player-muzyka.png`, `png/10B-player-radio.png`.

## Zadania
1. **Układ 40:60:** okładka po lewej (~440 dp). Po prawej: etykieta, tytuł, wykonawca · album, postęp i przyciski (losowo, ⏮, ⏯, ⏭, powtarzanie, ♡). U góry pigułki stanu (Sendspin, format) i głośność.
2. Tło: `ContentBackdrop` z okładki albo z koloru stacji.
3. **Muzyka:** panel „Następne w kolejce” z 4 pozycjami i opcją „↓ cała kolejka”. `OK` na utworze przeskakuje do niego.
4. **Radio:**
   - zamiast paska postępu: źródło · miasto · język · jakość;
   - w miejscu kolejki „Ulubione stacje”: numer, kwadratowa miniatura, nazwa, utwór grany teraz; bieżąca stacja podświetlona jako „gra teraz”;
   - ⏮ ⏭ to poprzednia / następna ulubiona stacja (kolejność z SUI4);
   - `OK` na wierszu przełącza na tę stację.
5. `CH±` przez `RemoteKeyDispatcher` z SUI2. Nie dubluj obsługi klawiszy.
6. Ekran nie gaśnie, gdy odtwarzacz jest na wierzchu (`keepScreenOn`). Wygaszacz z okładką (S14a) nie startuje, gdy w Wyglądzie jest wyłączony, a domyślnie jest wyłączony.

## Gotowe, gdy
- Na emulatorze zrzuty: utwór z biblioteki i stacja radiowa.
- Test ViewModelu: radio vs muzyka; przejście dalej z ostatniej stacji wraca do pierwszej.
- PR otwarty.

## Wynik
- Działa (testy JVM + build): układ 40:60, pigułki Sendspin/głośność, tło wg „Wyglądu”, 4 następne utwory z OK-skokiem, radio z panelem ulubionych stacji, ⏮⏭ po ulubionych (zawija), `keepScreenOn`. Wygaszacz był już domyślnie wyłączony (`look.screensaver`).
- API: `NowPlayingActions.onStepStation/onPlayStation`, `StationRow`, `NowPlayingUiState.stations/radioMeta/playerBackground`; `PlayerBackend` + `favoriteStations/refreshStations/playStation/playerBackground`. `ShellViewModel` (CH±, dock) przepięty na `FavoriteStationsRepository`.
- Braki: linia radia pokazuje tylko źródło i opis (API MA nie daje miasta/języka/jakości); brak pigułki formatu; tryb tła „color” używa koloru z nazwy, nie z okładki.
- Zrzuty z emulatora NIE zrobione: emulator nie ma odtwarzacza MA (nic nie gra), a build `.debug` wywala się tam przy starcie usługi odtwarzania. Do sprawdzenia wzrokowo na TV (są `@Preview` muzyka/radio/kolejka).
- Pułapka: `lintDebug` w worktree failuje na `local.properties` (PropertyEscape), nie na kodzie.
