# S03 — Sieć i wykrywanie (Sendspin)
**Model:** Opus · **Zależy od:** S02 · **Gałąź:** `s03-network`

## Cel
TV pojawia się w Music Assistant jako odtwarzacz i odbiera ramki audio.

## Przed startem (od użytkownika)
IP i model telewizora, włączone debugowanie sieciowe.

## Przeczytaj
`docs/SENDSPIN.md` (transport i mDNS), „Wynik” w `S02-protocol.md`, `core/sendspin/README.md`.

## Zadania
1. W `:core:sendspin` pakiet `transport`: serwer Ktor CIO na porcie 8928, ścieżka `/sendspin`, most do `SendspinSession`.
2. Ogłoszenie `NsdManager` jako `_sendspin._tcp` z rekordami TXT ze specyfikacji; nazwa odtwarzacza = nazwa urządzenia (do zmiany w ustawieniach).
3. Zapasowo: klient łączący się z `ws://<serwer>:8927/sendspin` (adres podawany jako parametr).
4. Tymczasowy przełącznik startu w buildzie debug (cykl życia przejmie S05).

## Gotowe, gdy
Na TV: MA widzi odtwarzacz, handshake w logu (`adb logcat -s Spinroom:*`), licznik odebranych ramek rośnie po puszczeniu muzyki.

## Nie rób
Odtwarzania dźwięku (S04) ani usługi pierwszoplanowej (S05).

## Wynik
**Co działa:** pakiet `transport` w `:core:sendspin`: serwer Ktor CIO `0.0.0.0:8928/sendspin` (403 dla nagłówka `Origin`, drugie połączenie zamykane 1013 `busy`), mDNS `_sendspin._tcp` przez `NsdManager` (TXT `path`, `name`; nazwa = `Settings.Global.DEVICE_NAME`/`Build.MODEL`), zapasowy klient OkHttp do `ws://<host>:8927/sendspin` z backoffem 1→30 s. 8 testów JVM na prawdziwych gniazdach. **Na TV (10.0.0.6, „2020/2021 UHD Android TV”):** MA 2.10.8 sam znalazł odtwarzacz po mDNS, połączył się, handshake Noise na Sentinel (`psk=sn`) przeszedł, `server/activate` z rolami player/controller/metadata/artwork, zegar zbiega. **MA 2.10.8 mówi Noise** (zamyka pytanie otwarte 1 z S01a).
**Niesprawdzone:** licznik ramek audio — MA aktywuje z pustym `activities` (niesparowany), czeka na zgodę operatora w MA albo parowanie tokenem `SP:0…`.
**Publiczne API:** `SendspinNetwork(context, session, scope, log)` → `startServer(name)` / `startClient(host)` / `stop()` / `shutdown()`, `bridge.receivedBinaryFrames`; `NsdAdvertiser.deviceName(context)`. Szczegóły w `core/sendspin/README.md`.
**Przełącznik debug (tymczasowy):** `app/src/debug` — `ContentProvider` startuje odtwarzacz przy starcie procesu (domyślnie włączony, tożsamość w SharedPreferences), broadcast `SENDSPIN_START [--es server <ip>]` / `SENDSPIN_STOP` (komendy w KDoc `SendspinDebug`). Loguje stan, zdarzenia i co 5 s licznik ramek. S05 usuwa cały `app/src/debug`.
**Dalej (S05):** usługa trzyma `SendspinNetwork` + sesję, `CredentialStore` na DataStore, ekran tokenu parowania; ustawienie nazwy odtwarzacza → `startServer(newName)`.
**Pułapki:** `SessionBridge` jest jedynym kolektorem `session.outgoing` przez całe życie sesji — nie zbierać go nigdzie indziej. Ramki wyprodukowane bez połączenia są gubione. `SendspinServer.stop()` blokuje (~1 s) — nie z wątku UI. Proces bez usługi pierwszoplanowej ginie w tle (S05).
