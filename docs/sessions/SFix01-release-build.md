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
_(uzupełnia sesja)_
