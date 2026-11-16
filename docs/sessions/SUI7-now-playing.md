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
_(uzupełnia sesja)_
