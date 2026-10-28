# Spinroom Player — for Music Assistant

Aplikacja Android TV, która zamienia telewizor w osobny odtwarzacz [Music Assistant](https://www.music-assistant.io/). Muzyka gra z głośników TV albo soundbara, a bibliotekę, radio i YouTube Music przeglądasz pilotem.

> Nieoficjalny klient Music Assistant. Projekt nie jest powiązany z Music Assistant ani z Open Home Foundation.

**Status:** wersja 1.0. Zmiany: [CHANGELOG.md](CHANGELOG.md). Plan: [docs/ROADMAP.md](docs/ROADMAP.md).

## Wymagania
- Music Assistant 2.10.7 lub nowszy (Sendspin 1.0)
- Android TV / Google TV z Androidem 8.0+

## Instalacja na TV
1. Pobierz `spinroom-vX.Y.Z.apk` z [Releases](https://github.com/Riberrr/spinroom-player/releases).
2. Włącz na TV opcje programisty i debugowanie sieciowe, a potem:
   ```bash
   adb connect <IP-telewizora>
   adb install -r spinroom-vX.Y.Z.apk
   ```
3. Do automatycznych aktualizacji dodaj repozytorium w [Obtainium](https://github.com/ImranR98/Obtainium) (źródło: GitHub, adres `https://github.com/Riberrr/spinroom-player`). Kolejne wersje instalują się na poprzednie bez odinstalowania, bo są podpisane tym samym kluczem.

## Wygląd
Makiety ekranów (zrzuty z urządzenia dojdą po wydaniu): [Start](design/screens/1-start.png), [Biblioteka](design/screens/2-biblioteka.png), [Źródła](design/screens/3-zrodla.png), [Szczegóły](design/screens/4-szczegoly.png), [Szukaj](design/screens/5-szukaj.png), [Teraz odtwarzane](design/screens/6-teraz-odtwarzane.png).

## Pierwsze uruchomienie
W Ustawieniach wpisz adres serwera Music Assistant i zaloguj się. W Music Assistant zatwierdź odtwarzacz „Telewizor” (Odtwarzacze, Telewizor, Pairing). „O aplikacji” w Ustawieniach pokazuje wersję i licencje.

## Budowanie
JDK 17+ i Android SDK 36:
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. Wydanie powstaje automatycznie po wypchnięciu tagu `v*`.

## Licencja
[MIT](LICENSE)
