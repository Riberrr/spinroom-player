# SFix-UI1 — Start i Ustawienia (usterki z przeglądu SUIv)
**Gałąź:** `claude/fix-ui1-home-settings-493377` · Zakres: `:feature:home`, `:feature:settings`

## Błąd
1. Start: brak „Wszystkie stacje →” / „Wszystkie playlisty →” widocznych w rzędach (kafel na końcu długiego rzędu), brak logo w szynie menu, chip statusu bez nazwy odtwarzacza, kafle stacji za duże (5 zamiast 8 w rzędzie). Powitanie bez imienia — to nie błąd (MA nie daje imienia).
2. Start: puste ulubione playlisty → kafel „Dodaj playlistę do ulubionych” zamiast siatki — ocenić.
3. Ustawienia: D-pad Lewo z listy kategorii nie przenosi do szyny menu.
4. Ustawienia → Połączenie: Enter w polu tekstowym przeskakuje na „Nazwa odtwarzacza” i otwiera okno zamiast do następnego pola.

## Zadania
Wg `SFix-template.md`.

## Wynik
- Start: „Wszystkie stacje →” / „Wszystkie playlisty →” to teraz linki po prawej w nagłówku sekcji (jak makieta 03B), zawsze widoczne; kafel na końcu długiego rzędu usunięty. Kafle stacji 84 dp (8 w rzędzie na 960 dp TV), playlisty 100 dp.
- Chip statusu: „<nazwa odtwarzacza> · połączono” (`PlayerNameStore`, `HomeBackend.playerName`); bez ustawionej nazwy zostaje „Połączono” (brak domyślnej nazwy w danych).
- Puste ulubione playlisty: kafel-podpowiedź zostaje (zgodne z zamysłem „puste ulubione”), a link „Wszystkie playlisty →” jest widoczny obok.
- Ustawienia, Enter w polu tekstowym: `ImeAction.Next` + `onNext` przenosi do następnego elementu (sprawdzone na emulatorze: Adres → następny przycisk, okno nazwy się nie otwiera).
- Lewo z listy kategorii: na emulatorze działa już na tym kodzie (otwiera szynę) — nie udało się odtworzyć; jeśli na TV nadal zawodzi, to inny stan fokusu (do sprawdzenia na TV).
- Poza zakresem: logo w szynie menu (`:core:design` SideMenu / `:app`) — do osobnej poprawki. Test emulatora: apk debug ma pakiet `.debug`.
