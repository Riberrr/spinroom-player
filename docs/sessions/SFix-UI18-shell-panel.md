# SFix-UI18 — panel odtwarzacza i menu
**Model:** Sonnet (Opus dla dźwięku/synchronizacji) · **Gałąź:** `sfix-ui18-shell`

Koordynator kopiuje ten plik jako `SFix-<temat>.md` i wpisuje opis błędu od użytkownika.

## Błąd
1. Rozwinięty panel u góry: Strzałka w dół poza ostatni element musi zwinąć panel i oddać fokus ekranowi (nie może zostać zablokowany).2. Zwinięty pasek większy i wyraźniejszy (76 dp), bo Start traci nagłówek.3. Wybór pozycji w menu bocznym zwija menu i ładuje ekran z fokusem na treści (zawsze widok główny, Wstecz działa).
## Zadania
1. Odtwórz błąd: test jednostkowy albo `adb logcat -d -s Spinroom:* AndroidRuntime:E | tail -80`.
2. Minimalna poprawka w module, którego dotyczy; test, który by ten błąd złapał.
3. Nie poprawiaj „przy okazji” innych rzeczy — zapisz je w „Wynik”.

## Gotowe, gdy
Test zielony, PR otwarty; koordynator wydaje wersję patch (`v0.x.y+1`).

## Wynik
- Pasek `DOCK_BAR_HEIGHT` 76 dp: okładka 56 dp, większy tytuł/artysta, ikony 32/40/32 dp, jaśniejsze tło i ikony; treść ekranów przesuwa się sama (pasek jest w układzie).
- Zwijanie: Dół na lewych kontrolkach panelu (⏮⏯⏭♡) wywołuje `onCollapse` wprost (wcześniej focus search zostawiał fokus na sercu); Dół z listy już działał.
- Menu: po wyborze `SpinroomShell` ponawia `requestFocus` na ekran, aż fokus utrzyma się ~1 s; w tym czasie pasek ma `focusLocked` (ekran ładuje się asynchronicznie i tracił fokus na rzecz paska).
- Test: emulator-5554 (Radio, Biblioteka z menu; serce+Dół); `scripts/check.sh` OK. Logika fokusu bez testów jednostkowych (Compose).
- Uwaga: pierwsza próba (jednorazowe requestFocus) lądowała na pasku — stąd blokada.
