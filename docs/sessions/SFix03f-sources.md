# SFix03f — Źródła: fokus po wejściu do folderu, „..”, liczba pozycji
**Model:** Sonnet · **Gałąź:** `fix-sources` · **Moduł:** `:feature:sources` (tylko). Zależy od SFix03b.

## Błędy (emulator, wydanie 1.0.1, MA 2.10)
1. **Fokus po wejściu do folderu** (np. Music Assistant → Radio, RadioBrowser → By category) ucieka do menu bocznego na „Start”. Element z fokusem znika, gdy lista się podmienia. Po wejściu fokus ma trafić na pierwszą pozycję nowej listy, a po Wstecz na folder, z którego wyszliśmy.
2. Na każdym poziomie, także w głównym folderze dostawcy, jest pozycja „..” (kafelek z „.”), liczona do licznika. Usuń ją z listy, bo Wstecz już idzie folder wyżej (podpowiedź na dole to mówi).
3. Odmiana: „1 pozycji”, „4 pozycji” → `plurals`: 1 pozycja, 2–4 pozycje, 5+ pozycji (`sources_count` w `strings.xml`).
4. Okruszki ucinają się („RadioBrowser › By cat…”) i dochodzą do licznika. Skracaj początek ścieżki („… › By category”) albo przenieś licznik pod okruszki.
5. Do zbadania: Music Assistant → Radio pokazuje tylko „..”, chociaż biblioteka ma 5 ulubionych stacji (Start je pokazuje). Sprawdź surową odpowiedź `music/browse` dla tej ścieżki (log z debug). Jeśli MA zwraca stacje, a my je gubimy (paginacja lub filtr typów), napraw. Jeśli MA zwraca pusto, zapisz to w „Wynik”.
6. Bez połączenia: zamiast wiecznego „Wczytuję…” ten sam stan co Start (SFix03d).

## Zadania
Punkty 1–6. Testy JVM `SourcesViewModel` dla filtra „..”, odmiany (funkcja czysta) i stanu offline. Punkty 1 i 5: jeden przegląd na emulatorze (poproś koordynatora; **nie odtwarzaj** pozycji z emulatora).

## Gotowe, gdy
Testy zielone, przegląd potwierdza fokus po wejściu i wyjściu z folderu, PR otwarty.

## Wynik
- Fokus: po wejściu do folderu lista dostaje fokus na pierwszej pozycji, po Wstecz/okruszku na folderze, z którego wyszliśmy (`focusItems`/`focusKey` w stanie, `Crumb.rowKey`). Przegląd na emulatorze (release): RadioBrowser › By category › By language i z powrotem — OK. Pusty folder: fokus zostaje na dostawcach.
- „..” usunięte z listy folderów i dostawców (`name == ".."`); licznik przez `plurals` pod okruszkami; okruszki pokazują „… › rodzic › bieżący”.
- Offline (`SourcesUiState.offline`: Disconnected/Failed): „Brak połączenia z serwerem” + ponów zamiast „Wczytuję…”. Zweryfikowane na emulatorze przed logowaniem.
- Punkt 5 (Radio): surowe `music/browse` dla `builtin://radios` zwraca tylko „..” (raw=1, decoded=1) — MA nie zwraca stacji w drzewie, nie gubimy ich po stronie klienta (Start bierze ulubione z biblioteki). Uwaga: `decodeList()` po cichu pomija pozycje, których nie da się zdekodować; tu nie zachodzi.
- Odmiana: tylko `plurals` (Android zna polskie reguły), bez osobnej funkcji czystej, więc bez testu JVM odmiany. Testy VM: filtr „..”, fokus, offline.
- Pułapki: `R` z dostawcy ląduje na pozycji najbliższej wysokości (nie zawsze pierwszej); debug ANR-uje na emulatorze, testy na release.
