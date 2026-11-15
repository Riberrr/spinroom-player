# SFix05 — Back z menu w Ustawieniach, „Albumy · 0”
**Model:** Sonnet · **Gałąź:** `fix-back-albums` · **Moduły:** `:app` (`SpinroomShell.kt`, `SpinroomNavHost`), `:feature:library`, `:core:data`/`:core:ma-api` tylko jeśli przyczyną jest licznik. Źródło: „Wynik” SFix04a i SFix04b.

## Błędy
1. Back przy otwartym menu bocznym na ekranie Ustawień cofa do Startu zamiast wrócić fokusem do treści ekranu. Prawdopodobnie kolejność `BackHandler`ów NavHost vs shell (SFix04a dodał w `SpinroomShell` Back-w-menu → `screenFocus.requestFocus()`). Ustal, czy to zachowanie jest sprzed SFix04a (sprawdź `git log`/diff #36), i sprawdź ten sam scenariusz na innych ekranach (Start, Biblioteka, Źródła, Szukaj). Oczekiwane: Back przy otwartym menu zwija menu i oddaje fokus treści; Back przy zwiniętym menu na ekranie głównym zachowuje dotychczasowe działanie (nie wychodzi z aplikacji niespodziewanie).
2. Biblioteka: chip „Albumy · 0” mimo że biblioteka ma utwory. SFix04b dodał `libraryCountLabel` (rozmiar strony z Paging, gdy siatka ma pozycje), ale przyczyny samego 0 z `count()` nie ustalił. Ustal, czy MA zwraca 0 dla albumów (np. inna kolejność/filtr `library_only`), czy licznik jest ładowany za wcześnie albo z błędnego wywołania; napraw u źródła, jeśli to błąd klienta.

## Zadania
Punkty 1–2, każdy osobno i lokalnie. Testy JVM dla logiki (np. licznik); Back i fokus sprawdź na emulatorze (release, `install -r`). Nie ruszaj innych ekranów „przy okazji”.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554 jest wolny i przydzielony Tobie (jedna sesja naraz); gdy skończysz, napisz do koordynatora „emulator wolny”. Użytkownik jest zalogowany na koncie Tv w wariancie release (`io.github.riberrr.spinroom`): instaluj tylko `-r`, NIE odinstalowuj, NIE wpisuj danych logowania. Nie uruchamiaj odtwarzania. Klawisze pojedynczo (aplikacja wolna); `adb input text` gubi znaki. Jeśli po restarcie emulatora brak sieci: `svc wifi enable` + `cmd wifi connect-network AndroidWifi open` (VPN-y na hoście mogą blokować sieć). Prawdziwego TV (10.0.0.6) nie dotykaj — nie uruchamiaj na nim ekranu.

## Gotowe, gdy
`bash scripts/check.sh` czyste (lint może paść na `local.properties` — środowisko), PR otwarty, nie scalony. W „Wynik” napisz, co zweryfikowałeś na emulatorze, a co tylko testem JVM.

## Wynik
_(uzupełnia sesja)_
