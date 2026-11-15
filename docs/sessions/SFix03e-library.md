# SFix03e — Biblioteka: stany i pasek liter
**Model:** Haiku · **Gałąź:** `fix-library` · **Moduł:** `:feature:library` (tylko `LibraryScreen.kt`, `LibraryViewModel.kt`, strings). Zależy od SFix03b.

## Błędy (emulator)
1. Bez połączenia z serwerem ekran jest pusty (bez komunikatu), a pasek liter (#, A, D…) i tak się wyświetla i można na niego wejść fokusem.
2. Pasek liter jest widoczny także na pustej karcie („Nic tu jeszcze nie ma”, np. Albumy · 0).
3. Albumy, Wykonawcy i Utwory pokazały pustą listę bez licznika, a po OK „Albumy · 0”. Licznik ma być widoczny od razu po załadowaniu karty, nie dopiero po wybraniu.

## Zadania
1. Bez połączenia: ten sam wzorzec co Start (SFix03d): `EmptyState` „Połącz się z serwerem” albo „Łączę z serwerem…”. Teksty skopiuj dosłownie z `feature/home/.../strings.xml` po scaleniu SFix03d, a jeśli jeszcze go nie ma, użyj tych samych słów.
2. Pasek liter tylko przy `itemCount > 0`.
3. Test JVM `LibraryViewModel` dla stanu offline i pustej karty.
4. Bez emulatora: wystarczą `@Preview` + testy.

## Gotowe, gdy
Testy zielone, `bash scripts/check.sh` czyste, PR otwarty.

## Wynik
- Bez połączenia / niezalogowany: `EmptyState` „Połącz się z serwerem” + „Wpisz adres serwera w Ustawieniach.”; łączenie przy pustej siatce: „Łączę z serwerem…”; „Wczytuję…” przy ładowaniu; `Failed` → `ErrorState`. Teksty są w `library_no_server_*` / `library_connecting` / `library_loading` — po scaleniu SFix03d zrównać z jego słowami.
- Pasek liter tylko gdy `itemCount > 0`. Liczniki (`counts`) ładują się przy każdym przejściu w stan gotowy (połączony + zalogowany) dla wszystkich zakładek — stąd licznik widać od razu, nie po OK.
- Public API: `libraryStatus(connection, refresh, itemCount)` (internal, pure); `LibraryUiState.connection`; `LibraryBackend.connection: Flow<MaConnectionState>` (nowe pole, zaimplementowane w `MaLibraryBackend` przez `ConnectionManager.state`). Pager tylko przy gotowości.
- Testy: `LibraryViewModelTest` 10/10 zielone (offline, bez zalogowania, pusta karta, tabela `libraryStatus`). `scripts/check.sh` kończy się błędem `:app:lintDebug` (PropertyEscape na `local.properties` tej worktree — plik lokalny, poza repo); `assembleDebug assembleRelease testDebugUnitTest` przechodzą.
- Emulator nie był użyty (sesja pozwalała na testy JVM + Preview). Do sprawdzenia ręcznie: Biblioteka bez logowania, po zalogowaniu liczniki bez OK.
- Pułapki: nazwa testu JVM nie może zawierać `:`; `pager()` zawiesza się/wyrzuca wyjątek bez połączenia, więc items są gated; `Disconnected` pojawia się też przed pierwszym połączeniem (krótki „Połącz się” zanim „Łączę”).
