# Status

_Prowadzi koordynator po każdym scaleniu. Ostatnio scalone: S01a, S01b, S07 (27.10.2026)._

## Działa
- Szkielet Gradle: `:app`, `:core:*`, 7 modułów `:feature:*` (zaślepki ekranów), Hilt + KSP.
- Powłoka UI (`SpinroomShell`): menu boczne, `SpinroomNavHost`, mini-odtwarzacz (stan przykładowy). Zgodna z makietą `1-start.png` na emulatorze TV.
- `:core:design`: Inter (OFL), typografia TV, ikony, komponenty (CoverTile, SectionRow, StationTile, SideMenu, MiniPlayerBar, EmptyState, ErrorState, FocusSurface) — API w README.
- `docs/SENDSPIN.md` + kontrakty `core/sendspin/.../api/` + 30 fixtures protokołu.
- `docs/MA-API.md` (MA 2.10.8) + `scripts/record-fixtures.sh`.
- CI: `assembleDebug` + testy zielone. Wersja z tagu → versionCode.

## Nie sprawdzone
- APK nie był uruchomiony na prawdziwym TV.
- Fixtures MA nie są nagrane — pozycje ⚠️ w `docs/MA-API.md` niepotwierdzone.
- Czy MA 2.10.8 mówi Noise i czy `noise-java` obsługuje `KKpsk2` (S02).

## W toku
- Fala 2: S02 protokół (Opus), S04 audio (Opus), S06 klient API MA (Sonnet).

## Następne
- S03 ← S02, S08 ← S06 + S07.
- Od użytkownika: uruchomić `scripts/record-fixtures.sh` z `MA_TOKEN`; przed S03 IP i model TV + debugowanie sieciowe.

## Pułapki
- Spec Sendspin `main` wymaga Noise `KKpsk2` + parowania (nie zwykły WS). Opcji „legacy clients” w MA nie używamy.
- `Text` poza `Surface` jest czarny — `SpinroomTheme` ustawia `LocalContentColor`. Ekrany same żądają fokusu.
- Emulator domyślnie 960 dp — ustawić gęstość 480 dla 1280 dp.
- Nowsze androidx wymagają compileSdk 37 + AGP 9.1 — przypięte (lifecycle 2.10.0, nav 2.9.8, hilt-nav 1.3.0).
- `docs/DECISIONS.md` często daje konflikty przy równoległych PR — rozwiązywać sumą linii.
- Pilnować bitu wykonywalności `gradlew` i `scripts/*.sh` na Windowsie.
- `local.properties`: `sdk.dir` z `/`. AGP 9 ma wbudowany Kotlin.
