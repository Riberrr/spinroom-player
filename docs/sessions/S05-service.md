# S05 — Usługa odtwarzania i pilot → v0.1.0
**Model:** Opus · **Zależy od:** S03, S04 · **Gałąź:** `s05-service`

## Cel
TV gra muzykę puszczoną z telefonu, także przy wygaszonym ekranie, a przyciski pilota działają.

## Przeczytaj
„Wynik” w `S03-network.md` i `S04-audio.md`, README `:core:sendspin` i `:core:audio`.

## Zadania
1. Media3 `MediaSessionService` jako usługa pierwszoplanowa (`mediaPlayback`, uprawnienia Androida 14), powiadomienie.
2. Własny `Player` Media3 łączący komendy (play/pauza/następny/poprzedni) z kontrolerem Sendspin i stanem `AudioEngine`; metadane i okładka do sesji.
3. Cykl życia: start serwera Sendspin i ogłoszenia mDNS razem z usługą, ponowne łączenie, Wi-Fi lock.
4. Instrukcja dla użytkownika w „Wynik”: `keytool` do wygenerowania klucza i nazwy Secrets (`SIGNING_*`).

## Gotowe, gdy
Test na TV: muzyka z aplikacji MA na telefonie gra z TV, przy wygaszonym ekranie też; Play/Pauza i ⏭ ⏮ z pilota działają. Koordynator taguje **v0.1.0**.

## Nie rób
UI poza powiadomieniem.

## Wynik
**Co działa:** `:core:audio/service` — `SpinroomPlaybackService` (Media3 `MediaSessionService`, pierwszoplanowa `mediaPlayback`, uprawnienia z Androida 14 w manifeście modułu) startowana z `MainActivity`, działa po wyjściu z aplikacji (`onTaskRemoved` jej nie zatrzymuje). `SendspinReceiver` trzyma sesję + `SpinroomAudioEngine` + serwer 8928/mDNS, Wi-Fi lock, wake lock w trakcie grania, restart serwera/mDNS po zmianie sieci. `SendspinMediaPlayer` (`SimpleBasePlayer`): metadane, okładka, pozycja; play/pauza/⏭/⏮/seek → komendy kontrolera Sendspin. Tożsamość i parowania w DataStore (`SendspinCredentialStore` w `:core:data`). `app/src/debug` z S03 usunięty. 14 nowych testów JVM (mapowanie stanu i komend, magazyn kluczy).
**Na TV (10.0.0.6, API 30):** usługa na pierwszym planie, MA 2.10.8 łączy się sam (handshake, 4 role, `group/update` z 14 komendami), tożsamość przetrwała reinstalację, akcje powiadomienia wysyłają `client/command` (Next/Play/Pause) do MA. **Niesprawdzone przez człowieka:** granie z telefonu, wygaszony ekran, klawisze pilota — wymaga zgody na nowy odtwarzacz „Telewizor” w MA (nowa tożsamość, stara z S03 była w SharedPreferences).
**API:** `SpinroomPlaybackService.start(context)`; aplikacja implementuje `PlaybackHost` (`sendspinCredentials()`, `softwareVersion`, `notificationIcon`, `onSendspinConnected`). `SpinroomApp` ustawia `settings.playerId` = client id Sendspin, jeśli pusty (⚠️ zakładam, że MA używa client id jako `player_id` — do potwierdzenia przy S08 na żywo).
**Dalej:** ekran tokenu parowania `SP:0…` i nazwa odtwarzacza (ustawienia), start przy włączeniu TV (`BOOT_COMPLETED`), S14c — `setOutputDelayMs`.
**Pułapki:** Android kieruje klawisze multimediów do aplikacji, która ostatnio grała dźwięk — nasza sesja przejmuje je dopiero, gdy nasz `AudioTrack` zagra. Powiadomienie jest nasze (Media3 schodzi z pierwszego planu, gdy nic nie gra — odbiornik musi być osiągalny). Start/stop odbiornika blokuje — jeden wątek `Spinroom-service`, żeby restart nie walczył o port 8928.

**Klucz podpisu (robi użytkownik, raz):**
1. `keytool -genkeypair -v -keystore spinroom-release.jks -alias spinroom -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Spinroom Player"` — keytool sam zapyta o hasło (nie wpisuj go w linii poleceń). W PKCS12 hasło klucza = hasło magazynu.
2. Base64 (PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("spinroom-release.jks")) | Set-Content -NoNewline keystore.b64`
3. Secrets w repo (każdy `gh secret set` bez `--body` pyta o wartość): `gh secret set SIGNING_KEYSTORE_BASE64 < keystore.b64`, `gh secret set SIGNING_STORE_PASSWORD`, `gh secret set SIGNING_KEY_PASSWORD` (to samo hasło), `gh secret set SIGNING_KEY_ALIAS --body spinroom`. Potem usuń `keystore.b64`.
4. Kopia `.jks` i hasła poza repo (menedżer haseł). Utrata klucza = aktualizacje nie zainstalują się na starą wersję. Lokalnie opcjonalnie `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`; w `.gitignore`).
5. Ustaw Secrets **przed** tagiem `v0.1.0` — bez nich CI podpisze APK jednorazowym kluczem debug.
