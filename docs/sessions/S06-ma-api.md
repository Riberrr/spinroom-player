# S06 — Klient API MA i warstwa danych
**Model:** Sonnet · **Zależy od:** S01b · **Gałąź:** `s06-ma-api`

## Cel
Typowany klient WebSocket API MA i repozytoria, z których korzystają wszystkie ekrany.

## Przeczytaj
`docs/MA-API.md`, „Wynik” w `S01b-ma-api-digest.md`, fixtures, README `:core:ma-api` i `:core:data`.

## Zadania
1. `:core:ma-api`: klient OkHttp WS z korelacją `message_id`, timeoutami i ponownym łączeniem (backoff), uwierzytelnianie tokenem, komendy z `MA-API.md`, zdarzenia jako `Flow`. Każda grupa komend w osobnym pliku (`LibraryApi.kt`, `PlayerQueueApi.kt`…), żeby kolejne sesje dopisywały pliki zamiast edytować wspólne.
2. `:core:data`: `SettingsStore` (DataStore: adres serwera, token, id odtwarzacza TV), repozytoria `Player`, `Library` (z `PagingSource`), `Browse`, `Search` — każde w osobnym pliku. Moduły Hilt.
3. `:feature:settings`: prosty ekran adresu serwera i logowania (hasło wpisuje użytkownik na TV; zapisujemy tylko token).
4. Testy: fixtures + MockWebServer.

## Gotowe, gdy
Testy zielone; na TV/emulatorze logowanie zapisuje token i lista albumów pobiera się (log lub test); README obu modułów opisują API.

## Nie rób
Ekranów poza ustawieniami; nie loguj tokenu.

## Wynik
- `:core:ma-api`: `MaClient` (OkHttp WS, korelacja `message_id`, timeout 15 s, reconnect z backoffem 1→30 s, `state`/`events` jako Flow, scalanie `partial`, kod 23 = `Failed` bez ponawiania), grupy komend w osobnych plikach: `LibraryApi`, `BrowseApi`, `PlayerQueueApi`, `AuthApi` (login → token 1-roczny), `ImageUrls`; tolerancyjne modele w `MaModels.kt`. Szczegóły w README modułu.
- `:core:data`: `SettingsStore` (DataStore: adres, token, id gracza), `ConnectionManager`, `AuthRepository`, `LibraryRepository` (Paging, 50/stronę), `PlayerRepository` (kolejka/gracze na żywo ze zdarzeń), `BrowseRepository`, `SearchRepository`, Hilt w `di/DataModule.kt`.
- `:feature:settings`: ekran adresu i logowania (hasło tylko w pamięci, zapisywany token), status połączenia, po zalogowaniu pobiera liczbę albumów.
- Testy: 11 w ma-api (MockWebServer: auth, korelacja, partial, błędy, zdarzenia, reconnect, login), 3 w data. `bash scripts/check.sh` zielone.
- **Do potwierdzenia fixtures (⚠️):** kształt zdarzeń (`queue_updated` itd.) i wynik komendy `auth`, URL `imageproxy`, `order_by` (używam tylko `name`), prawdziwe `partial`, `players/all` vs `queue_id`. NIE testowano na żywym serwerze 10.0.1.50 (brak hasła/tokenu u sesji) — wymaga ręcznego sprawdzenia logowania na TV.
- Uwagi: OkHttp obniżony do 5.1.0 (5.5.1 wymaga compileSdk 37). Dodano `androidx-paging-common` na końcu `libs.versions.toml`. `ConnectionManager.start()` wywołuje dziś tylko `SettingsViewModel` — app powinna wołać go przy starcie (SFix/S-app). Test SettingsStore używa DataStore w pamięci (plikowy nie przechodzi na Windows).
