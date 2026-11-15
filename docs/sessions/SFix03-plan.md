# SFix03 — Poprawki z testów UI na emulatorze (plan dla koordynatora)

Źródło: test emulatora z 2026-11-15 (wersja wydania 1.0.1 zalogowana do MA 10.0.1.50 oraz debug `0.0.0-dev`).
Zakres: ekran Start, Biblioteka, Ulubione, Źródła; przy okazji Szukaj, Ustawienia, Szczegóły, Teraz odtwarzane.
Koordynator czyta **tylko ten plik** i sekcje „Wynik” plików `SFix03?-*.md`. Kodu nie czyta.

## Zadania

| ID | Temat | Plik sesji | Model | Fala | Zależy od | Gałąź |
| --- | --- | --- | --- | --- | --- | --- |
| SFix03a ✓ | Crash przy zajętym porcie Sendspin | `SFix03a-sendspin-port.md` | Sonnet | 1 | — | `fix-sendspin-port` |
| SFix03b ✓ | Fokus menu bocznego i powrót do treści | `SFix03b-shell-focus.md` | Sonnet | 1 | — | `fix-shell-focus` |
| SFix03c ✓ | Układ Szczegółów, Teraz odtwarzane, mini-odtwarzacza | `SFix03c-player-layout.md` | Sonnet | 1 | — | `fix-player-layout` |
| SFix03d ✓ | Start: odświeżanie, fokus, stany, stacje | `SFix03d-home.md` | Sonnet | 2 | b | `fix-home` |
| SFix03e ✓ | Biblioteka: stany, pasek liter | `SFix03e-library.md` | Haiku | 2 | b | `fix-library` |
| SFix03f ✓ | Źródła: fokus po wejściu do folderu, „..”, liczba pozycji | `SFix03f-sources.md` | Sonnet | 2 | b | `fix-sources` |
| SFix03g ✓ | Szukaj i Ustawienia: fokus, chipy, klawiatura | `SFix03g-search-settings.md` | Haiku | 3 | b | `fix-search-settings` |
| SFix03v ✓ | Test końcowy na emulatorze | `SFix03v-verify.md` | Sonnet (+ Haiku do zrzutów) | 4 | a–g scalone | brak (tylko raport) |

Opcjonalnie, osobno po decyzji użytkownika: **zakładka „Ulubione” w Bibliotece** (nowa funkcja, nie poprawka). Nie uruchamiać bez zgody.

## Dlaczego taki podział (oszczędzanie tokenów)
- Jeden moduł = jedna sesja: sesje w jednej fali nie dotykają tych samych plików, więc nie ma konfliktów ani ponownego czytania kodu.
- Haiku tam, gdzie poprawki są lokalne i mechaniczne (Biblioteka, Szukaj/Ustawienia). Sonnet tam, gdzie trzeba zrozumieć fokus Compose albo sieć. Opus niepotrzebny: żadne zadanie nie zmienia protokołu, audio ani synchronizacji.
- Sesje weryfikują logikę testami jednostkowymi. Emulator: tylko krótki przegląd na koniec, najlepiej przez podagenta Haiku z listą kontrolną (zrzuty 1280×720, `scripts/emu-shot.sh`).
- Emulator jest jeden (`emulator-5554`): **tylko jedna sesja naraz** używa emulatora. Koordynator przydziela emulator sesji, która o niego poprosi, pozostałe czekają.

## Pułapki środowiska (przekazać w każdym prompcie)
- Na emulatorze zainstalowane są `io.github.riberrr.spinroom` (wydanie) i `io.github.riberrr.spinroom.debug`. Do czasu scalenia SFix03a oba warianty nie mogą działać jednocześnie: drugi pada na porcie 8928. Przed testem: `adb -s emulator-5554 shell am force-stop io.github.riberrr.spinroom`.
- Logowanie: serwer `10.0.1.50:8095`, konto testowe `Tv`; hasło podaje użytkownik w czacie sesji (nie zapisywać w repo ani w docs).
- Na emulatorze wybranym odtwarzaczem jest „Web (Chrome on Windows)”, bo MA nie widzi emulatora za NAT (SFix02). To nie błąd. **Nie uruchamiać odtwarzania z emulatora** (zagra w przeglądarce lub na TV użytkownika).
- `adb` nie jest w PATH: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`.

## Prompt dla czatu-koordynatora

```
Jesteś koordynatorem SFix03 w repo Spinroom Player (ten folder). Model: Sonnet.
Czytasz tylko docs/sessions/SFix03-plan.md i sekcje „Wynik” plików docs/sessions/SFix03?-*.md. Nie czytasz kodu.
1. Uruchom fale po kolei. W fali uruchom każdą sesję jako osobną sesję w nowym worktree z gałęzi main,
   z promptem: „Zrób sesję <ID> według docs/sessions/<plik>.” i modelem z tabeli (maks. 3 naraz).
2. Emulator ma jedna sesja naraz. Gdy sesja pyta o emulator, przydziel go albo każ poczekać.
3. Gdy sesja zgłosi PR: sprawdź CI (gh run view --log-failed | tail -60 przy błędzie), pokaż mi 2–3 zdania
   z „Wynik” i zapytaj o scalenie. Po mojej zgodzie scal (squash), dopisz ✓ w tabeli tego pliku.
4. Następną falę zacznij, gdy wszystkie zależności z tabeli są scalone.
5. Po fali 4 pokaż mi raport SFix03v. Nowe błędy z raportu zapisz jako nowe wiersze (SFix04…), nie poprawiaj sam.
6. Na koniec zaktualizuj docs/STATUS.md (≤ 40 linii) i przypomnij o tagu patch i teście na TV.
```

## Wynik
_(koordynator: data scalenia każdej sesji, numer wersji)_
- Scalone 2026-11-15: SFix03a (#26), b (#28), c (#27), d (#29), e (#30), f (#31), g (#32). Raport SFix03v: PR #33. Wersja: jeszcze bez tagu (patch po SFix04 i teście na TV).

## Nowe wiersze z raportu SFix03v (SFix04; uruchomić po decyzji użytkownika)
| ID | Temat (numer błędu z raportu) | Model | Zależy od | Gałąź |
| --- | --- | --- | --- | --- |
| SFix04a ✓ | Ustawienia: fokus startowy na „Wyloguj” zamiast mini-playera (2); lista odtwarzaczy: duplikaty, rozpoznanie „tego TV” (3) | Sonnet | — | `fix-settings-focus` |
| SFix04b ✓ | Biblioteka: „Albumy · 0”, ↓/→ przy pustej zakładce, powrót z Now Playing (4); Źródła: fokus na ostatnio aktywnym po powrocie → (5) | Sonnet | — | `fix-library-sources-focus` |
| SFix04c ✓ | Now Playing radio: „kolejka (1)” vs „Kolejka jest pusta” (6); „Radio RMF” bez logo, ucięte „Dodaj do…” w Szczegółach (7) | Haiku | — | `fix-ui-polish` |
| SFix04d ✓ | Debug: ANR `startForegroundService` bez `startForeground` na emulatorze; BindException debug+release (1) — najpierw sprawdzić na świeżym debugu z main | Sonnet | — | `fix-debug-start` |

Błąd 8 (długie OK na mini-playerze uruchamia odtwarzanie) to pomyłka testu, nie błąd aplikacji. Wykrywanie sieci (mDNS) nie działa na emulatorze (NAT) — testować na TV 10.0.0.6.

SFix04a–d scalone 2026-11-15 (PR #36, #37, #35, #38). SFix04d: ANR i BindException nie odtworzone na świeżym debugu z `main` (tylko dokumentacja). Do ewentualnego SFix05: Back przy otwartym menu na Ustawieniach cofa do Startu (SFix04a); „Albumy · 0” — przyczyna nieustalona (SFix04b); sprawdzić na emulatorze fokus po Wstecz w Bibliotece/Źródłach i układ Teraz odtwarzane/Szczegółów (SFix04b/c bez wizualnej weryfikacji).
