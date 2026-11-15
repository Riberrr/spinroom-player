# SFix03a — Crash przy zajętym porcie Sendspin
**Model:** Sonnet · **Gałąź:** `fix-sendspin-port` · **Moduły:** `:core:sendspin` (ew. `:core:audio`)

## Błąd
Gdy port 8928 jest zajęty (np. przez drugi wariant aplikacji, wydanie i debug na jednym urządzeniu), aplikacja pada 1–4 s po starcie:
`java.net.BindException: Address already in use` z `io.ktor.server.cio.backend.HttpServerKt$httpServer$acceptJob$1`.
Wyjątek leci z korutyny CIO bez obsługi, więc zabija cały proces (także usługę `SpinroomPlaybackService`).

## Gdzie
- `core/sendspin/.../transport/SendspinServer.kt:42`: `embeddedServer(CIO, port = port).start(wait = false)`. Bind jest asynchroniczny, więc `try/catch` wokół `start()` nic nie da.
- `transport/SendspinNetwork.kt:25` (`startServer`) i `transport/NsdAdvertiser.kt:21` (`register(port = DEFAULT_PORT)`): mDNS ogłasza stały port, a nie rzeczywisty.

## Zadania
1. Test JVM: zajmij port `ServerSocket`, uruchom `SendspinServer` na tym porcie i sprawdź, że proces nie pada, a serwer działa na innym porcie albo zgłasza błąd przez API.
2. Poprawka: sprawdź port przed startem (albo zrób bind synchronicznie). Przy zajętym 8928 spróbuj 8929…8938, potem port 0. `boundPort()` zwraca rzeczywisty port.
3. mDNS ma ogłaszać `boundPort()`, a nie `DEFAULT_PORT`. Sprawdź, czy MA łączy się z portu z TXT/SRV (docs/SENDSPIN.md; nie pobieraj specyfikacji).
4. Każdy błąd startu serwera: log `Spinroom` + stan błędu, bez crasha.

## Gotowe, gdy
Test zielony, `bash scripts/check.sh` czyste. Na emulatorze uruchomione oba warianty naraz bez crasha (`adb logcat -d -b crash | tail` puste). PR otwarty.

## Wynik
_(uzupełnia sesja)_
