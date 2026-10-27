# Decyzje

Jedna linia na decyzję, najnowsze na dole. Format: `RRRR-MM-DD: decyzja — powód`.

- 2026-10-27: Nazwa Spinroom Player, w launcherze „Spinroom”, opis „for Music Assistant” — brzmi po angielsku, nawiązuje do Sendspin, nie podszywa się pod MA.
- 2026-10-27: Ikona: koncepcja nr 10 (płyta z bursztynową etykietą-domem) — subtelne nawiązanie do logo MA.
- 2026-10-27: Licencja MIT, repo publiczne `Riberrr/spinroom-player`.
- 2026-10-27: applicationId `io.github.riberrr.spinroom` (debug: `.debug`), minSdk 26, targetSdk 36.
- 2026-10-27: Budujemy tylko pod Sendspin 1.0 (MA ≥ 2.10.7), bez wersji „technical preview”.
- 2026-10-27: Moduły `:feature:*` powstają w sesji, która buduje dany ekran — mniej pustego kodu. (Zmienione niżej.)
- 2026-10-27: Inter dołączamy w S7 (motyw); S0 używa domyślnego fontu.
- 2026-10-27: versionCode = major·10000 + minor·100 + patch, liczony z tagu `v*` w CI.
- 2026-10-27: Praca równoległa: koordynator + sesje w osobnych worktree, maks. 3 naraz; plan w `docs/SESSIONS.md`, instrukcje w `docs/sessions/`.
- 2026-10-27: Zaślepki wszystkich `:feature:*`, katalog wersji, Hilt i nawigacja powstają z góry w S00b — żeby sesje równoległe nie edytowały wspólnych plików.
- 2026-10-27: Kontrakty `AudioChunk`/`StreamFormat`/`ClockSource` pisze S01a, żeby S02 i S04 mogły iść równolegle.
- 2026-10-27: Claude nie dostaje haseł ani tokenów MA w czacie; nagrania API robi skrypt uruchamiany przez użytkownika.
- S00b: androidx versions pinned to what compileSdk 36 / AGP 9.0.1 accept (lifecycle 2.10.0, navigation-compose 2.9.8, hilt-navigation-compose 1.3.0); newer ones need compileSdk 37 + AGP 9.1.
- S00b: details route is `details/{itemUri}` with the MA URI URL-encoded (`Routes.details(uri)`); `WithStubNav` button row in `:app` is temporary scaffolding, remove per screen.
- S07: Inter bundled (4 static TTFs, OFL); icons as hand-copied Material vector paths (no icons-extended); menu expands on focus, left at content edge opens it; mini player state is placeholder until S08.
- S01b: klient MA używa WS (`auth` z tokenem po server_info); login przez `POST /auth/login`, potem `auth/token/create` (1 rok) do DataStore; fixtures nagrywa `scripts/record-fixtures.sh` (Node 22+), ⚠️ w docs/MA-API.md = do potwierdzenia po nagraniu.
- S01a: Spec Sendspin `main` wymaga Noise `KKpsk2` (TV = serwer WS 8928 + mDNS `_sendspin._tcp`); parowanie metodą Pairing PSK (token `SP:0…` + QR w Ustawieniach), `unpaired_access` domyślnie włączony; Noise: `org.signal.forks:noise-java:0.1.1` (do weryfikacji w S02), zestaw ChaChaPoly.
- S01a: Role v1: player (flac > opus > pcm), metadata, artwork (1 kanał), controller; bez visualizer/color/source.
- S06: OkHttp przypięty do 5.1.0 (5.5.x wymaga compileSdk 37, AGP 9.0.1 wspiera 36); `ConnectionManager.start()` musi wywołać aplikacja; modele MA są jedną tolerancyjną klasą `MediaItem` (ItemMapping + pełne obiekty); `MaClient` niczego nie loguje.
