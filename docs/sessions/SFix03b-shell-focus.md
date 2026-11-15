# SFix03b — Fokus menu bocznego i powrót do treści
**Model:** Sonnet · **Gałąź:** `fix-shell-focus` · **Pliki:** `app/.../SpinroomShell.kt`, `core/design/.../SideMenu.kt` (tylko te)

## Błędy (emulator, wydanie 1.0.1)
1. ← z treści otwiera menu na pozycji na tej samej wysokości, a nie na bieżącym ekranie. Przykłady: z kafelka na Start fokus trafia na „Źródła” albo „Szukaj”, z klawiatury Szukaj na „Start”.
   Przyczyna: `focusManager.moveFocus(Left)` w `SpinroomShell` znajduje pozycję menu geometrycznie, a `menuFocus` wskazuje zawsze pierwszą pozycję.
2. → z menu do treści trafia na element na tej samej wysokości, a nie na ostatnio aktywny w treści. Przykłady: Ustawienia → „Zaloguj” zamiast pola adresu, Źródła → „Teraz odtwarzane” albo „By category”.
3. Gdy element z fokusem znika z treści (np. przycisk „Zaloguj” po zalogowaniu, lista po wejściu do folderu w Źródłach), fokus ucieka do menu na „Start”, a menu się rozwija.
4. Rozwinięte menu ściska treść zamiast ją przykrywać: chip „Wykonawcy” łamie się na „Wykon/awcy”, tytuł mini-odtwarzacza skraca się do „Ra…”.

## Zadania
1. Menu: `focusGroup` + `focusProperties { onEnter = … }`, żeby wejście do menu zawsze trafiało na pozycję `selectedId`.
2. Treść: `focusRestorer()` (albo zapamiętany `FocusRequester`), żeby → z menu wracało do ostatniego elementu treści. Gdy go nie ma, fokus trafia na pierwszy element ekranu.
3. Utrata fokusu w treści nie może przenosić fokusu do menu. Fokus zostaje w treści, a ekrany (SFix03d/f/g) ustawiają go same.
4. Rozwinięte menu jako nakładka (`Box` + `zIndex`/offset) nad treścią o stałej szerokości.
5. Test UI (Compose `createComposeRule`, jeśli w `:app` są testy instrumentacyjne) albo test logiki wyboru pozycji. Jeśli testy nie są możliwe, sprawdź na emulatorze (zapytaj koordynatora o emulator).

## Gotowe, gdy
Na każdym z 5 ekranów ← trafia na bieżącą pozycję menu, a → wraca do poprzedniego elementu treści. Treść nie zmienia układu przy rozwinięciu menu. PR otwarty.

## Wynik
_(uzupełnia sesja; opisz API dla ekranów, np. jak zgłosić „domyślny fokus ekranu”)_
