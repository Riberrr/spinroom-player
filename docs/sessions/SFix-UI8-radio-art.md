# SFix — Poprawka z testów na TV (szablon)
**Model:** Sonnet (Opus dla dźwięku/synchronizacji) · **Gałąź:** `fix-<temat>`

Koordynator kopiuje ten plik jako `SFix-<temat>.md` i wpisuje opis błędu od użytkownika.

## Błąd
Radio (stacje z Radio Browser): „Teraz odtwarzane” bez okładki (v1.1.0). Gałąź `fix-ui8-radio-art`.

## Zadania
1. Odtwórz błąd: test jednostkowy albo `adb logcat -d -s Spinroom:* AndroidRuntime:E | tail -80`.
2. Minimalna poprawka w module, którego dotyczy; test, który by ten błąd złapał.
3. Nie poprawiaj „przy okazji” innych rzeczy — zapisz je w „Wynik”.

## Gotowe, gdy
Test zielony, PR otwarty; koordynator wydaje wersję patch (`v0.x.y+1`).

## Wynik
- Diagnoza (MA 10.0.1.50 + TV): dane o obrazie przychodzą — kolejka ma `media_item.metadata.images[thumb]` z `proxy_id`; `/imageproxy/<id>` działa dla większości stacji. Gubi się w dwóch miejscach: (1) proxy zwraca 404 (np. „Radio RMF” — `path` to strona WWW, nie obraz) albo `image/svg+xml` (Złote Przeboje — Coil bez modułu SVG nie dekoduje); ekran pokazywał wtedy ogólny placeholder; (2) okładka utworu z kolejki (`queue_item.image`, MA wypełnia z tytułu strumienia) była ignorowana na rzecz logo stacji.
- Poprawka (`:feature:nowplaying`): `pickCoverImage` — dla radia najpierw obraz elementu kolejki (okładka utworu), potem logo stacji; nigdy logo MA. Gdy obraz się nie załaduje, radio pokazuje inicjały na kolorze stacji (`StationCover`), nie szary placeholder. Test: `radioCoverPrefersSongArtThenStationLogoNeverMaLogo`.
- Nie sprawdzone na TV po poprawce (odtwarzanie testowej stacji zablokowane przez klasyfikator uprawnień); pole `artwork` Sendspin (bajty) nadal nieużywane przez UI.
- Następne: SVG loga stacji wymaga `coil-svg` (libs.versions.toml — koordynator); dock/tło w `:app`/`:core:design` nadal bez zapasu inicjałów (zakres SFix-UI7).
