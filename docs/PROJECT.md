# MA TV Player — projekt aplikacji Android TV dla Music Assistant

Oct 26, 2026 · @Łukasz

## Cel i zakres

Telewizor z Android TV staje się pełnoprawnym, osobnym odtwarzaczem w Music Assistant 2.10.5. Muzyka gra z głośników TV (lub podpiętego soundbara), a całą obsługę robisz pilotem.

**W zakresie:**

- TV widoczny w MA jako osobny odtwarzacz (np. „Salon TV”) — można też na niego puszczać muzykę z aplikacji MA na telefonie lub z Home Assistant
- przeglądanie biblioteki: wykonawcy, albumy, utwory, playlisty, ostatnio odtwarzane
- źródła z wtyczek zainstalowanych na serwerze: radio (np. TuneIn / Radio Browser), YouTube Music i inne providery
- wyszukiwarka z klawiaturą ekranową i (opcjonalnie) głosem
- ekran „Teraz odtwarzane” z okładką, postępem, kolejką, losowo / powtarzanie
- sterowanie pilotem: D-pad, OK, Wstecz, Play/Pauza, następny/poprzedni

**Poza zakresem (na razie):** zarządzanie innymi odtwarzaczami i grupami, edycja ustawień serwera, wideo.

## Architektura

Aplikacja łączy się z serwerem MA dwoma niezależnymi kanałami. Jednym odbiera dźwięk jako odtwarzacz, drugim przegląda bibliotekę i steruje kolejką.

**1. Kanał odtwarzacza — Sendspin (TV = osobny odtwarzacz)**

[Sendspin](https://www.music-assistant.io/player-support/sendspin) to własny protokół Music Assistant do zsynchronizowanego multiroomu. Aplikacja implementuje w nim role `player`, `metadata`, `artwork` i `controller`.

- Aplikacja ogłasza się w sieci przez mDNS jako `_sendspin._tcp` (port 8928, ścieżka `/sendspin`), a MA sam się do niej podłącza. Zapasowo może sama łączyć się z serwerem na `ws://<serwer>:8927/sendspin` ([specyfikacja](https://github.com/Sendspin/spec)).
- MA widzi TV jako zwykły odtwarzacz: można go grupować z innymi głośnikami i puszczać na nim muzykę z dowolnego miejsca.
- Dźwięk przychodzi jako FLAC, Opus lub PCM ze znacznikami czasu, a synchronizacja zegara (`client/time` ↔ `server/time`) pozwala grać równo z innymi głośnikami.
- Odtwarzanie działa w usłudze pierwszoplanowej Media3 `MediaSessionService`, więc muzyka gra także przy wyłączonym ekranie, a przyciski multimedialne pilota działają globalnie.

**2. Kanał sterowania — WebSocket API (biblioteka i wtyczki)**

Połączenie z `ws://<serwer>:8095/ws`, uwierzytelnione tokenem użytkownika MA (logowanie loginem i hasłem przy pierwszym uruchomieniu, potem token długoterminowy).

| Funkcja | Komenda API (do potwierdzenia na 2.10.5) |
| --- | --- |
| Biblioteka | `music/artists/library_items`, `music/albums/library_items`, `music/tracks/library_items`, `music/playlists/library_items` |
| Wtyczki: radio, YouTube itd. | `music/browse` (drzewo providerów), `music/radios/library_items` |
| Wyszukiwanie | `music/search` (wszystkie providery naraz) |
| Odtwarzanie na TV | `player_queues/play_media` z `queue_id` = id odtwarzacza TV |
| Kolejka i tryby | `player_queues/items`, `player_queues/shuffle`, `player_queues/repeat` |
| Stan na żywo | zdarzenia `player_updated`, `queue_updated`, `queue_items_updated` |

Wtyczki nie wymagają osobnego kodu w aplikacji: MA wystawia je jako providery, więc pojawiają się automatycznie w „Źródłach”. Dokładne nazwy komend potwierdzę w dokumentacji API Twojego serwera (`http://<serwer>:8095/api-docs`).

&#91;embedded content: architektura · 2 kanały między TV a serwerem MA\]

Dźwięk płynie z serwera do TV przez Sendspin, a przeglądanie i sterowanie idą przez WebSocket API — awaria jednego kanału nie zatrzymuje drugiego.

## Ekrany i nawigacja pilotem

Aplikacja ma sześć ekranów i boczne menu (rozwija się strzałką w lewo). Makiety wszystkich ekranów: [MA TV Player — makiety](https://claude.ai/artifact/5oJWbFsKLpTXk2JQ1xG3KG).

| Ekran | Co pokazuje |
| --- | --- |
| Start | Wiersze kafelków: ostatnio odtwarzane, Twoje playlisty, ulubione stacje radia, polecane z YouTube |
| Biblioteka | Zakładki Wykonawcy / Albumy / Utwory / Playlisty, siatka okładek, szybki skok po literze |
| Źródła | Lista wtyczek z serwera (Radio, YouTube Music, …) i przeglądanie ich drzewa folderów |
| Szczegóły albumu / wykonawcy | Duża okładka, przyciski Odtwórz / Losowo / Dodaj do kolejki, lista utworów |
| Szukaj | Klawiatura ekranowa lub głos, wyniki pogrupowane wg typu i źródła |
| Teraz odtwarzane | Pełnoekranowa okładka z rozmytym tłem, postęp, sterowanie, podgląd kolejki |

Mini-odtwarzacz na dole każdego ekranu pokazuje, co gra. Po 60 s bezczynności aplikacja przechodzi w tryb wygaszacza z okładką.

| Przycisk pilota | Działanie |
| --- | --- |
| D-pad | Ruch po kafelkach; lewo na krawędzi otwiera menu |
| OK | Otwórz element; przytrzymanie = menu kontekstowe (Odtwórz następny, Do kolejki, Ulubione) |
| Wstecz | Poprzedni ekran; na Teraz odtwarzane zamyka pełny ekran |
| Play/Pauza, ⏭ ⏮ | Działają zawsze, także przy wygaszonym ekranie |
| Mikrofon | Wyszukiwanie głosowe (jeśli pilot go ma) |

## Stos technologiczny

| Warstwa | Wybór |
| --- | --- |
| Język | Kotlin, minSdk 26 (Android 8.0), targetSdk 35 |
| UI | Jetpack Compose + `androidx.tv:tv-material` (fokus, karuzele, karty pod D-pad) |
| Sieć | OkHttp WebSocket (API MA), Ktor CIO server (gniazdo Sendspin), `NsdManager` (mDNS) |
| Szyfrowanie Sendspin 1.0 | Noise `KKpsk2` — biblioteka `noise-java` |
| Audio | Media3 `MediaSessionService` + `AudioTrack` z własnym buforem czasowym; dekodery FLAC/Opus przez `MediaCodec` |
| Obrazy | Coil (okładki z cache) |
| Architektura kodu | MVVM, Kotlin Coroutines/Flow, Hilt, DataStore na ustawienia i token |
| Budowanie | Gradle, APK budowany w chmurze, instalacja przez `adb connect <IP TV>` |

## Plan etapów

Najpierw TV musi grać jako odtwarzacz, bo to najtrudniejsza część. Interfejs dokładamy na działający dźwięk.

1. **Etap 0 — makiety i decyzje** (ten dokument + makiety): akceptacja wyglądu i aktualizacja serwera MA.
2. **Etap 1 — odtwarzacz Sendspin:** TV pojawia się w MA, gra muzykę puszczoną z telefonu, przyciski pilota działają. Pierwszy APK do testu.
3. **Etap 2 — połączenie z API i Teraz odtwarzane:** logowanie, ekran odtwarzania, kolejka.
4. **Etap 3 — Biblioteka i Szczegóły:** przeglądanie, odtwarzanie z TV.
5. **Etap 4 — Źródła i Szukaj:** radio, YouTube i pozostałe wtyczki, wyszukiwarka.
6. **Etap 5 — szlif:** wygaszacz, ulubione, wyszukiwanie głosowe, autostart, ikona na ekranie głównym TV.

Po każdym etapie dostajesz APK do testu na swoim telewizorze.

## Ryzyka i otwarte pytania

**Najważniejsze: wersja serwera.** Sendspin 1.0 (szyfrowany, stabilna specyfikacja) wymaga MA 2.10.7 lub nowszego. Masz 2.10.5, który obsługuje tylko wcześniejszą, nieszyfrowaną wersję protokołu w trybie „technical preview” — a obsługa starszych klientów ma zostać usunięta. Rekomendacja: zaktualizować MA przed Etapem 1 i budować tylko pod Sendspin 1.0.

- **Synchronizacja i opóźnienie dźwięku:** TV często ma opóźnienie HDMI/soundbaru. MA pozwala ustawić stałe opóźnienie 0–5000 ms; dodam też kalibrację w aplikacji.
- **Jeden serwer Sendspin w sieci:** Sendspin działa tylko z jednym serwerem MA na sieć.
- **Wydajność tanich TV:** słabsze przystawki mogą mieć problem z płynnością dużych siatek okładek — stronicowanie i małe miniatury.
- **Gotowy klient jako punkt odniesienia:** istnieje [SendspinDroid](https://newreleases.io/project/github/chrisuthe/SendspinDroid/release/v2.0.0) z obsługą Android TV, ale jest tylko odtwarzaczem bez przeglądania biblioteki. Można go użyć do szybkiego sprawdzenia, czy Sendspin działa w Twojej sieci.

**Otwarte pytania:**

- [ ] Czy możesz zaktualizować MA do ≥ 2.10.7?
- [ ] Model telewizora / przystawki i wersja Android TV?
- [ ] Czy w MA włączone jest logowanie użytkowników (potrzebny login i hasło lub token)?
- [ ] Które wtyczki masz zainstalowane (nazwy providerów radia i YouTube)?

## Źródła

- [Music Assistant — Sendspin provider](https://www.music-assistant.io/player-support/sendspin)
- [Specyfikacja Sendspin](https://github.com/Sendspin/spec)
- [Music Assistant 2.10 — ogłoszenie](https://www.music-assistant.io/blog/2026/08/26/music-assistant-2-10/)
- [SendspinDroid 2.0.0](https://newreleases.io/project/github/chrisuthe/SendspinDroid/release/v2.0.0)
- [Uwierzytelnianie API MA](https://instagit.com/music-assistant/server/webserver-api-authentication/)
