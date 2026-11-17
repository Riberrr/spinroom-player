# SFix-UI3 — Biblioteka i Szczegóły
**Gałąź:** `claude/fix-ui3-library-details-01765e`

## Błąd
1. Biblioteka → Albumy „0 / Nic tu jeszcze nie ma”, Szczegóły „albumu” bez utworów.
2. Szczegóły: Lewo z przycisków nie otwiera menu; Back przechodzi przez stos szczegółów, potem aplikacja zostaje „zaparkowana” i wznawia ostatni ekran.
3. Biblioteka/Szczegóły: czarne tło zamiast `ContentBackdrop`.
4. Playlisty systemowe MA mają logo MA jako okładkę.

## Wynik
- **Albumy 0 to prawda serwera** (sprawdzone WS-em: `music/albums/count` = 0, artists 3, tracks 3, playlists 12; konto TV). Kod stronicowania/filtra jest poprawny. Prawdziwa przyczyna „pustego albumu”: Szczegóły traktowały każdy element inny niż artysta/playlista jako album — utwór (np. z „Ostatnio odtwarzane”, `ytmusic://track/…`) szedł do `album_tracks` i serwer odpowiadał błędem 999 „Invalid album browseId”. Nowy `DetailsKind.Track` pokazuje sam utwór (test `single_track_shows_itself_without_album_tracks`).
- Lewo z przycisków: wiersz przycisków był za szeroki — serce (Ulubione) wypadało poza ekran i przechwytywało fokus. Zmniejszone odstępy/paddingi; teraz Lewo otwiera menu.
- Back: na Starcie `BackHandler` kończy aktywność (`finish()`), więc Android 12+ nie parkuje zadania i start aplikacji zaczyna od Startu. Otwarcie elementu z „Przypiętych” będąc na Szczegółach zastępuje bieżące Szczegóły (Back wraca skąd przyszliśmy). Artysta → album nadal układa stos (to naturalne). Zmiany w `:app` (`SpinroomShell.kt`) — poza zakresem z promptu, ale konieczne.
- Tło: Szczegóły używają `ContentBackdrop` (okładka), przyciski/wiersze na szkle; Biblioteka zawsze ma tło (okładka pierwszego elementu, gdy fokus na zakładkach; reset przy zmianie zakładki).
- Okładka-logo MA: `MediaItem.hasOnlyMaLogo`/`coverImage` (nowy plik w `:core:ma-api`, provider `builtin`/`logo.png`) → brak okładki → `PlaylistMosaic` (własny kolor+ikona). Tylko Biblioteka i Szczegóły; Home/Playlisty (inne sesje) mogą użyć `coverImage`.
- Pułapka: na emulatorze są dwie aplikacje — `…spinroom` (stare wydanie) i `…spinroom.debug` (świeży build); testować `.debug`.
- Do zrobienia: gdy playlista bez okładki jest zaznaczona, tło Biblioteki jest czarne (brak koloru zastępczego).
