# SFix-UI4 — Play/Pause: przytrzymanie pauzowało muzykę
**Model:** Sonnet · **Gałąź:** `claude/fix-ui4-playpause-53000b`

## Błąd
Na TV: przytrzymanie Play/Pause otwiera panel „Teraz gra”, ale chwilowo pauzuje granie (Sendspin `Pause`). Krótkie naciśnięcie miało przełączać odtwarzanie/pauzę niezależnie od ekranu.

## Zadania
1. Odtwórz błąd (kod + test; adb na TV odrzucony przez klasyfikator uprawnień, więc bez testu na urządzeniu).
2. Minimalna poprawka w `RemoteKeyDispatcher` + test JVM.
3. Inne rzeczy tylko w „Wynik”.

## Gotowe, gdy
Test zielony, PR otwarty.

## Wynik
- Przyczyna: domyślnie Play/Pause ma tylko skrót „przytrzymaj” (panel), bez skrótu krótkiego. `dispatch` oddawał wtedy `down` systemowi (`return false`), a ten przekazywał go do MediaSession, który pauzował od razu; dopiero po ~500 ms wchodził hold i otwierał panel.
- Poprawka: `shortAction()` — Play/Pause z samym skrótem hold ma niejawny skrót krótki `PLAY_PAUSE`. `down` jest konsumowane, krótka akcja idzie przy puszczeniu (`handler.playPause()`), hold otwiera panel i nie rusza odtwarzania. Bez żadnego skrótu na klawisz — przechodzi do systemu jak dawniej.
- Testy: `RemoteKeyDispatcherTest` (krótkie = toggle przy puszczeniu, hold = tylko panel, brak skrótów = przepuszczone).
- Niezweryfikowane na urządzeniu: adb na TV (10.0.0.6) odrzucony przez klasyfikator; emulator niesprawdzany.
- Uwaga: poza aplikacją (usługa dostępności, SUI9) klawisz z hold nadal nie jest przechwytywany — świadoma decyzja z SUI9.
