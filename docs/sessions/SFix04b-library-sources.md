# SFix04b — Biblioteka i Źródła: fokus po powrocie, puste zakładki
**Model:** Sonnet · **Gałąź:** `fix-library-sources-focus` · **Moduły:** `:feature:library`, `:feature:sources`. Źródło: raport SFix03v (błędy 4 i 5).

## Błędy (emulator, release)
1. Biblioteka: po powrocie (Wstecz z Teraz odtwarzane) fokus wraca na „Albumy · 0”, nie na ostatnio aktywny element. Przy pustej zakładce ↓ przeskakuje do mini-odtwarzacza, a → z ostatniego chipa też. Do sprawdzenia: „Albumy · 0” przy bibliotece z utworami — czy licznik naprawdę jest 0 (SFix03e ładuje liczniki po połączeniu), czy ładuje się za wcześnie/za późno.
2. Źródła: po powrocie do listy folderów → trafia w pierwszą pozycję, nie w ostatnio aktywną (np. „Playlists”). Zapamiętaj klucz ostatnio sfokusowanego elementu na poziom.
3. Źródła: ← z listy folderów idzie najpierw do listy wtyczek, dopiero drugie ← do menu. Oceń, czy to zamierzone (poziom wyżej); jeśli nie, napraw tylko jeśli proste — inaczej opisz w „Wynik”.

## Zadania
Punkty 1–3, lokalnie, w stylu SFix03e/SFix03f (przeczytaj ich „Wynik”). Test JVM dla logiki stanu. Nie ruszaj Start ani Szukaj.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554 ma jedna sesja naraz — poproś koordynatora o przydział. Testuj wariant release (`io.github.riberrr.spinroom`), instaluj tylko `-r` (odinstalowanie wylogowuje użytkownika). Użytkownik jest zalogowany na koncie Tv; nie wpisuj danych logowania. Nie uruchamiaj odtwarzania. Klawisze pojedynczo (aplikacja wolna). Po restarcie emulatora Wi-Fi: `cmd wifi connect-network AndroidWifi open`.

## Gotowe, gdy
`bash scripts/check.sh` czyste (lint może paść na `local.properties` — to środowisko), PR otwarty, nie scalony.

## Wynik
- Biblioteka: ostatnio sfokusowany element (chip albo kafelek o indeksie) trzyma `LibraryViewModel` (`LibraryFocus`); po powrocie ekran przewija do kafelka i fokusuje go (czeka, aż siatka się załaduje). Zmiana zakładki resetuje fokus na chipy.
- Pusta zakładka / brak siatki: chipy mają `down = Cancel`, a ostatni chip także `right = Cancel`, więc fokus nie ucieka do mini-odtwarzacza.
- „Albumy · 0”: nie udało się odtworzyć bez emulatora. Licznik na chipie bierze teraz rozmiar strony z Paging (`libraryCountLabel`), gdy siatka ma pozycje, więc przypadkowe 0 z `count()` nie wygra. Przyczyny samego 0 nie ustaliłem.
- Źródła: `lastFocusKey` w stanie (ustawiane z `onFocusChanged` wiersza, zerowane przy zmianie folderu/dostawcy); po powrocie do ekranu lista przywraca fokus na ten wiersz, a kolumna dostawców nie przejmuje fokusu.
- ← z listy folderów do wtyczek to zwykła nawigacja przestrzenna (kolumna dostawców jest po lewej), drugie ← idzie do menu — uznane za zamierzone, bez zmian.
- Testy JVM: fokus Biblioteki, `libraryCountLabel`, `lastFocusKey` w Źródłach. Emulatora nie użyłem (brak przydziału); do sprawdzenia ręcznie: Wstecz z Teraz odtwarzane w Bibliotece i Źródłach.
