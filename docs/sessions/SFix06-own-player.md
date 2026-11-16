# SFix06 — Ustawienia: rozpoznanie własnego odtwarzacza na liście
**Model:** Sonnet · **Gałąź:** `fix-own-player` · **Moduły:** `:feature:settings` (`PlayerRows.kt`), `:core:ma-api` (modele graczy), `:core:data` (tożsamość Sendspin / wybrany odtwarzacz). Źródło: test na TV 2026-11-16, „Wynik” SFix04a.

## Błąd (TV 10.0.0.6, MA 2.10.12, build debug z `main`)
Lista odtwarzaczy w Ustawieniach miała: „Odtwarzacz i tv”, „Sync Group Player”, **dwa** wpisy „Telewizor / 2020/2021 UHD Android TV” (jeden „wybrany”). Etykieta „To urządzenie” (SFix04a, `buildPlayerRows`) **nie pojawiła się** na żadnym wpisie. Na TV były wtedy zainstalowane równolegle wydanie 1.0.0 (`io.github.riberrr.spinroom`) i debug (`.debug`), każde z własną tożsamością Sendspin — stąd prawdopodobnie dwa wpisy; kopia debug została już odinstalowana.

## Zadania
1. Ustal, czym jest `player_id` własnego odtwarzacza w MA względem tożsamości Sendspin zapisanej w DataStore (`SendspinCredentials` / klucz klienta). `buildPlayerRows` porównuje id z kluczem — sprawdź w logach (`Spinroom: sendspin: group: Telewizor`, `player id set to this TV`) i w dokumencie `docs/MA-API.md`/`docs/SENDSPIN.md`, jak MA nazywa gracza Sendspin (może `player_id` ≠ `client_id`, np. z prefiksem). Napraw dopasowanie tak, aby własny odtwarzacz dostał „To urządzenie” i był pierwszy.
2. Gdy MA zwraca kilka graczy o tej samej nazwie i modelu, rozróżnij je (np. drugą linią: model · krótki identyfikator, albo „nieaktywny”/niedostępny), by użytkownik widział, który jest który. Nie usuwaj niczego z serwera.
3. Test JVM na dopasowanie własnego id (warianty z prefiksem/bez) i na duplikaty nazw.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554 jest wolny i przydzielony Tobie (jedna sesja naraz); emulator jest zalogowany na konto Tv w wariancie release (`io.github.riberrr.spinroom`): instaluj tylko `-r`, NIE odinstalowuj, NIE wpisuj danych logowania, nie uruchamiaj odtwarzania. Klawisze pojedynczo. Na emulatorze MA nie widzi odtwarzacza emulatora (NAT), więc lista nie pokaże własnego wpisu — własne dopasowanie sprawdzaj testem JVM i logami. Prawdziwego TV (10.0.0.6) NIE dotykaj i nie uruchamiaj na nim ekranu — jeśli potrzebujesz próby na TV, poproś koordynatora, a ten zapyta użytkownika.

## Gotowe, gdy
`bash scripts/check.sh` czyste (lint może paść na `local.properties` — środowisko; build bez niego: `ANDROID_HOME=C:/Users/lukas/AppData/Local/Android/Sdk`), PR otwarty, nie scalony. W „Wynik” opisz, co sprawdzone testem, a co wymaga próby na TV.

## Wynik
_(uzupełnia sesja)_
