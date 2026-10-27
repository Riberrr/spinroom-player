# S01b — Streszczenie API Music Assistant
**Model:** Sonnet · **Zależy od:** S00b, serwer MA ≥ 2.10.7 · **Gałąź:** `s01b-ma-api-digest`

## Cel
Potwierdzić na prawdziwym serwerze komendy i zdarzenia API, zostawić streszczenie i nagrane odpowiedzi do testów.

## Przed startem (od użytkownika przez koordynatora)
Adres serwera i wersja MA. **Hasła ani tokenu nie wpisujemy w czacie.** Komendy wymagające logowania nagrywa skrypt uruchamiany przez użytkownika.

## Przeczytaj
`docs/PROJECT.md` (sekcja „Kanał sterowania” — tabela komend), `http://<serwer>:8095/api-docs` (zapisz do scratchpada, czytaj `grep`-iem, nie w całości).

## Zadania
1. Potwierdź każdą komendę z tabeli w `PROJECT.md` (nazwa, parametry, stronicowanie) i dopisz brakujące: ulubione, ostatnio odtwarzane, rekomendacje, URL okładek (imageproxy).
2. Uwierzytelnianie: logowanie → token długoterminowy, wygasanie, nagłówki. Wykrywanie serwera w sieci (mDNS?).
3. Zdarzenia: nazwy i kształt `player_updated`, `queue_updated`, `queue_items_updated`.
4. `scripts/record-fixtures.sh`: użytkownik uruchamia go z tokenem w zmiennej środowiskowej; zapisuje zanonimizowane odpowiedzi do `core/ma-api/src/test/resources/fixtures/`.
5. `docs/MA-API.md` (≤ 250 linii) z numerem wersji serwera.

## Gotowe, gdy
Wszystkie komendy potwierdzone, fixtures nagrane, PR otwarty.

## Nie rób
Kodu klienta (to S06).

## Wynik
- `docs/MA-API.md` (MA 2.10.8, ~97 linii): transport WS, auth (login → token długoterminowy 1 rok → `auth` na WS), komendy z tabeli PROJECT.md potwierdzone w `/api-docs/commands.json` (nazwy, parametry, stronicowanie limit/offset), plus ulubione, recently_played_items, recommendations, browse, okładki (imageproxy).
- `scripts/record-fixtures.sh` + `record-fixtures.mjs`: uruchamia użytkownik z `MA_TOKEN`, zapisuje zanonimizowane odpowiedzi i ~25 s zdarzeń do `core/ma-api/src/test/resources/fixtures/`.
- **Niezrobione:** fixtures NIE są nagrane (wymagają tokenu użytkownika), nie było logowania. Pozycje ⚠️ w MA-API.md (kształt zdarzeń `player_updated`/`queue_updated`/`queue_items_updated`, mDNS `_mass._tcp`, URL imageproxy, `order_by`, `partial`) wynikają z wiedzy o kodzie MA i czekają na nagranie.
- Pułapki: `base_url` w server_info ma spacje na początku; kody błędów 20 (brak auth) i 23 (zły token); `library_items` bywa `ItemMapping`.
- Dalej: użytkownik odpala skrypt, commit fixtures, S06 poprawia ⚠️ w docs (SFix).
