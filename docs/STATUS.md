# Status

_Nadpisywany na koniec każdej sesji. Ostatnia: S0 (27.10.2026)._

## Działa
- Szkielet Gradle: `:app` + `:core:{design,sendspin,audio,ma-api,data}` (moduły core są puste, mają README).
- `assembleDebug` przechodzi lokalnie i w CI; ekran „Hello Spinroom” z ikoną, nazwą i wersją.
- Ikona adaptacyjna (koncepcja nr 10) + monochromatyczna, banner TV 320×180 (`design/banner.html` → `drawable-xhdpi/banner.png`).
- `SpinroomColors` / `SpinroomTheme` w `:core:design`.
- Wersja z tagu: CI przekazuje `-PappVersion=1.2.3` → versionCode 10203. Lokalnie `0.0.0-dev`.
- Workflow `release.yml` podpisuje APK kluczem z Secrets; dopóki go nie ma, kluczem debug (każde wydanie trzeba wtedy instalować po odinstalowaniu poprzedniego).

## Nie sprawdzone
- APK nie był jeszcze uruchomiony na TV ani emulatorze.

## Następna sesja: S1 (streszczenia)
- Spec Sendspin 1.0 → `docs/SENDSPIN.md`; `http://<serwer>:8095/api-docs` → `docs/MA-API.md`.
- Rozstrzygnąć: Noise `KKpsk2` wymaga znanych kluczy — jak spec rozwiązuje parowanie?
- Potrzebne od użytkownika: adres serwera MA, wersja MA (cel ≥ 2.10.7), dane logowania do testów.

## Pułapki
- `local.properties`: `sdk.dir` z ukośnikami `/` (backslashe psują ścieżkę).
- AGP 9 ma wbudowany Kotlin — nie dodawać pluginu `kotlin-android`.
