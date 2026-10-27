# Decyzje

Jedna linia na decyzję, najnowsze na dole. Format: `RRRR-MM-DD: decyzja — powód`.

- 2026-10-27: Nazwa Spinroom Player, w launcherze „Spinroom”, opis „for Music Assistant” — brzmi po angielsku, nawiązuje do Sendspin, nie podszywa się pod MA.
- 2026-10-27: Ikona: koncepcja nr 10 (płyta z bursztynową etykietą-domem) — subtelne nawiązanie do logo MA.
- 2026-10-27: Licencja MIT, repo publiczne `Riberrr/spinroom-player`.
- 2026-10-27: applicationId `io.github.riberrr.spinroom` (debug: `.debug`), minSdk 26, targetSdk 36.
- 2026-10-27: Budujemy tylko pod Sendspin 1.0 (MA ≥ 2.10.7), bez wersji „technical preview”.
- 2026-10-27: Moduły `:feature:*` powstają w sesji, która buduje dany ekran — mniej pustego kodu.
- 2026-10-27: Inter dołączamy w S7 (motyw); S0 używa domyślnego fontu.
- 2026-10-27: versionCode = major·10000 + minor·100 + patch, liczony z tagu `v*` w CI.
