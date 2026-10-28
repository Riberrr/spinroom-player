# S15 — Wydajność i jakość
**Model:** Sonnet · **Zależy od:** wszystkie ekrany i S14* · **Gałąź:** `s15-performance`

## Zadania
1. Baseline profile dla startu i przewijania siatek; reguły R8 dla serializacji/Hilt/Ktor.
2. Rozmiary miniatur, cache Coil, zbędne rekompozycje w siatkach (Layout Inspector / metryki kompilatora).
3. Włącz `lintDebug` w CI i popraw błędy.
4. Test na najsłabszym urządzeniu użytkownika; wyniki w „Wynik”.

## Gotowe, gdy
Brak przycięć przy przewijaniu na najsłabszym urządzeniu; CI z lintem zielone.

## Wynik
- **Krytyczny bug (S14b):** `ItemMenuHost` używał `viewModel()` → aplikacja (debug i release) crashowała przy starcie na „Cannot create an instance of ItemMenuViewModel". Poprawione na `hiltViewModel()` (+ `hilt-navigation-compose` w `:core:design`). Release v0.3.0 z tego powodu się nie uruchamiał.
- R8: reguły dla kotlinx.serialization, nazw ViewModeli (Hilt), `-dontwarn` Ktor/slf4j/conscrypt. Release uruchomiony na emulatorze (API 31) i na TV 10.0.0.6 — startuje, łączy się z MA, bez crashy.
- Coil: singleton loader w `SpinroomApp` (pamięć 20%, dysk 100 MB); miniatury już są przeskalowane po stronie serwera (`ImageUrls`, 200 px w bibliotece).
- Baseline profile: ręczny `app/src/main/baseline-prof.txt` + `profileinstaller` (dodane do `libs.versions.toml`/`app/build.gradle.kts`). Pełny profil z macrobenchmarku — następna sesja.
- Lint: `lintDebug` w CI i `scripts/check.sh`; naprawione: POST_NOTIFICATIONS w manifeście, `@SuppressLint("RestrictedApi")` na `dispatchKeyEvent`. Lokalnie `local.properties` wymaga `C\:/...` (ostrzeżenie PropertyEscape tylko lokalnie).
- **Niezmierzone:** płynność przewijania biblioteki 2000+ albumów — TV co chwilę gubił połączenie adb. Do zrobienia ręcznie (`dumpsys gfxinfo`).
- Pułapka: debug (`.debug`) i release słuchają na tym samym porcie 8928 — dwa naraz = `BindException` (nieobsłużony, crash). Warto złapać wyjątek w `SendspinServer`.
- Zostaje: `:core:design` → `:core:data` (przez `ItemMenuViewModel`) nadal nie rozplątane.
