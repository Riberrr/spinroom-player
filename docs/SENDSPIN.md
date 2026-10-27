# Sendspin 1.0 — streszczenie dla Spinroom

Źródło: github.com/Sendspin/spec (gałąź `main`, po tagu `1.0.0-rc1`; stan z 2026-10-27) + music-assistant.io/player-support/sendspin. **Uwaga:** spec `main` ≠ `rc1` — doszło obowiązkowe szyfrowanie Noise i parowanie (sekcja 11). Przykładowe wiadomości: `core/sendspin/src/test/resources/fixtures/`. Kontrakty S02/S04: `core/sendspin/.../api/`.

## 1. Role
Klient deklaruje w `supported_roles` (kolejność = priorytet): `player@v1` (gra dźwięk), `controller@v1` (steruje grupą), `metadata@v1` (tytuł/artysta/postęp), `artwork@v1` (okładki), `visualizer@v1`, `color@v1`, `source@v1` (wysyła audio do serwera). Serwer aktywuje po jednej wersji na rodzinę (`active_roles` w `server/activate`) i może pominąć rolę. Klient nie wysyła stanu/komend dla ról nieaktywnych i odrzuca binarki nieaktywnych ról. Nieznane pola JSON i nieznane `type` — ignorować.

## 2. Transport i odkrywanie
- WebSocket, zwykłe `ws://` (poufność daje Noise wewnątrz). Dwa tryby, klient używa **dokładnie jednego**:
  - **Server-initiated (zalecany):** klient ogłasza mDNS `_sendspin._tcp.local.`, port **8928**, TXT `path=/sendspin` (wymagane), `name=<przyjazna nazwa>` (opcjonalne). Serwer (MA) łączy się do klienta. Klient **odrzuca** upgrade z nagłówkiem `Origin`. Podczas ogłaszania klient NIE łączy się sam do serwerów.
  - **Client-initiated:** serwer ogłasza `_sendspin-server._tcp.local.`, port **8927**, TXT `path=/sendspin`; klient łączy się sam. Podczas tego trybu klient NIE ogłasza `_sendspin._tcp`.
- MA: `ws://<host>:8927/sendspin` (port 8095 `/sendspin` jest tylko dla wbudowanego playera webowego). W MA można też wpisać ręcznie IP odtwarzacza (discovery ręczne).
- Jedno aktywne połączenie naraz; kolejne serwery: wyższy `activities` wygrywa (`playback` > `pairing` > puste); przegrany dostaje `client/goodbye` `another_server`/`concurrent_attempt`. Klient pamięta `server_id` ostatniego serwera z `playback`. Połączenie bez `server/activate` przez 30 s — zerwać.

## 3. Kolejność handshake
Wszystkie komunikaty to JSON `{"type": "...", "payload": {...}}`. Jawne (WS **text**): 1–4; potem tylko WS **binary** (Noise transport).
1. C→S `client/init` {client_id, version:1, suite}
2. S→C `server/init` {server_id, version:1}, zaraz potem
3. S→C `noise/handshake` {data} — Noise msg 1 (payload: `psk_id`, `psk_category`)
4. C→S `noise/handshake` {data} — Noise msg 2 (payload `{}`)
5. (szyfrowane) S→C `server/hello` → C→S `client/hello` → S→C `server/activate`
6. C→S `client/state` (`available:true` dopiero po zbiegnięciu zegara) → S→C `group/update`, `server/state`, `stream/start`, audio…
Timeout na każdy krok handshake ~30 s. Błąd z samego `client/init` → `server/error` {reason: `unsupported_version`|`unsupported_suite`|`malformed`} i zamknięcie; każda inna usterka handshake = ciche zamknięcie.

## 4. Wiadomości (pola)
- `client/hello`: name (≤64 B), device_info? {product_name, manufacturer, software_version, mac_address}, supported_roles, `player@v1_support` {supported_formats[{codec,channels,sample_rate,bit_depth}], buffer_capacity}, **supported_pair_methods** (obiekt; musi zawierać `pairing_psk`), **unpaired_access** {enabled}.
- `server/hello`: name, languages?.
- `server/activate`: activities[`playback`|`pairing`], active_roles?, pairing? {method, format?}.
- `client/state`: available, player? {volume 0–100, muted, output_delay_ms, required_lead_time_ms, min_buffer_ms, supported_commands[`volume`,`mute`,`set_output_delay`], format?}. Pełny stan roli w każdej wiadomości; wysyłać przy każdej zmianie (także po zmianie głośności pilotem).
- `server/command`: player {command, volume?, mute?, output_delay_ms?} (tylko komendy z `supported_commands`).
- `stream/start`: server_transmitted, player {codec, sample_rate, channels, bit_depth, codec_header? (Base64 std)}, artwork {channels[{source,format,width,height}]}. Powtórny `stream/start` zmienia format w locie (stare chunki dekoduj starym formatem, bez przerwy).
- `stream/clear` {server_transmitted, roles?} — seek/skok: opróżnij bufory, strumień trwa. `stream/end` {roles?} — koniec strumienia (stop, wyczyść). Zmiana utworu = **bez** żadnej z nich (gapless).
- `group/update`: playback_state (`playing`|`stopped`), group_id, group_name.
- `server/state`: metadata? (sekcja 8), controller? (sekcja 9), color?.
- `client/command` {controller:{command, volume?, mute?, position_ms?, offset_ms?}}; `client/leave` (opuść grupę).
- `client/goodbye` {reason: `another_server`|`shutdown`|`restart`|`user_request`|`unauthorized`|`pairing_required`|`concurrent_attempt`|`unpaired`}; `server/unpair` (serwer kasuje parowanie → klient kasuje rekord, wysyła goodbye `unpaired`).
- `client/time` {client_transmitted} / `server/time` {client_transmitted, server_received, server_transmitted} (sekcja 6).
- Pairing: `client/pair-init`, `client/pair-finalize`, `server/pair-finalize`, `pair/abort` {reason} (sekcja 11).

## 5. Ramki binarne (po deszyfrowaniu; bajt 0 = ID)
| ID | Znaczenie | Układ |
| --- | --- | --- |
| 0 | JSON | UTF‑8 `{type,payload}` |
| 1 | fragment | `[1][flags][orig_type][data]` pierwszy, `[1][flags][data]` kolejne; flags bit1=pierwszy, bit0=ostatni; reszta 0; jedna wiadomość naraz |
| 4 | audio (player) | `[4][ts int64 BE µs][send_ahead uint32 BE µs][dane]`; <13 B = błąd protokołu |
| 8–11 | okładka kanał 0–3 | announce `[id][flags=2][ts int64][total_size uint32]`, część `[id][flags=0][dane]`, cancel `[id][flags=1]`; `total_size=0` = wyczyść; naraz jeden transfer |
| 16–20 | visualizer | pomijamy |
- Limit: jeden komunikat Noise ≤ 65535 B (payload aplikacji ≤ 65518 B) — większe JSON-y fragmentowane (ID 1). Nieznane ID binarne ignorować (ale deszyfrować i śledzić fragmenty).
- Audio: `ts` = czas serwera, kiedy pierwsza próbka ma wyjść z głośnika; gra się `compute_client_time(ts) − output_delay`. `send_ahead` służy tylko do pomiaru jittera (→ `min_buffer_ms`), nie do planowania. Chunk 15–150 ms; spóźnione chunki odrzucać. Serwer nie przekracza `buffer_capacity` (liczone nagłówek 13 B + dane).

## 6. Synchronizacja zegara
Serwer ma monotoniczny zegar w µs. Klient wysyła **serie** `client/time` ≥ 8 wiadomości (następna po odpowiedzi na poprzednią), serie co ≤ 10 s; do filtra trafia tylko pomiar z najmniejszym `max_error`. T1=client_transmitted, T2=server_received, T3=server_transmitted, T4=lokalny czas odbioru (ostatni bajt WS). Wszystko `Long` µs, dzielenie całkowite (do zera):
`measurement = ((T2−T1)+(T3−T4))/2`, `max_error = ((T4−T1)−(T3−T2))/2`, `time_added = T4`.
Filtr Kalmana 2D (stan: offset, drift, P_oo, P_od, P_dd, last_update, count, use_drift; start P_oo=∞, reszta 0; double):
1. odrzuć gdy `time_added ≤ last_update` lub `max_error ≤ 0`; `dt = time_added − last_update`; `R = (0.5·max_error)²`.
2. count=0: offset=measurement, P_oo=R, count=1. count=1: drift=(m−offset)/dt, P_dd=(P_oo+R)/dt², offset=m, P_oo=R, count=2.
3. predykcja: `offset_p = offset+drift·dt`; `P_oo_p = P_oo+2P_od·dt+P_dd·dt²+process_std²·dt`; `P_od_p = P_od+P_dd·dt`; `P_dd_p = P_dd+drift_std²·dt`.
4. `y = m − offset_p`; count<100 → count++; inaczej gdy `|y| > 3·max_error` pomnóż P_*_p przez `forget²` (=4).
5. korekta: `S=P_oo_p+R; K_o=P_oo_p/S; K_d=P_od_p/S; offset=offset_p+K_o·y; drift+=K_d·y; P_oo=P_oo_p−K_o·P_oo_p; P_od=P_od_p−K_d·P_oo_p; P_dd=P_dd_p−K_d·P_od_p`; `use_drift = drift² > 2²·P_dd`.
Parametry: process_std=0, drift_process_std=1e‑11, forget_factor=2, adaptive_cutoff=3, min_samples=100, drift_significance=2, max_error_scale=0.5.
Konwersja (d = drift jeśli use_drift, inaczej 0): `server = t + round(offset + d·(t − last_update))`; `client = round((t_srv − offset + d·last_update)/(1+d))`. Referencja: github.com/Sendspin/time-filter (C++). Gracz zgłasza `available:true` dopiero po zbiegnięciu filtra.
Wymagania synchronizacji: błąd ≤ ±1 ms (cel 0,5 ms), korekta max ±0,5 % prędkości; zalecana korekta przez usuwanie/dublowanie ramek (N = max(1, round(21 µs·fs/1e6))) z martwą strefą ~100 µs; duży błąd lub start → jednorazowe przycięcie/cisza.

## 7. Kodeki
`flac`, `pcm`, `opus` (serwer musi umieć flac+pcm, opus opcjonalnie; gracz musi wystawić flac lub pcm). Chunk zawiera całe jednostki kodeka:
- **pcm:** little-endian signed, 24 bit = 3 B/próbka, interleaved; brak `codec_header`.
- **flac:** pełne ramki FLAC; `codec_header` (Base64 std z paddingiem) = `fLaC` + blok STREAMINFO — dekoder konfigurowany nim.
- **opus:** dokładnie jeden pakiet Opus (RFC 6716) na chunk, bez kontenera, brak nagłówka; dekoder z `sample_rate`/`channels` ze `stream/start`. Patenty — nasza sprawa licencyjna.
Głośność: `amplitude = (volume/100)^1.5`, z krótką rampą; `muted` niezależne od `volume`. MA wysyła audio jako 16‑bit.

## 8. Metadane i okładki
`server/state.metadata`: timestamp (µs czasu serwera; przyszły = zaplanowana zmiana, max 1 oczekująca), title?, artist?, album_artist?, album?, artwork_url?, year?, track?, progress? {track_progress ms, track_duration ms (0 = nieznany), playback_speed ×1000 (0 = pauza)}. Pozycja: `progress + (now_server − timestamp)·speed/1e6`, obcięta do `[0, duration]`. Brak `progress` w nowym stanie = brak pozycji. Teksty to plain text (nie interpretować jako HTML). Okładki: `client/state.artwork.channels[]` (źródło `album`|`artist`|`none`, `jpeg`|`png`, width×height — serwer skaluje z paddingiem czarnym); serwer wysyła obraz po `stream/start` artwork.

## 9. Controller
`server/state.controller`: supported_commands[`play`,`pause`,`stop`,`next`,`previous`,`volume`,`mute`,`repeat_off|one|all`,`shuffle`,`unshuffle`,`switch`,`seek`,`seek_relative`], volume, muted, repeat, shuffle, seek_max_ms?. Wysyłać tylko komendy z listy. Volume/mute dotyczą **całej grupy**. `seek` wymaga `position_ms` w `[0, seek_max_ms]`, `seek_relative` — `offset_ms`.

## 10. Błędy i ponowne łączenie
- Zerwanie bez goodbye na połączeniu `playback`/puste: serwer zakłada `restart` i sam wraca (mDNS). Goodbye `shutdown`/`restart` → serwer wraca; `another_server`/`user_request`/`unpaired`/`pairing_required`/`unauthorized` → nie wraca sam. W trybie client-initiated reconnect robi klient (backoff wykładniczy).
- Błąd AEAD, ramka cleartext po przejściu w tryb transport, pusta ramka, zła sekwencja fragmentów, audio <13 B → zamknij WS bez komunikatu. Serwer stosuje backoff po powtarzanych porażkach handshake.
- Ping/Pong WS = liveness (nieszyfrowane).
- **Re-handshake** w locie: serwer wysyła `noise/handshake` jako zaszyfrowane JSON-y (ID 0), nowy prolog = poprzedni `h`; potem pierwszy komunikat to `server/activate`. Stan (strumienie, filtr czasu) zostaje.

## 11. Noise `KKpsk2` i parowanie
- Wzorzec **KKpsk2**; **serwer = initiator, klient = responder** (niezależnie kto otworzył WS). Zestawy: `25519_ChaChaPoly_SHA256` (preferowany u nas) lub `25519_AESGCM_SHA256`; klient wybiera w `client/init`, serwer musi znać oba. Pełna nazwa: `Noise_KKpsk2_25519_ChaChaPoly_SHA256`.
- **Klucze statyczne:** `client_id`/`server_id` = base64url (bez paddingu, 43 zn.) klucza publicznego Curve25519. Klient generuje parę raz (CSPRNG), trzyma na stałe (DataStore/Keystore); prywatny klucz nigdy nie wychodzi. Klucz serwera klient poznaje z `server/init` (przed wiadomością 1 musi go mieć — wzorzec KK: znane obu stronom).
- **Prolog:** surowe bajty `client/init` + surowe bajty `server/init` dokładnie jak na drucie (nie re-serializować!).
- **PSK** (32 B), trzy kategorie, wskazywane w msg 1 przez `psk_id = base64url(SHA‑256("sendspin-psk-id-v1" ‖ PSK))` i `psk_category`: `lt` (long‑term, z parowania), `pr` (pairing PSK klienta), `sn` (Sentinel).
  - **Sentinel PSK** = `SHA‑256("sendspin-sentinel-psk-v1")` = `1b5e24dbc1aed95fc2a5a338a90c05df44bd10f5ec1f4cd66cbf86272767b9d3` (publiczna stała; `psk_id` = `GFsV9tLaSQm9HcFWpKsgYQOr7wFTvNUtkmFwuVz3zoo`).
  - Klient odszyfrowuje payload msg 1 (bez PSK), szuka PSK o tym `psk_id` i kategorii, miksuje w msg 2. Brak trafienia w handshake początkowym → **Sentinel** (nie błąd). Po trafieniu `server_id` zapisany przy PSK musi równać się `server_id` z `server/init`.
- **Parowanie (metoda obowiązkowa: Pairing PSK):** klient ma stały, losowy per urządzenie *pairing PSK* (CSPRNG, nigdy wspólny domyślny) i musi go zawsze trzymać wśród kandydatów. Operator wpisuje w MA **token** `SP:0…` = `"SP:" + "0" + base32(client_key[32] ‖ pairing_psk[32])` (RFC 4648, bez `=`, cyfra `2`→`9`; wielkość liter bez znaczenia; wyświetlać też jako QR). Przebieg: handshake z `pr` → `server/activate` activities=[`pairing`], pairing.method=`pairing_psk` → klient od razu wysyła `client/pair-init` {pairing_index} i `client/pair-finalize` {long_term_psk (43 zn. base64url, losowy 32 B)} → serwer odpowiada `server/pair-finalize` → obie strony zapisują rekord *(long_term_psk + server_id)* (min. 5 rekordów, wywłaszczanie LRU) → serwer robi re‑handshake na `lt` i wysyła `server/activate` z `playback`. Przed `pair-init` klient sprawdza, że dopasowane PSK to pairing PSK (inaczej `pair/abort` `method_not_supported`). Timeout próby ~2 min (`attempt_timeout`).
- Inne metody (opcjonalne): `dynamic_pairing_code` (PIN 6 cyfr/QR na ekranie + CPace‑X25519‑SHA512), `static_pairing_code` — na razie nie robimy.
- **Unpaired access** (`unpaired_access.enabled`): sesja na Sentinel, zgoda operatora w MA ("Setup → allow to play without pairing"); gra bez parowania, ale podatna na MITM. Wyłączone → `client/goodbye` `pairing_required`.
- Utrata rekordu / wywłaszczenie → następny handshake ląduje w Sentinel Fallback; serwer widzi "credential mismatch" i proponuje ponowne parowanie. MA oferuje też "Allow legacy clients" (bez szyfrowania, tymczasowe) — **nie** używamy.

## 12. Różnice 1.0 (main) vs preview/rc1
rc1: zwykły WS bez Noise, `client/hello` jako pierwsza wiadomość, brak parowania, ramka audio `[4][ts 8B][dane]` (bez `send_ahead`), zegar bez serii. Teraz: `client/init`/`server/init`/Noise, `server/activate` + `activities`, `send_ahead`, `required_lead_time_ms`/`min_buffer_ms`, `client/leave`, `server/unpair`, `unpaired_access`. Biblioteki `sendspin-jvm` i inne mogą jeszcze mówić rc1 — traktować jako inspirację, nie źródło prawdy.

## Decyzje dla Spinroom
- **Role:** `player@v1` (kodeki: `flac` 44,1/48 kHz 16 bit stereo, `opus` 48 kHz stereo, `pcm` 16 bit 44,1/48 kHz jako ostatni zapas — kolejność w `supported_formats` = priorytet; `bit_depth`≥24 później), `metadata@v1`, `artwork@v1` (1 kanał, album, jpeg, rozmiar z ekranu Now Playing), `controller@v1`. Bez `visualizer`, `color`, `source`.
- **Transport:** TV jest **serwerem WS** (Ktor CIO, port 8928, `/sendspin`) + ogłasza mDNS `_sendspin._tcp` (NsdManager, TXT `path`, `name`). Tryb client-initiated tylko jako zapas (ręczny adres serwera 8927) — nie w v1.
- **Szyfrowanie:** Noise‑Java `org.signal.forks:noise-java:0.1.1` (MIT, Maven Central, zawiera własne Curve25519/ChaChaPoly, wzorzec `KK`; **S02 sprawdza** obsługę modyfikatora `psk2` na zestawie `Noise_KKpsk2_25519_ChaChaPoly_SHA256` testem z wektorami; jeśli brak — cienka nakładka na `HandshakeState` lub własne ~300 linii na JCA/BouncyCastle). Preferowany zestaw: ChaChaPoly (TV bez gwarancji HW AES).
- **Parowanie w UI:** metoda Pairing PSK; ekran w Ustawieniach pokazuje token tekstem + QR (`SP:0…`), pairing PSK i klucz tożsamości w DataStore (klucz prywatny w Android Keystore‑wrapped lub szyfrowany). `unpaired_access` domyślnie **włączony** (łatwy start: operator klika w MA "allow"), wyłączalny w Ustawieniach.
- **Zegar:** własna implementacja filtra z sekcji 6 (testy JVM na danych z referencji); `ClockSource` (kontrakt) dostarcza S02, konsumuje S04.
- **Podział:** S02 = protokół/połączenie/Noise/zegar, wystawia `PlayerStream` + `SendspinConnection`; S04 = dekodowanie, bufor, AudioTrack, korekta dryfu, MediaSession.

## Pytania otwarte
1. Czy MA 2.10.8 (serwer testowy) mówi już spec `main` (Noise) czy tylko rc1 + "legacy"? Sprawdzić ręcznie przy pierwszym połączeniu (S02/S03 — nagranie handshake).
2. Czy Noise‑Java obsługuje `psk2` na KK bez łatek? (S02, test z wektorami.)
3. Czy MA wymaga `unpaired access` lub tokenu — jakie UI parowania pokazuje (PIN vs token)? Zależy od wersji.
4. Kolejność bajtów/endianness nonce'a w Noise‑Java vs spec dla AESGCM — nieistotne przy ChaChaPoly (wybrane).
5. Kształt rekordu w TXT (`name`) i czy MA wymaga innych kluczy mDNS — do potwierdzenia na żywo.
