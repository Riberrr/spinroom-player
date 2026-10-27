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
_(uzupełnia sesja)_
