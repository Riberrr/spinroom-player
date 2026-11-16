# Zasady wspólne dla sesji SUI (przebudowa interfejsu)

Czytasz: swój plik `SUI<nr>-*.md`, ten plik, „Wynik” sesji, od których zależysz, i wskazane PNG makiet z `design/concepts/png/` (1280×720 = rozmiar TV w dp). **Nie otwieraj** plików `design/concepts/*.html` (duże) ani `docs/UI-REDESIGN.md`, chyba że Twój plik każe.

## Styl (obowiązuje wszystkie ekrany)
- Tło z treści: rozmyty kolor z okładki/logo zaznaczonego elementu, przyciemniony ~60%, płynna zmiana ~300 ms. Komponent z `:core:design` (SUI1), nie własne implementacje.
- Szkło: powierzchnie półprzezroczyste (biel 6–12%), nie pełne `surface`. Fokus bez zmian: biała obwódka + akcent z motywu.
- Stacje radiowe: **kwadratowe** miniatury, ten sam rytm kafli co okładki muzyki. Brak logo → inicjały na kolorze.
- Kolory tylko z motywu (`SpinroomColors`/`MaterialTheme`), nigdy hex w `:feature:*`. Teksty po polsku w `strings.xml`.
- Każdy element osiągalny D-padem, `Wstecz` zawsze działa.

## Testy
1. Logika: testy JVM (`./gradlew :<moduł>:testDebugUnitTest -q`), na koniec `bash scripts/check.sh`.
2. Wygląd: **tylko emulator `emulator-5554`**, nigdy TV 10.0.0.6.
   - `bash scripts/emu-lock.sh acquire <ID>` przed instalacją, `release <ID>` zaraz po testach (jeden emulator, sesje równoległe).
   - `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` (**nigdy** uninstall: kasuje logowanie).
   - Zrzuty: `bash scripts/emu-shot.sh <nazwa> [klawisze]` (1280×720). Maks. 4 zrzuty na sesję, porównaj z PNG makiety.
   - Logi: `adb -s emulator-5554 logcat -d -s Spinroom:* AndroidRuntime:E | tail -80`.
   - Jeśli aplikacja pokazuje logowanie: dane testowe MA dostajesz w prompcie od koordynatora. Nigdy nie zapisuj ich w plikach, commitach, PR ani logach.
   - Emulator bez sieci po restarcie: `adb -s emulator-5554 shell cmd wifi connect-network AndroidWifi open`.

## Pliki
- Pisz tylko w modułach ze swojego pliku. Pliki koordynatora (`settings.gradle.kts`, `libs.versions.toml`, `app/build.gradle.kts`, `docs/STATUS.md`, `docs/SESSIONS.md`, `docs/UI-SESSIONS.md`) zmieniasz tylko, jeśli Twój plik wprost na to pozwala. Nowa biblioteka: dopisz na końcu sekcji i zaznacz w PR.
- Nowe komendy API i repozytoria w nowych plikach.

## Koniec
„Wynik” w swoim pliku (5–10 linii: co działa, publiczne API, pułapki, co dalej), jednolinijkowe decyzje w `docs/DECISIONS.md`, README zmienionych modułów, commit na `sui<nr>-<temat>`, push, PR (bez scalania). Napisz koordynatorowi jedno zdanie: numer PR i czy emulator zwolniony.
