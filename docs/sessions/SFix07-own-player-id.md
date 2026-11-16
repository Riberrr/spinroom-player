# SFix07 — Ustawienia: ustal prawdziwe id własnego odtwarzacza w MA
**Model:** Sonnet · **Gałąź:** `fix-own-player-id` · **Moduły:** `:feature:settings` (`PlayerRows.kt`, `SettingsViewModel`), `:core:ma-api` (modele graczy), `:core:audio`/`:app` (tożsamość Sendspin). Źródło: próba na TV po SFix06 (2026-11-16).

## Błąd (TV 10.0.0.6, MA 2.10.12, debug z `main` po SFix06)
Lista odtwarzaczy w Ustawieniach: dwa wpisy „Telewizor · 2020/2021 UHD Android TV” (`#735a7e`, `#461d4c`; drugi „wybrany”), żaden bez etykiety „To urządzenie”, własny nie jest pierwszy. `isOwnPlayerId` (równość/zawieranie `identity.id`) nie dopasowuje. `settings.playerId` jest ustawiane w `SpinroomApp.onSendspinConnected(clientId)` = `credentials.identity.id`, więc „wybrany” wpis może, ale nie musi być własny. Hipoteza: dwa wpisy to ten sam TV (stary wpis z usuniętego wydania 1.0.0 i debug), a MA nazywa gracza Sendspin inaczej niż `client_id`.

## Zadania
1. Dodaj (tylko w debug, `BuildConfig.DEBUG`) log `Spinroom` przy ładowaniu listy graczy: `ownId` (sam fragment: pierwsze 6 i ostatnie 6 znaków, NIE cały klucz) oraz dla każdego gracza `player_id`, `name`, `available`, `provider`/typ, jeśli model je ma. Nie loguj kluczy ani tokenów. Nie czytaj pliku `sendspin_credentials` przez `run-as` (klucze).
2. Zainstaluj debug na TV, zbierz logi (`adb logcat -d -s Spinroom:*`), ustal zależność `player_id` ↔ `client_id`/`ownId` (MA 2.10: sprawdź też pola gracza typu `provider`, `device_info`, `connection`).
3. Popraw dopasowanie tak, by własny odtwarzacz dostał „To urządzenie” i był pierwszy; test JVM z realnym wzorcem id z logów. Jeśli drugi wpis „Telewizor” to osierocony stary gracz, zostaw go (nie usuwaj z serwera), ale niech `shortId`/„nieaktywny” go odróżnia.
4. Usuń lub zostaw log tylko jeśli nie zawiera wrażliwych danych (preferuj usunięcie przed PR).

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Build bez `local.properties`: `ANDROID_HOME=C:/Users/lukas/AppData/Local/Android/Sdk bash ./gradlew :app:assembleDebug -q`.
**Prawdziwy TV 10.0.0.6:5555** — użytkownik zezwolił koordynatorowi na próby przy włączonym ekranie na tę sesję i jest zalogowany na debug (`io.github.riberrr.spinroom.debug`). `adb install -r` TYLKO (nie odinstalowuj — traci login), NIE wpisuj danych logowania, nie uruchamiaj odtwarzania. Gdy skończysz próby na TV, uśpij ekran (`input keyevent 223`) i napisz do koordynatora. Jeśli TV nie odpowiada na adb, poproś koordynatora (użytkownik musi włączyć debugowanie sieciowe). Emulator emulator-5554 jest tu zbędny.

## Gotowe, gdy
Na TV lista pokazuje „To urządzenie” na własnym odtwarzaczu (zweryfikowane uiautomator dump), `bash scripts/check.sh` czyste (lint może paść na `local.properties` — środowisko), PR otwarty, nie scalony. W „Wynik”: jakie jest realne id w MA i co sprawdzone na TV.

## Wynik
Realne id w MA 2.10.12: własny TV to `up6b461d4c` (provider `universal_player`, device_info bez ip/mac/identifiers), NIE klucz Sendspin ani jego skrót (MD5/SHA-1/256/512 sprawdzone na TV). Drugi „Telewizor” `up31735a7e` to wbudowany Chromecast TV (cast_uuid, ip_address, mac).
- `resolveOwnPlayerId` (PlayerRows.kt): najpierw dopasowanie po id (equals/contains), potem heurystyka: bez cast_uuid/ip_address, model == `Build.MODEL`, provider != sendspin; remis → wybrany, dostępny, najniższe id. `buildPlayerRows(..., ownModel)`.
- Model: `PlayerDeviceInfo.address` = `ip_address` (wcześniej zły klucz), `identifiers: JsonElement?`, `isChromecast`. Chromecast ma drugą linię „Chromecast · IP”, bez `#id`.
- Testy JVM na zanonimizowanych JSON-ach obu wpisów (PlayerRowsTest); `scripts/check.sh` zielone. Logi debug (TEMP-SFIX07) usunięte.
- Na TV: log `players:`/`rawplayer:` zebrał koordynator; weryfikacja końcowa listy (uiautomator) po instalacji tego PR — do zrobienia przez koordynatora.
- Pułapka: heurystyka opiera się na modelu zgłaszanym przez Sendspin (`Build.MODEL`); inny TV bez zgodnego modelu nie dostanie „To urządzenie”.
