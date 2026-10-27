# Plan sesji — Spinroom Player

Cała budowa rozpisana na sesje. Każda sesja ma własny plik w `docs/sessions/` z celem, zakresem plików i kryterium „gotowe”.
Sesja czyta **tylko swój plik** + `CLAUDE.md` (ładuje się sam). Ten indeks czyta tylko koordynator.

## Jak pracujemy

1. **Czat-koordynator** (jeden, długo żyjący, model Sonnet, niski effort) — uruchamia sesje, sprawdza CI, scala PR-y po Twojej zgodzie, aktualizuje `docs/STATUS.md` i odhacza tabelę niżej. **Nie czyta kodu.** Prompt startowy: na dole pliku.
2. **Sesje robocze** — każda w osobnym worktree i na własnej gałęzi `s<nr>-<temat>`. Prompt startowy to jedno zdanie:
   `Zrób sesję S02 według docs/sessions/S02-protocol.md.`
3. **Koniec sesji** = PR z zielonym CI + uzupełniona sekcja „Wynik” w jej pliku (5–10 linii: co działa, co dalej, pułapki). To jest jedyne przekazanie wiedzy — następne sesje czytają „Wynik” zależności, nie kod.
4. **Nowe pliki zamiast wspólnych.** Komendy API i repozytoria dopisuje się w nowych plikach (`<Temat>Api.kt`, `<Temat>Repository.kt`).
5. **Pliki wspólne** (`settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `docs/STATUS.md`) zmienia tylko S00b i koordynator. Sesja, która naprawdę potrzebuje nowej biblioteki, dopisuje ją na końcu sekcji w katalogu wersji i zaznacza to w PR.
6. **Równolegle maks. 3 sesje.** Więcej to więcej przeglądania PR-ów i konfliktów, a nie szybciej.

## Fale (co może iść równocześnie)

```
Fala 0  S00b przygotowanie ───────────────────────────────────────────────┐
Fala 1  S01a Sendspin (streszczenie) │ S01b API MA (streszczenie)* │ S07 motyw + nawigacja
Fala 2  S02 protokół ← S01a          │ S04 audio ← S01a             │ S06 klient API ← S01b
Fala 3  S03 sieć + mDNS ← S02        │ S08 Teraz odtwarzane ← S06, S07
Fala 4  S05 usługa + pilot ← S03, S04  ══► tag v0.1.0 (TV gra jako odtwarzacz)
        S09 Start │ S10 Biblioteka │ S12 Źródła │ S13 Szukaj   ← S08  (po 3 naraz)
Fala 5  S11 Szczegóły ← S10 │ S14a wygaszacz + autostart ← S05, S08 │ S14c kalibracja opóźnienia ← S05
        ══► tag v0.2.0 (wszystkie ekrany)
Fala 6  S14b ulubione + menu OK ← S09–S13 (sama, dotyka wielu ekranów)  ══► tag v0.3.0
Fala 7  S15 wydajność ──► S16 wydanie  ══► tag v1.0.0
```
\* S01b wymaga działającego serwera MA ≥ 2.10.7 i Twoich danych (patrz „Twoje zadania”).

## Wszystkie sesje

| ✓ | Sesja | Plik | Zależy od | Model | Wersja po scaleniu |
| --- | --- | --- | --- | --- | --- |
| ✅ | S00 Bootstrap | — | — | — | — |
| ✅ | S00b Przygotowanie do pracy równoległej | `S00b-parallel-prep.md` | S00 | Sonnet | — |
| ✅ | S01a Streszczenie Sendspin 1.0 | `S01a-sendspin-digest.md` | S00b | Sonnet | — |
| ✅ | S01b Streszczenie API MA | `S01b-ma-api-digest.md` | S00b, serwer MA | Sonnet | — |
| ✅ | S07 Motyw, Inter, nawigacja, mini-odtwarzacz | `S07-ui-shell.md` | S00b | Sonnet | — |
| ⬜ | S02 Protokół Sendspin | `S02-protocol.md` | S01a | **Opus** | — |
| ⬜ | S04 Silnik audio | `S04-audio.md` | S01a | **Opus** | — |
| ⬜ | S06 Klient API MA + dane | `S06-ma-api.md` | S01b | Sonnet | — |
| ⬜ | S03 Sieć i wykrywanie | `S03-network.md` | S02 | **Opus** | — |
| ⬜ | S08 Teraz odtwarzane (ekran-wzorzec) | `S08-now-playing.md` | S06, S07 | Sonnet | — |
| ⬜ | S05 Usługa odtwarzania i pilot | `S05-service.md` | S03, S04 | **Opus** | **v0.1.0** |
| ⬜ | S09 Ekran Start | `S09-home.md` | S08 | Sonnet | — |
| ⬜ | S10 Biblioteka | `S10-library.md` | S08 | Sonnet | — |
| ⬜ | S12 Źródła (wtyczki) | `S12-sources.md` | S08 | Sonnet | — |
| ⬜ | S13 Szukaj | `S13-search.md` | S08 | Sonnet | — |
| ⬜ | S11 Szczegóły albumu/wykonawcy | `S11-details.md` | S10 | Sonnet | **v0.2.0** (gdy S08–S13 scalone) |
| ⬜ | S14a Wygaszacz i autostart | `S14a-screensaver.md` | S05, S08 | Sonnet | — |
| ⬜ | S14b Ulubione i menu kontekstowe | `S14b-favorites.md` | S09–S13 | Sonnet | — |
| ⬜ | S14c Kalibracja opóźnienia dźwięku | `S14c-latency.md` | S05 | **Opus** | **v0.3.0** (razem z S14a, S14b) |
| ⬜ | S15 Wydajność | `S15-performance.md` | wszystkie ekrany | Sonnet | — |
| ⬜ | S16 Wydanie 1.0 | `S16-release.md` | S15 | Sonnet | **v1.0.0** |
| — | SFix Poprawki z testów na TV | `SFix-template.md` | dowolna | Sonnet | patch `x.y.z` |

## Twoje zadania (poza Claude)

| Kiedy | Co |
| --- | --- |
| przed S01b | Zaktualizuj MA do ≥ 2.10.7. Podaj adres serwera, wersję, login testowy (hasło wpisujesz sam, nie w czacie). |
| przed S03 | Włącz na TV opcje programisty i debugowanie sieciowe; podaj IP telewizora i model. |
| przed v0.1.0 | Wygeneruj klucz podpisu (`keytool`, instrukcja w S05) i wklej do GitHub Secrets. Zrób kopię klucza poza repo. |
| po każdym tagu | Zainstaluj APK na TV, zgłoś błędy koordynatorowi → sesje SFix. |

## Oszczędzanie tokenów — skrót

- Prompt sesji = jedno zdanie; sesja czyta 1 plik planu + „Wynik” zależności. Koordynator nie czyta kodu.
- Opus tylko do protokołu, audio i synchronizacji (S02–S05, S14c). Reszta Sonnet.
- Zrzut makiety otwierać tylko dla budowanego ekranu (`design/screens/<nr>-*.png`).
- Logi: `bash scripts/check.sh`, `gh run view --log-failed | tail -60`, `adb logcat` z filtrem.
- Po scaleniu sesja się kończy. Poprawki = nowa sesja SFix z opisem błędu, nie kontynuacja starej rozmowy.

## Prompt dla czatu-koordynatora

```
Jesteś koordynatorem budowy Spinroom Player (repo w tym folderze).
Czytasz tylko docs/SESSIONS.md, docs/STATUS.md i sekcje „Wynik” w docs/sessions/*. Nie czytasz kodu.
1. Sprawdź tabelę w docs/SESSIONS.md i wybierz sesje, których zależności są scalone (maks. 3 naraz).
2. Każdą uruchom jako osobną sesję w nowym worktree, z promptem: „Zrób sesję <ID> według docs/sessions/<plik>.” i modelem z tabeli.
3. Gdy sesja zgłosi PR: sprawdź CI, pokaż mi 2–3 zdania podsumowania i zapytaj o scalenie. Po mojej zgodzie scal (squash), odhacz ✓ w tabeli, nadpisz docs/STATUS.md (≤ 40 linii) i uruchom kolejne sesje.
4. Przed sesją, która wymaga moich danych (tabela „Twoje zadania”), zapytaj mnie o nie.
5. Po scaleniu sesji oznaczonej wersją przypomnij o tagu i teście na TV.
```
