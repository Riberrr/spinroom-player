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
Zrobione i sprawdzone na emulatorze (build release; debug ANR-uje na emulatorze także na HEAD, nie wiem czemu):
- ← z treści otwiera menu na pozycji bieżącego ekranu (Ustawienia → „Ustawienia”), → wraca do ostatnio aktywnego elementu treści (np. „Opóźnienie dźwięku”), nie do elementu na tej samej wysokości.
- Rozwinięte menu przykrywa treść (slot 88 dp, `zIndex`), treść nie zmienia układu.
- Fokus wpadający do menu bez ← (zniknięcie elementu) wraca do treści (`contentFocus`); ekrany same ustawiają swój fokus (SFix03d/f/g).
API: bez zmian dla ekranów. `SideMenu(..., enterFocusRequester)` zastąpiło `firstItemFocusRequester`. „Domyślny fokus ekranu” = pierwszy fokusowalny element treści (fallback `focusRestorer`).
Pułapki: `cancelFocusChange()` sprawia, że `moveFocus()` zwraca true (krawędź ← wykrywana flagą w `onExit`); `onExit` z kierunkiem `Enter` musi przechodzić; `focusRestorer` w osobnej grupie wewnętrznej, bo onExit zewnętrznej go nadpisuje. Nie przetestowano ←/→ na ekranach z danymi (brak logowania MA w tej sesji): Start/Biblioteka/Źródła/Szukaj sprawdzić po SFix03d/f/g. Brak testu automatycznego (w :app nie ma testów UI).

