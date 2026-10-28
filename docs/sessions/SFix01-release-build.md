# SFix01 — Build release nie przechodzi (v0.2.0)
**Model:** Sonnet · **Gałąź:** `sfix01-release-build`

## Błąd
Workflow Release dla tagu v0.2.0 (run 40714938288) pada na `:feature:{details,home,library,nowplaying}:compileReleaseKotlin` — `Unresolved reference 'Preview'` / `'tooling'`. `ui-tooling-preview` było tylko w `debugImplementation`, a CI budował wyłącznie wariant debug.

## Zadania
1. `implementation(libs.androidx.compose.ui.tooling.preview)` w modułach `:feature:*` używających `@Preview` (`ui-tooling` zostaje w debug).
2. CI i `scripts/check.sh` budują też `assembleRelease`.

## Gotowe, gdy
`./gradlew assembleRelease` przechodzi lokalnie, CI w PR zielone.

## Wynik
- `ui-tooling-preview` jako `implementation` w `:feature:{details,home,library,nowplaying,search,sources}` (`:core:design` i `:app` już miały).
- Drugi błąd release: R8 `Missing class java.lang.management.*` (Ktor) → `-dontwarn` w `app/proguard-rules.pro`.
- CI i `scripts/check.sh` budują teraz też `assembleRelease` (klucz debug, bez sekretów).
- `./gradlew assembleRelease` lokalnie: BUILD SUCCESSFUL. Nie uruchamiano zminimalizowanego APK na TV (R8 może wymagać keep rules dla serializacji/Hilt — sprawdzić przy teście v0.2.1).
- Edytowane pliki współdzielone: `.github/workflows/ci.yml`, `scripts/check.sh`.
