# SFix04a — Ustawienia: fokus startowy i lista odtwarzaczy
**Model:** Sonnet · **Gałąź:** `fix-settings-focus` · **Moduły:** `:feature:settings`, ewentualnie `:core:ma-api` (modele graczy), `:core:data`. Źródło: raport SFix03v (`SFix03v-verify.md`, błędy 2 i 3).

## Błędy (emulator, release)
1. Po wejściu w Ustawienia (z menu →) fokus ląduje na „Poprzedni” w mini-odtwarzaczu zamiast w treści (na „Wyloguj” gdy zalogowany, inaczej na pierwszym polu). Sprawdź, dlaczego `focusRestorer`/pierwszy fokus z SFix03b nie działa na tym ekranie.
2. Lista odtwarzaczy ma duplikaty i stare wpisy („Telewizor” ×2, „Web (Chrome on Windows)”, „2020/2021 UHD Android TV”); nie widać, który wpis to to urządzenie. SFix03g dodał drugą linię (model · adres z `device_info`) — sprawdź, czy działa. Oznacz własny odtwarzacz (np. „To urządzenie”) i ukryj/oznacz wpisy niedostępne (`available=false`), jeśli MA to podaje.

## Zadania
Punkty 1–2, lokalnie. Test JVM dla logiki listy (kolejność, oznaczenie własnego, niedostępne). Nie ruszaj innych ekranów.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554 ma jedna sesja naraz — poproś koordynatora o przydział. Testuj wariant release (`io.github.riberrr.spinroom`), instaluj tylko `-r` (inny podpis/odinstalowanie wylogowuje użytkownika). Użytkownik jest zalogowany na koncie Tv; nie wpisuj danych logowania. Nie uruchamiaj odtwarzania. Aplikacja jest wolna: klawisze pojedynczo. Po restarcie emulatora Wi-Fi: `cmd wifi connect-network AndroidWifi open`.

## Gotowe, gdy
`bash scripts/check.sh` czyste (lint może paść na `local.properties` — to środowisko), PR otwarty, nie scalony.

## Wynik
_(uzupełnia sesja)_
