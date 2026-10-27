# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S00b (27.10.2026)._

## Działa
- Szkielet Gradle: `:app` + `:core:{design,sendspin,audio,ma-api,data}` (core puste, z README).
- 7 modułów `:feature:*` jako zaślepki (`<Nazwa>Screen`, `DetailsScreen(itemUri)`).
- Nawigacja: `SpinroomNavHost` + `Routes` (start = `Routes.HOME`, `Routes.details(uri)` koduje URI).
- Hilt + KSP w `:app`.
- Katalog wersji ma już biblioteki dla kolejnych sesji: OkHttp, Ktor, kotlinx.serialization, Media3, Coil, DataStore, Paging.
- `assembleDebug` + testy przechodzą lokalnie i w CI. Ikona, banner TV, `SpinroomColors`/`SpinroomTheme`.
- Wersja z tagu: CI przekazuje `-PappVersion=1.2.3` → versionCode 10203. Lokalnie `0.0.0-dev`.
- `release.yml` podpisuje APK kluczem z Secrets (na razie kluczem debug).

## Nie sprawdzone
- APK nie był uruchomiony na TV ani emulatorze.
- Tymczasowy pasek przycisków między zaślepkami (`WithStubNav`) nie był sprawdzony pilotem.

## W toku
- Fala 1: S01a (streszczenie Sendspin), S01b (streszczenie API MA), S07 (motyw, nawigacja, mini-odtwarzacz).
- Serwer MA do testów: `http://10.0.1.50:8095/`, wersja 2.10.8.

## Następne
- Fala 2 po S01a/S01b: S02, S04 (← S01a), S06 (← S01b).
- Od użytkownika przed S03: IP i model TV, debugowanie sieciowe włączone.

## Pułapki
- `local.properties`: `sdk.dir` z ukośnikami `/`.
- AGP 9 ma wbudowany Kotlin — nie dodawać pluginu `kotlin-android`.
- Nowsze androidx (lifecycle 2.11, nav 2.10, hilt-nav 1.4) wymagają compileSdk 37 + AGP 9.1 — przypięte do 2.10.0 / 2.9.8 / 1.3.0.
- Na Windowsie pilnować bitu wykonywalności `gradlew` i `scripts/*.sh` (`git update-index --chmod=+x`).
