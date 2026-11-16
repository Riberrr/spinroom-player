# Plan sesji — przebudowa interfejsu (SUI)

Indeks dla koordynatora. Specyfikacja: `docs/UI-REDESIGN.md` (koordynator jej nie czyta, sesje mają wszystko w swoich plikach). Zasady wspólne sesji: `docs/sessions/SUI-common.md`. Makiety do porównań: `design/concepts/png/*.png`.

## Fale (maks. 3 sesje naraz, jeden emulator → blokada `scripts/emu-lock.sh`)

```
Fala 0  SUI0 moduły radio/playlists (Haiku)
Fala 1  SUI1 fundament stylu (Opus)                 ← SUI0 niekonieczne, ale SUI0 jest krótka: najpierw ona
Fala 2  SUI2 szkielet + mini-odtwarzacz (Opus) │ SUI3 Start │ SUI5 Playlisty + Biblioteka     ← SUI1
Fala 3  SUI4 Radio ← SUI2 │ SUI6 Szukaj ← SUI5 │ SUI8a Ustawienia ← SUI1
Fala 4  SUI7 Odtwarzacz ← SUI4 │ SUI8b Przyciski pilota ← SUI2, SUI8a
Fala 5  SUI9 CH± poza aplikacją (Opus) ← SUI7, SUI8b
Fala 6  SUIv przegląd całości ══► tag v1.1.0
```
W fali 2 tylko SUI2 zmienia `:core:design`; SUI3 i SUI5 zgłaszają braki w „Wynik”.

## Sesje

| ✓ | Sesja | Plik | Zależy od | Model | Effort |
| --- | --- | --- | --- | --- | --- |
| ✓ | SUI0 Moduły Radio i Playlisty | `SUI0-modules.md` | — | haiku | low |
| ✓ | SUI1 Fundament stylu | `SUI1-design-foundation.md` | — | **opus** | high |
| | SUI2 Szkielet, menu, wysuwany odtwarzacz, pilot | `SUI2-shell.md` | SUI0, SUI1 | **opus** | high |
| | SUI3 Start 03B-1 | `SUI3-home.md` | SUI1 | sonnet | medium |
| | SUI5 Playlisty + Biblioteka + filtry | `SUI5-playlists-library.md` | SUI0, SUI1 | sonnet | medium |
| | SUI4 Radio | `SUI4-radio.md` | SUI1, SUI2 | sonnet | high |
| | SUI6 Szukaj | `SUI6-search.md` | SUI1, SUI5 | sonnet | medium |
| | SUI8a Ustawienia, nazwa, Wygląd | `SUI8a-settings.md` | SUI1 | sonnet | medium |
| | SUI7 Odtwarzacz 40:60 | `SUI7-now-playing.md` | SUI1, SUI2, SUI4 | sonnet | high |
| | SUI8b Przyciski pilota | `SUI8b-remote-keys.md` | SUI2, SUI8a | sonnet | medium |
| | SUI9 CH± poza aplikacją | `SUI9-global-keys.md` | SUI7, SUI8b | **opus** | high |
| | SUIv Przegląd na emulatorze | `SUIv-verify.md` | wszystkie | sonnet | low |
| | SFix-UI… poprawki z SUIv | `SFix-template.md` | SUIv | haiku (drobne) / sonnet | low–medium |

Dobór modelu: **Haiku** do mechanicznych zmian (szkielety modułów, teksty, drobne SFix z jasnym opisem), **Sonnet** do ekranów, **Opus** tylko tam, gdzie błąd kosztuje wszystkich (fundament stylu, nawigacja i fokus w powłoce, usługa systemowa). Gdy sesja Sonnet utknie dwa razy na tym samym, koordynator może ją zakończyć i uruchomić tę samą sesję na Opus z jednym zdaniem o problemie.

## Prompt dla koordynatora

```
Jesteś koordynatorem przebudowy interfejsu Spinroom Player (repo w tym folderze). Oszczędzasz tokeny.
Czytasz tylko: docs/UI-SESSIONS.md, docs/STATUS.md i sekcje „Wynik” w docs/sessions/SUI*.md. Nie czytasz kodu, makiet ani PNG.

Start:
0. Jeśli docs/UI-SESSIONS.md nie ma na main, PR z gałęzią planu (claude/radio-app-theme-designs-*) czeka na scalenie: pokaż mi go i scal po mojej zgodzie. Dopisz jeden wiersz do docs/SESSIONS.md: „SUI — przebudowa UI → docs/UI-SESSIONS.md”.

Pętla:
1. Wybierz z tabeli sesje, których zależności są scalone (maks. 3 naraz, kolejność fal).
2. Każdą uruchom jako osobną sesję w nowym worktree, z modelem i effortem z tabeli, promptem:
   „Zrób sesję <ID> według docs/sessions/<plik>. Zasady: docs/sessions/SUI-common.md.”
   Jeśli sesja testuje na emulatorze, dopisz do promptu dane logowania MA z pamięci (ma-test-server, konto TV): „Gdyby aplikacja na emulatorze prosiła o logowanie: serwer http://10.0.1.50:8095, login …, hasło … — nie zapisuj ich nigdzie.”
3. Nie odpytuj sesji. Gdy sesja zgłosi PR: `gh pr checks <nr>`; przy błędzie `gh run view --log-failed | tail -60` i odeślij to do tej samej sesji. Przy zielonym CI pokaż mi 2–3 zdania z „Wynik” i zapytaj o scalenie. Po zgodzie: squash merge, odhacz ✓ w tabeli, nadpisz docs/STATUS.md (≤ 40 linii), uruchom kolejne sesje.
4. Konflikt przy scalaniu: odeślij do sesji „zrób rebase na main i rozwiąż konflikty”. Sam nie rozwiązujesz.
5. Testy tylko na emulatorze emulator-5554, nigdy na TV 10.0.0.6. Jeśli `bash scripts/emu-lock.sh status` pokazuje blokadę starszą niż 60 min, a jej właściciel skończył pracę, zwolnij ją.
6. Po SUIv: z listy w „Wynik” zrób sesje SFix-UI (szablon docs/sessions/SFix-template.md), drobne na Haiku. Na końcu przypomnij mi o tagu v1.1.0 i teście na TV.
7. Pytaj mnie tylko o: scalenie PR, decyzje produktowe zgłoszone przez sesję, dane, których nie ma w pamięci.
```
