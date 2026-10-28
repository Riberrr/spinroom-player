# S14b — Ulubione i menu kontekstowe
**Model:** Sonnet · **Zależy od:** S09–S13 scalone · **Gałąź:** `s14b-favorites`
**Uwaga:** dotyka wielu modułów `:feature:*` — uruchamiać **bez innych sesji ekranowych** równolegle.

## Przeczytaj
README `:core:design`, „Wzorzec ekranu” w `feature/nowplaying/README.md`.

## Zadania
1. Komponent menu kontekstowego w `:core:design` (przytrzymanie OK): Odtwórz następny, Do kolejki, Ulubione.
2. Podłącz go w kafelkach wszystkich ekranów; komendy ulubionych w nowym pliku `FavoritesApi.kt`.
3. Wiersz „Ulubione” na ekranie Start.

## Gotowe, gdy
Przytrzymanie OK działa wszędzie tak samo; ulubione synchronizują się z MA.

## Wynik
- Menu kontekstowe (przytrzymanie OK): `ContextMenu` + `ItemMenuHost`/`LocalItemMenu` w `:core:design`; `FocusSurface/CoverTile/StationTile` mają `onLongClick`. Logika w `ItemMenuViewModel` (`:core:data`): Odtwórz następny (`play_media option=next`), Do kolejki (`add`), Ulubione (dodaj/usuń).
- Podpięte w: Start (kafelki + stacje), Biblioteka, Szczegóły (albumy wykonawcy + utwory), Szukaj (wyniki + najlepszy), Źródła (elementy grywalne; serduszko zostaje).
- `FavoritesApi.kt` w `:core:ma-api` (`add/remove/resolve/set`, `parseMediaUri`); nowy wiersz „Ulubione” na Start (ulubione albumy/wykonawcy/playlisty). Testy: parseMediaUri, wiersz Ulubione. `check.sh` i `assembleRelease` zielone.
- NIE sprawdzone na TV/serwerze: długie przytrzymanie OK i `item_by_uri` dla pozycji spoza biblioteki. Pułapka: usunięcie z ulubionych elementu spoza biblioteki wymaga id z biblioteki (best effort przez `item_by_uri`); brak toastów/odświeżenia wiersza po zmianie; `:core:design` zależy teraz od `:core:data`.
