# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S00 (27.10.2026)._

## Działa
- Szkielet Gradle: `:app` + `:core:{design,sendspin,audio,ma-api,data}` (moduły core są puste, mają README).
- `assembleDebug` przechodzi lokalnie i w CI; ekran „Hello Spinroom” z ikoną, nazwą i wersją.
- Ikona adaptacyjna (koncepcja nr 10) + monochromatyczna, banner TV 320×180 (`design/banner.html` → `drawable-xhdpi/banner.png`).
- `SpinroomColors` / `SpinroomTheme` w `:core:design`.
- Wersja z tagu: CI przekazuje `-PappVersion=1.2.3` → versionCode 10203. Lokalnie `0.0.0-dev`.
- Workflow `release.yml` podpisuje APK kluczem z Secrets; dopóki go nie ma, kluczem debug (każde wydanie trzeba wtedy instalować po odinstalowaniu poprzedniego).

## Nie sprawdzone
- APK nie był jeszcze uruchomiony na TV ani emulatorze.

## Następne
- Plan sesji: `docs/SESSIONS.md` (prowadzi koordynator). Teraz: **S00b** (sama), potem fala 1: S01a, S01b, S07.
- Od użytkownika przed S01b: adres i wersja serwera MA (cel ≥ 2.10.7).

## Pułapki
- `local.properties`: `sdk.dir` z ukośnikami `/` (backslashe psują ścieżkę).
- AGP 9 ma wbudowany Kotlin — nie dodawać pluginu `kotlin-android`.
