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
_(uzupełnia sesja)_
