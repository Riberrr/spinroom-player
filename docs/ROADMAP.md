# Spinroom Player — roadmapa

Etapy, sesje i wdrożenie. Sesja czyta tylko swój wiersz z sekcji 2. Zasady pracy: `CLAUDE.md`. Branding: `docs/BRAND.md`. Założenia aplikacji: `docs/PROJECT.md`.

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

Każdy etap kończy się APK do testu na TV i tagiem wersji. Szacunek: **~16 sesji**.

### Etap 0 — przygotowanie (bez kodu aplikacji)

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S0** Bootstrap repo ✅ | Repo, szkielet Gradle (`:app` + `:core:*`), `CLAUDE.md`, `docs/*`, `.claude/settings.json`, `scripts/check.sh`, CI + release, zrzuty makiet w `design/screens/`, ikona i banner | Lokalnie i w CI buduje się APK „Hello Spinroom” |
| **S1** Streszczenia | Przeczytać spec Sendspin 1.0 → `docs/SENDSPIN.md`. Pobrać `api-docs` z serwera → `docs/MA-API.md` (potwierdzić komendy z tabeli w projekcie). Rozstrzygnąć kwestię Noise KK | Oba pliki ≤ 250 linii, wszystkie komendy potwierdzone |

### Etap 1 — odtwarzacz Sendspin → **v0.1.0**

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S2** Protokół | `:core:sendspin`: modele wiadomości (kotlinx.serialization), maszyna stanów połączenia, Noise, sync zegara. Testy JVM na nagranych przykładach | Testy przechodzą bez urządzenia |
| **S3** Sieć | Serwer Ktor `/sendspin` na 8928, ogłoszenie mDNS `_sendspin._tcp`, zapasowe połączenie do `:8927` | TV widoczny w MA jako „Salon TV” |
| **S4** Dźwięk | `:core:audio`: dekodery FLAC/Opus (MediaCodec), bufor wg znaczników czasu, AudioTrack, kompensacja opóźnienia | Muzyka puszczona z telefonu gra z TV, równo z innym głośnikiem |
| **S5** Usługa i pilot | `MediaSessionService`, powiadomienie, przyciski multimedialne, autoodnowienie połączenia | Gra przy wygaszonym ekranie, Play/⏭ działają → **tag v0.1.0** |

### Etap 2 — API MA i Teraz odtwarzane → **v0.2.0**

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S6** Klient API | `:core:ma-api`: WebSocket `:8095/ws`, logowanie → token długoterminowy w DataStore, zdarzenia `player_updated`/`queue_updated` jako Flow | Testy z nagranymi odpowiedziami serwera |
| **S7** Motyw i szkielet UI | `:core:design` (kolory już są w `SpinroomColors`; dodać typografię Inter i wspólne kafelki), menu boczne, mini-odtwarzacz, ekran logowania/wyboru serwera | Nawigacja pilotem między pustymi ekranami |
| **S8** Teraz odtwarzane | Pełny ekran: okładka, rozmyte tło, postęp, sterowanie, kolejka, losowo/powtarzanie | Wzorcowy ekran → **tag v0.2.0** |

### Etap 3 — Biblioteka i Szczegóły → **v0.3.0**

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S9** Start | Wiersze kafelków (ostatnio odtwarzane, playlisty, radio) | Działa na danych z serwera |
| **S10** Biblioteka | Zakładki, siatka z stronicowaniem (Paging 3), skok po literze, małe miniatury | Płynnie przy 2000+ albumów |
| **S11** Szczegóły | Album / wykonawca, Odtwórz / Losowo / Do kolejki, menu kontekstowe (długie OK) | → **tag v0.3.0** |

### Etap 4 — Źródła i Szukaj → **v0.4.0**

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S12** Źródła | `music/browse` — drzewo providerów (radio, YouTube Music), odtwarzanie stacji | Wtyczki widoczne bez osobnego kodu |
| **S13** Szukaj | Klawiatura ekranowa, wyniki wg typu i źródła, wyszukiwanie głosowe (`RecognizerIntent`) | → **tag v0.4.0** |

### Etap 5 — szlif i wydanie → **v1.0.0**

| Sesja | Zadanie | Gotowe, gdy |
| --- | --- | --- |
| **S14** Szlif | Wygaszacz po 60 s, ulubione, kalibracja opóźnienia dźwięku, autostart po włączeniu TV, banner w launcherze | Wszystkie punkty zakresu z projektu |
| **S15** Wydajność | Profil na najsłabszym urządzeniu, baseline profile, R8 | Brak przycięć w siatkach |
| **S16** Wydanie | Podpisywanie, workflow wydań, README z instrukcją instalacji, zrzuty ekranu, CHANGELOG | → **tag v1.0.0** na GitHub Releases |

---

## 3. Wdrożenie i dystrybucja

### Gałęzie i wersje

- `main` zawsze buduje się zielono. Każda sesja pracuje na gałęzi `s<nr>-<krótki-opis>` → PR → merge. Moduły `:feature:*` zakłada sesja, która buduje dany ekran.
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
| Sesje „gubią” kontekst | `STATUS.md` + `DECISIONS.md` aktualizowane na koniec każdej sesji |
