# MA-API — streszczenie API Music Assistant

Zweryfikowane na serwerze **Music Assistant 2.10.8** (schema_version 65, min. 28), 2026-10-27.
Źródła: `GET /api-docs/commands.json`, `/api-docs/schemas.json` (maszynowo czytelne, 127 schematów), żywe zapytania bez logowania.
Legenda: ✅ potwierdzone na serwerze/w docs; ⚠️ z wiedzy o kodzie MA, **do potwierdzenia fixtures** (`scripts/record-fixtures.sh`).
Fixtures lądują w `core/ma-api/src/test/resources/fixtures/` po uruchomieniu skryptu przez użytkownika (patrz „Fixtures").

## 1. Transport
- WebSocket `ws://<host>:8095/ws` ✅. Zaraz po połączeniu serwer wysyła bez pytania **server_info** (bez pola `event`/`message_id`):
  `{"server_id","server_version":"2.10.8","schema_version":65,"min_supported_schema_version":28,"base_url","homeassistant_addon","onboard_done","name","status":"running","internal_url","external_url","has_remote_access"}`.
  Klient powinien sprawdzić `schema_version >= min` i odmówić pracy ze zbyt starym serwerem. Uwaga: `base_url` bywa ze spacjami na początku (`"   http://..."`) — `trim()`.
- Komenda: `{"message_id":"<str>","command":"music/search","args":{...}}` ✅.
- Odpowiedź ✅: `{"message_id","result":<dane>,"partial":false}`; błąd: `{"message_id","error_code":<int>,"details":"..."}`.
  Znane kody: `20` = wymagane uwierzytelnienie, `23` = token nieprawidłowy/wygasły.
  `partial:true` ⚠️ = wynik przychodzi w kawałkach z tym samym `message_id` (m.in. `playlist_tracks`, generator); ostatni ma `partial:false`. Klient składa kawałki.
- Zdarzenia: wiadomości z polem `event` i bez `message_id` ⚠️ (kształt w sekcji 4).
- Równoległy interfejs HTTP ✅: `POST /api` `{"command","args"}` z `Authorization: Bearer <token>` — przydatny w testach i skryptach, klient używa WS.
- Inne endpointy ✅: `GET /info` (jak server_info, bez auth), `GET /auth/providers`, `POST /auth/login`, `GET /api-docs/openapi.json`.

## 2. Uwierzytelnianie
1. `GET /auth/providers` ✅ → `[{provider_id:"builtin",requires_redirect:false},{provider_id:"homeassistant",requires_redirect:true}]`. Obsługujemy tylko `builtin` (login+hasło); `homeassistant` wymaga przekierowania w przeglądarce — poza zakresem.
2. Logowanie ✅ `POST /auth/login` `{"provider_id":"builtin","credentials":{"username","password"}}` → `{"success":true,"token":"...","user":{...}}` albo `{"success":false,"error":"Invalid username or password"}`.
   To samo przez WS: komenda `auth/login` (bez auth) z args `username, password, provider_id, device_name` → `result:{success, token, user}`.
3. Token z logowania jest **krótkotrwały**: okno ślizgające 30 dni, odnawia się przy użyciu ✅ (opis `auth/token/create`).
4. **Długoterminowy token** ✅: `auth/token/create` `{name:"Spinroom TV"}` → `result: "<token>"` (string). Ważny **1 rok, nie odnawia się**; nie dla kont gość. Rekomendacja: po loginie od razu utworzyć długi token o nazwie „Spinroom <nazwa TV>", zapisać go w DataStore (nie hasło!), krótki token wyrzucić. Przy kodzie 23 → wróć do ekranu logowania. Unieważnienie: `auth/token/revoke {token_id}`, lista: `auth/tokens`.
5. Uwierzytelnienie połączenia WS ✅: po server_info wysłać `{"message_id":"1","command":"auth","args":{"token":"<token>"}}`. Zły token → `error_code 23`. Sukces zwraca ⚠️ obiekt użytkownika/ok (zob. fixture `auth_response.json`). Do czasu `auth` każda inna komenda daje `error_code 20`.
6. HTTP: `Authorization: Bearer <token>` ✅. Zakresy tokenu (`auth/scopes`): `library.read/write/manage`, `queues.read/control`, `players.read/control`, `providers.read`, `users.*`, `system.manage`. Nam wystarczą: library.read, library.write (ulubione), queues.*, players.*.
7. `auth/me` ✅ → `User{user_id,username,role,display_name,avatar_url,preferences,provider_filter,player_filter}`.

## 3. Wykrywanie serwera
- ⚠️ MA ogłasza się przez mDNS/Zeroconf jako **`_mass._tcp.local.`** (TXT: `server_id`, `version`, `base_url`, `homeassistant_addon`, `onboard_done`) — użyć Android `NsdManager`; do potwierdzenia na realnej sieci (`dns-sd -B _mass._tcp`). Z TXT wystarczy `base_url`.
- Fallback: ręczny adres (`host[:8095]`) + weryfikacja `GET /info`. Brak odpowiedzi → komunikat „Nie widzę serwera Music Assistant w sieci".
- Serwer za HA add-on ma `homeassistant_addon:true`, port nadal 8095 ✅.

## 4. Zdarzenia (⚠️ nazwy z kodu MA, kształt do potwierdzenia fixture `events.json`)
Kształt: `{"event":"<nazwa>","object_id":"<id>","data":<obiekt|null>}`. Subskrypcja nie jest potrzebna — po `auth` serwer wysyła wszystkie.
| event | object_id | data |
| --- | --- | --- |
| `player_added`, `player_updated`, `player_removed` | `player_id` | pełny `Player` (przy removed — brak/ostatni stan) |
| `queue_added`, `queue_updated` | `queue_id` | pełny `PlayerQueue` (`state`, `elapsed_time`, `current_item`, `shuffle_enabled`, `repeat_mode`) |
| `queue_items_updated` | `queue_id` | `PlayerQueue` — sygnał „lista zmieniona": pobierz ponownie `player_queues/items` |
| `queue_time_updated` | `queue_id` | liczba: `elapsed_time` w sekundach (częste, ~1/s) |
| `media_item_played` / `media_item_added` / `media_item_updated` / `media_item_deleted` | uri | element mediów — do odświeżania „Ostatnio odtwarzane" i biblioteki |
Wskazówka: nie przeładowuj kolejki po każdym `queue_updated`; `queue_time_updated` ignoruj dla pozycji i licz lokalnie z `elapsed_time` + `elapsed_time_last_updated`.

Ważne obiekty ✅ (schemas.json):
- **Player**: `player_id, provider, type, name, available, playback_state(idle|paused|playing|unknown), elapsed_time, elapsed_time_last_updated, powered, volume_level(int), volume_muted, group_members, synced_to, active_source, current_media:PlayerMedia, supported_features, device_info`.
- **PlayerMedia**: `uri, media_type, title, artist, album, album_artist, image_url, palette, duration, queue_item_id, elapsed_time…`.
- **PlayerQueue**: `queue_id, active, display_name, available, items(int=liczba), shuffle_enabled, repeat_mode(off|one|all|unknown), crossfade_enabled, autoplay_enabled, current_index, elapsed_time, elapsed_time_last_updated, state, current_item:QueueItem, next_item, flow_mode`.
- **QueueItem**: `queue_id, queue_item_id, name, duration, index, sort_index, media_item:Track?, image:MediaItemImage?, available`.
- `queue_id` == `player_id` odtwarzacza (nasz TV jako gracz Sendspin) ✅ wg PROJECT.md; potwierdź w `player_queues_all.json`.

## 5. Komendy (✅ nazwa i parametry z commands.json; * = wymagany)
### Biblioteka (zakres `library.read`)
Dla `artists|albums|tracks|playlists|radios|podcasts|audiobooks`: **`music/<typ>/library_items`**
args: `favorite:bool, search:str, limit:int, offset:int, order_by:str, provider:str|[str], genre:int|[int], played_only:bool, summary:bool, user`
oraz tylko artists: `album_artists_only, artist_type`; albums: `album_types`; tracks: `explicit`; playlists/radios/audiobooks: `collapse_collections`.
Zwraca tablicę pełnych obiektów (lub `ItemMapping` — zawsze obsłuż oba). Zliczanie: `music/<typ>/count {favorite_only}`.
- **Stronicowanie** ✅ `limit`+`offset`; brak znacznika końca — koniec, gdy odpowiedź krótsza niż `limit`. Zalecany `limit=50`. `order_by`: `name`, `sort_name`, `timestamp_added`, `last_played`, `random` + sufiks `_desc` ⚠️ (wartości do potwierdzenia na fixtures; `name` pewne).
- Ulubione ✅: ten sam `library_items` z `favorite:true`. Dodaj: `music/favorites/add_item {item:<uri|obiekt>}` (`library.write`); usuń: `music/favorites/remove_item {media_type*, library_item_id*}`.
- Szczegóły: `music/albums/get {item_id*, provider_instance_id_or_domain*}`, `music/albums/album_tracks {item_id*, provider_instance_id_or_domain*, in_library_only}`; `music/artists/get`, `artist_albums`, `artist_tracks`, `top_tracks`, `top_albums`, `similar_artists {…, limit}`; `music/playlists/get`, `music/playlists/playlist_tracks {item_id*, provider_instance_id_or_domain*, force_refresh, allow_dynamic_tracks}` (generator → może przyjść jako `partial`); `music/radios/get`, `music/tracks/get`.
- Uniwersalne: `music/item_by_uri {uri*}`, `music/item {media_type*, item_id*, provider_instance_id_or_domain*}`.
  Wszystkie elementy mają `item_id` + `provider` (domena lub `instance_id`) + `uri` (`<provider>://<typ>/<id>`), `media_type`, `favorite`, `metadata.images[]`.

### Start / ostatnio odtwarzane / rekomendacje
- **`music/recently_played_items`** ✅ args: `limit, media_types:[MediaType], userid, queue_id, fully_played_only, user_initiated_only, played_after_timestamp, providers, always_include_media_types` → `[ItemMapping]`.
- **`music/recommendations`** ✅ (bez args) → `[RecommendationFolder{item_id,provider,name,translation_key,path,icon,items:[…],subtitle…}]` — gotowe wiersze dla Startu (np. „Ostatnio odtwarzane", „Losowe albumy"…). `name`/`translation_key` ⚠️ — tytuły angielskie, mapuj `translation_key` na PL. Więcej: `music/recommendations/items {provider*, item_id*}`.
- `music/in_progress_items {limit}` ✅ (audiobooki/podcasty w toku).

### Źródła i wtyczki
- **`music/browse {path, player_id}`** ✅ → lista `BrowseFolder`/elementów. Bez `path` = korzeń (providery). Wejście w folder: `path` z `BrowseFolder.path` (np. `plex://…`/`spotify://…`) ⚠️. Element z `is_playable:true` można odtworzyć przez `play_media` z jego `uri`.
- Providery: `providers/manifests` (`providers.read`) → lista z `domain, name, type(music|player|plugin…)`; ikona `providers/icon {provider*, variant}`. Aktywne instancje ⚠️ `config/providers` (admin; nam niepotrzebne).

### Wyszukiwanie
**`music/search {search_query*, media_types, limit, library_only, providers, user}`** ✅ → `SearchResults{artists, albums, tracks, playlists, radio, audiobooks, podcasts, genres, sound_effects}` — każde to tablica Obiekt|ItemMapping. Brak stronicowania, tylko `limit` na typ (zalecane 8–10).

### Odtwarzanie i kolejka (zakres `queues.control`)
- **`player_queues/play_media {queue_id*, media*, option, radio_mode, start_item, shuffle, start_from_beginning}`** ✅. `media` = `uri` (string), lista uri, lub obiekt. `option: play|replace|next|replace_next|add` (domyślnie `play`; „Odtwórz następny" = `next`, „Do kolejki" = `add`, „Odtwórz teraz i zastąp" = `replace`). `start_item` = uri/`queue_item_id` utworu w albumie/playliście, od którego zacząć. `radio_mode:true` = pozwól MA dobierać podobne.
- Transport ✅ (wszystkie `{queue_id*}`): `player_queues/play`, `pause`, `play_pause`, `stop`, `next`, `previous`, `resume`; `seek {queue_id*, position(s)}`; `skip {seconds}`; `play_index {queue_id*, index*}`.
- Tryby ✅: `player_queues/shuffle {queue_id*, shuffle_enabled*}`, `player_queues/repeat {queue_id*, repeat_mode*: off|one|all}`, `crossfade`, `autoplay`.
- Zawartość ✅: `player_queues/items {queue_id*, limit, offset}` → `[QueueItem]`; `player_queues/get`, `all`, `get_active_queue {player_id*}`; edycja: `delete_item {queue_id*, item_id_or_index*}`, `move_item {queue_id*, queue_item_id*, pos_shift}`, `clear`, `save_as_playlist`.
- Głośność ✅: `players/cmd/volume_set {player_id*, volume_level*(0–100)}`; ✅ `players/cmd/volume_up`, `volume_down`, `volume_mute`. Stan graczy: `players/all`, `players/get {player_id*}`.
- Uwaga: przy graczu Sendspin-TV sterowanie idzie wyłącznie przez MA (`player_queues/*`), nie lokalnie w ExoPlayer; lokalny przycisk Play ma wołać `player_queues/play_pause`.

## 6. Okładki
- Obiekt: `metadata.images[]` = `MediaItemImage{type(thumb|landscape|fanart|logo|…), path, provider, remotely_accessible, proxy_id}` ✅; w `ItemMapping`/`QueueItem` pole `image` (pojedyncze). `PlayerMedia.image_url` ⚠️ gotowy URL.
- **Image proxy** ✅ endpoint istnieje (`GET /imageproxy` → 400 bez parametrów): ⚠️ `GET http://<host>:8095/imageproxy?path=<urlencoded path>&provider=<provider>&size=<px>` (+ opcjonalnie `fmt=jpeg`). Bierz `path` i `provider` wprost z `MediaItemImage`. Dla `remotely_accessible:true` można użyć `path` jako bezpośredniego URL-a, ale proxy daje skalowanie — **używaj proxy z `size=` zbliżonym do kafelka** (np. 300 siatka, 1080 Teraz odtwarzane). Nowsze wersje ⚠️ mogą używać `proxy_id` — jeśli ustawione, preferuj je (zob. fixtures). Obrazy nie wymagają nagłówka auth ⚠️ (Coil: zwykły URL).
- Paleta kolorów: `metadata/get_image_palette` ✅ (spec Sendspin color@v1) — przydatne do rozmytego tła.

## 7. Co dalej / pułapki
- Dane z `library_items` mogą być `ItemMapping` (okrojone) — przy szczegółach zawsze `…/get`.
- `queue_id` do `play_media` bierz z `Player`, nie wymyślaj; pierwsze uruchomienie: poczekaj aż TV zarejestruje się jako gracz (Sendspin) zanim wyślesz komendę.
- Token tylko w DataStore, nigdy w logach (loguj `command`, nie `args` z `token`/`password`).
- Wszystkie ⚠️ rozstrzygnie nagranie fixtures; po nim S06 powinien poprawić ten plik (SFix).

## Fixtures
`MA_TOKEN=<token> bash scripts/record-fixtures.sh` (Node 22+). Token: zaloguj się w UI MA → profil → długoterminowy token, albo `auth/token/create`. Skrypt: łączy się po WS, wysyła `auth`, nagrywa ~45 odpowiedzi (biblioteka strony 1/2 + ulubione, browse, recently played, recommendations, search, album/artist/playlist, kolejka), potem 25 s zdarzeń (w tym czasie naciśnij play/pauza/następny/shuffle w MA) → `fixtures/*.json`. Anonimizacja: adresy IP, MAC, token, nazwy użytkowników/e-maile; tytuły muzyki zostają. Przed commitem przejrzyj pliki.
