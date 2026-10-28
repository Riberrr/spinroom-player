# S16 — Wydanie 1.0
**Model:** Sonnet · **Zależy od:** S15 · **Gałąź:** `s16-release`

## Zadania
1. Sprawdź, że wydanie podpisuje się prawdziwym kluczem z Secrets (aktualizacja v0.x → v1.0 bez odinstalowania).
2. README: zrzuty ekranu z emulatora, instalacja, Obtainium, wymagania; `CHANGELOG.md` od v0.1.0.
3. Ekran „O aplikacji”: wersja, licencje (MIT, Inter OFL, biblioteki), dopisek o nieoficjalnym kliencie.
4. Opcjonalnie: metadane F-Droid (`fastlane/metadata`).

## Gotowe, gdy
Koordynator taguje **v1.0.0**, APK w GitHub Releases instaluje się na TV.

## Wynik
- Ekran „O aplikacji” (`AboutScreen`, `:feature:settings`): wersja z PackageManager, notka o nieoficjalnym kliencie, licencje (MIT, Inter OFL, Apache 2.0 bibliotek). Wejście z przycisku w Ustawieniach, Back wraca.
- `CHANGELOG.md` (0.2.0–1.0.0; tagu v0.1.0 nie ma, jego zakres jest w 0.2.0), README (Obtainium, pierwsze uruchomienie, makiety), `fastlane/metadata/android` (pl-PL, en-US; bez ikon i zrzutów).
- `release.yml`: tag inny niż `v0.*` bez kluczy podpisu kończy się błędem zamiast cichego podpisu kluczem debug (inaczej v1.0.0 nie zaktualizowałoby się nad v0.x z prawdziwym kluczem).
- Zadanie 1 (podpis prawdziwym kluczem, aktualizacja v0.x → v1.0) NIE sprawdzone: brak sekretów SIGNING_* w GitHub. Uwaga: v0.x były podpisane kluczem debug z CI, więc pierwsza aktualizacja do v1.0.0 wymaga odinstalowania; potem już nie.
- Zrzuty z emulatora nie zrobione (README linkuje makiety z `design/screens`).
- Zweryfikowano: `testDebugUnitTest` + `assembleDebug` zielone. Lint lokalnie padał tylko na moim `local.properties` (PropertyEscape), nie na kodzie; CI to sprawdzi. Brak tagu — taguje koordynator.
