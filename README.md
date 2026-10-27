# Spinroom Player — for Music Assistant

Aplikacja Android TV, która zamienia telewizor w osobny odtwarzacz [Music Assistant](https://www.music-assistant.io/). Muzyka gra z głośników TV albo soundbara, a bibliotekę, radio i YouTube Music przeglądasz pilotem.

> Nieoficjalny klient Music Assistant. Projekt nie jest powiązany z Music Assistant ani z Open Home Foundation.

**Status:** wczesny rozwój. Szkielet aplikacji jest gotowy, odtwarzacz Sendspin powstaje w Etapie 1. Plan: [docs/ROADMAP.md](docs/ROADMAP.md).

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
3. Do automatycznych aktualizacji możesz użyć [Obtainium](https://github.com/ImranR98/Obtainium) z adresem tego repozytorium.

## Budowanie
JDK 17+ i Android SDK 36:
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. Wydanie powstaje automatycznie po wypchnięciu tagu `v*`.

## Licencja
[MIT](LICENSE)
