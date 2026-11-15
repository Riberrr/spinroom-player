# SFix02 — Logowanie na TV nie działa + wygoda ekranu Ustawienia (v1.0.0)
**Model:** Opus · **Gałąź:** `fix-settings-login`

## Błąd
Na TV (v1.0.0) logowanie do MA zawsze kończyło się komunikatem „Nie widzę serwera Music Assistant w sieci”, mimo poprawnego adresu i hasła. Uwagi użytkownika do ekranu Ustawienia:
1. Wyszukiwanie serwerów MA w sieci lokalnej, nie tylko ręczny adres.
2. Pole adresu nie mówi, w jakim formacie wpisać adres (z `http://`? z portem?).
3. Placeholder pola pokazywał prywatny adres serwera autora.
4. Przy przechodzeniu pilotem między polami za każdym razem wyskakuje klawiatura; ma się włączać dopiero po OK (klawiatura sprzętowa: pisanie od razu).

## Zadania
1. Odtwórz i napraw logowanie (konto testowe na serwerze MA, emulator).
2. Punkty 1–4 w `:feature:settings` (+ odkrywanie w `:core:ma-api`).

## Gotowe, gdy
Logowanie działa na emulatorze z prawdziwym serwerem MA, testy zielone, PR otwarty.

## Wynik
- Logowanie: dwie przyczyny. (a) Android blokował nieszyfrowane `ws://` do serwera w LAN („CLEARTEXT … not permitted”) → `app/src/main/res/xml/network_security_config.xml` (cleartext dozwolony, adres wpisuje użytkownik). (b) MA 2.10 zwraca z `auth/login` pole `access_token`, a nie `token` → `AuthLoginResult` czyta oba. Sprawdzone na emulatorze z MA 2.10.11: „Połączono…”, „Zalogowano”.
- Błędy logowania trafiają do logcat (`login: failed: <klasa>: <komunikat>`, bez hasła/tokenu). Timeout z `withTimeout` (`TimeoutCancellationException`) nie jest już połykany jako anulowanie.
- Odkrywanie: `MaServerDiscovery` (`:core:ma-api/discovery`, `NsdManager`, `_mass._tcp`, adres z TXT `base_url`, zapasowo host:port), lista „Serwery w sieci” w Ustawieniach; wybór wpisuje adres. NIE sprawdzone na prawdziwym TV (emulator za NAT nie widzi mDNS z LAN).
- Pole adresu: etykieta „Adres serwera”, podpowiedź „np. 192.168.1.10 lub 192.168.1.10:8095” (przyjmuje też `http://…`).
- `SettingsTextField`: fokus pilotem tylko podświetla; OK = edycja + klawiatura ekranowa; Back/Gotowe/↑/↓ kończą edycję (Back w trakcie edycji nie zamyka ekranu); znak z klawiatury sprzętowej zaczyna edycję bez OK. Na emulatorze klawiatura ekranowa i tak się pokazuje przy pisaniu sprzętowym (emulator nie zgłasza fizycznej klawiatury) — sprawdzić na TV z klawiaturą USB/BT.
- Druga runda (emulator): ↑/↓ na brzegu ekranu wskakiwało do menu bocznego → `SpinroomShell` blokuje wyjście ↑/↓ z treści (`focusProperties.onExit` + `focusGroup`), menu tylko strzałką ←. Komunikat „Najpierw wybierz odtwarzacz w ustawieniach” odsyłał do nieistniejącej opcji → sekcja „Odtwarzacz” w Ustawieniach (lista graczy MA, wybór zapisuje `playerId`). Odtwarzania z emulatora nie uruchamiano (zagrałoby na TV).
- mDNS: MA ogłasza `_mass._tcp` z TXT `base_url` (sprawdzone z PC 10.0.0.38 w tej samej sieci co TV); emulator (10.0.2.16, NAT) go nie widzi — to nie błąd aplikacji. Na emulatorze odtwarzacz TV też się nie rejestruje (MA nie dosięga emulatora), więc trzeba wybrać innego gracza.
- Pułapki: `uiautomator`/`input` w Git Bash wymagają `MSYS_NO_PATHCONV=1`; `screencap` na emulatorze TV daje czarny obraz.
- Dalej: przenieść `SettingsTextField` do `:core:design`, gdy drugi ekran będzie miał pola tekstowe; `10.0.1.50` zostało w testach i docs (nie trafia do APK).
