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
_(uzupełnia sesja)_
