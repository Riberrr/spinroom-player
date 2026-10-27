# Spinroom Player — roadmapa

Etapy, sesje i wdrożenie. Sesje robocze tego pliku nie czytają — mają własne pliki w `docs/sessions/`. Zasady pracy: `CLAUDE.md`. Branding: `docs/BRAND.md`. Założenia aplikacji: `docs/PROJECT.md`.

---

## 1. Decyzje do podjęcia przed kodem (Etap 0)

Blokują Etap 1 — bez nich sesje będą zgadywać i palić tokeny.

- [ ] **Aktualizacja MA do ≥ 2.10.7** (Sendspin 1.0). Budujemy tylko pod 1.0, bez wsparcia wersji „technical preview”.
- [ ] Model TV / przystawki i wersja Android TV (wpływa na minSdk i testy wydajności).
- [ ] Logowanie w MA włączone? Login + hasło do testów.
- [ ] Lista zainstalowanych providerów (radio, YouTube Music, inne).
- [x] Nazwa **Spinroom Player** i ikona nr 10 — zatwierdzone 27.10.2026.
- [x] Repozytorium `github.com/Riberrr/spinroom-player` założone 27.10.2026 (publiczne, MIT, gałąź `main`, licencja MIT).
- [ ] Szybki test SendspinDroid na TV — potwierdza, że Sendspin działa w Twojej sieci, zanim napiszemy własny.
- [ ] Do zweryfikowania w specyfikacji: projekt podaje szyfrowanie Noise `KKpsk2`. Wzorzec KK wymaga wcześniejszej wymiany kluczy publicznych (parowanie) — sprawdzić, jak spec Sendspin 1.0 to rozwiązuje.

---
---

## 2. Etapy i sesje

Pełna lista sesji, fale pracy równoległej i prompt koordynatora: [`docs/SESSIONS.md`](SESSIONS.md). Szczegóły każdej sesji: `docs/sessions/<ID>-*.md`.

Wersje: **v0.1.0** TV gra jako odtwarzacz (S05) · **v0.2.0** wszystkie ekrany (S08–S13) · **v0.3.0** szlif (S14a–c) · **v1.0.0** wydanie (S16).

---

## 3. Wdrożenie i dystrybucja

### Gałęzie i wersje

- `main` zawsze buduje się zielono. Każda sesja pracuje w osobnym worktree na gałęzi `s<nr>-<krótki-opis>` → PR → koordynator scala (squash) po zgodzie użytkownika. Moduły `:feature:*` zakłada S00b jako zaślepki.
- Wersje SemVer; `versionCode` liczony z tagu (`v0.2.1` → `201`), więc nie trzeba go ręcznie podbijać.
- `CHANGELOG.md` aktualizowany przy każdym tagu.

### CI/CD (GitHub Actions)

| Workflow | Kiedy | Co robi |
| --- | --- | --- |
| `ci.yml` | każdy PR i push na `main` | testy jednostkowe, `assembleDebug`, APK jako artefakt (lint dochodzi w S15) |
| `release.yml` | tag `v*` | `assembleRelease` podpisany kluczem z GitHub Secrets, APK dołączony do GitHub Release z notatkami z CHANGELOG |

Klucz podpisu generujesz Ty, lokalnie (`keytool`), i wklejasz do Secrets jako base64 — Claude go nie widzi i nie przechowuje. Kopia zapasowa klucza poza repo: bez niego nie wydasz aktualizacji.

### Instalacja na TV

1. **Testy (każdy etap):** pobranie APK z GitHub Release → `adb connect <IP TV>` → `adb install -r spinroom.apk`.
2. **Codzienne użycie:** [Obtainium](https://github.com/ImranR98/Obtainium) na TV śledzi GitHub Releases i sam proponuje aktualizacje — bez adb.
3. **Później (opcjonalnie):** F-Droid (wymaga pełnego open source, mamy) lub Google Play (wymaga konta deweloperskiego i spełnienia wytycznych Android TV).

---

## 4. Ryzyka

| Ryzyko | Co robimy |
| --- | --- |
| MA zostaje na 2.10.5 | Etap 1 wstrzymany do aktualizacji — nie budujemy pod wersję przeznaczoną do usunięcia |
| Sync dźwięku na TV z soundbarem | Opóźnienie po stronie MA + kalibracja w aplikacji (S14) |
| Słabe przystawki | Paging, miniatury 200 px, test na najsłabszym urządzeniu w S15 |
| Zmiany w API MA między wersjami | Komendy tylko w `:core:ma-api`, potwierdzone w `docs/MA-API.md` z numerem wersji serwera |
| Sesje „gubią” kontekst | Sekcja „Wynik” w pliku sesji + `DECISIONS.md`; `STATUS.md` prowadzi koordynator |
| Konflikty przy pracy równoległej | S00b raz zmienia pliki wspólne; potem sesje dopisują nowe pliki; maks. 3 sesje naraz |
