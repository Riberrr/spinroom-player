# SFix03d — Start: odświeżanie, fokus, stany, stacje
**Model:** Sonnet · **Gałąź:** `fix-home` · **Moduł:** `:feature:home` (tylko). Zależy od SFix03b (przeczytaj jego „Wynik”).

## Błędy (emulator, wydanie 1.0.1)
1. **Brak odświeżania.** Wiersz „Ulubione” nie zmienia się po dodaniu ani usunięciu ulubionej (z menu kontekstowego w Bibliotece lub na Start). Usunięty kafelek zostaje, nowy pojawia się dopiero po restarcie.
   `HomeViewModel` ładuje dane tylko przy `Connected` i po „Spróbuj ponownie”.
2. **Fokus po Wstecz.** Po powrocie ze szczegółów fokus trafia na kartę „Kontynuuj słuchanie”, a nie na otwarty kafelek. `Rows()` w `HomeScreen.kt` woła `first.requestFocus()` przy każdym wejściu.
3. **Pusty ekran bez połączenia.** Brak serwera albo brak połączenia daje całkowicie pusty ekran (`HomeScreen.kt:85`, gałąź `loading && !hasContent -> Unit`, a `loading` nie spada przy `Disconnected`).
   Ma być: brak skonfigurowanego serwera → `EmptyState` „Połącz się z serwerem” + przycisk do Ustawień. Łączenie → wskaźnik/tekst „Łączę z serwerem…”. Błąd → `ErrorState`.
4. Karta „Kontynuuj słuchanie” dla radia pokazuje „· utwór 7 z 9”. Przy pustym wykonawcy nie dodawaj „·”.
5. Ulubione stacje: kafelki pokazują inicjały zamiast logo (logo jest w MA, bo karta „Kontynuuj” je pokazuje) i podpis „library” zamiast nazwy dostawcy. Pokaż `image` stacji. Podpis: nazwa dostawcy z `provider_mappings` albo brak podpisu, nigdy „library”.

## Zadania
1. Odświeżanie: przeładuj dane przy `ON_RESUME` ekranu albo nasłuchuj zdarzeń MA o zmianach biblioteki (sprawdź w docs/MA-API.md, czy jest `media_item_updated`). Wybierz tańsze rozwiązanie i opisz je w „Wynik”.
2. Fokus: zapamiętaj klucz ostatnio aktywnego kafelka (`rememberSaveable`) i przywróć go po powrocie. Pierwszy fokus tylko przy pierwszym wejściu.
3. Stany 3–5 + testy JVM `HomeViewModel` (fake `HomeBackend`): offline, odświeżanie, mapowanie stacji.
4. Ostrzeżenie: README mówi, że ulubione nie są sprawdzone na żywym serwerze. Po zmianie zrób jeden przegląd na emulatorze (poproś koordynatora o emulator, hasło konta `Tv` poda użytkownik).

## Gotowe, gdy
Testy zielone, przegląd na emulatorze potwierdza punkty 1–5, README modułu zaktualizowane, PR otwarty.

## Wynik
_(uzupełnia sesja)_
