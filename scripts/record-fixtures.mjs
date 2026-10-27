// Records anonymised Music Assistant API responses as test fixtures.
// Run via scripts/record-fixtures.sh (needs Node 22+, which has a global WebSocket).
import { mkdirSync, writeFileSync } from 'node:fs';

const server = process.env.MA_SERVER || 'http://10.0.1.50:8095';
const token = process.env.MA_TOKEN;
const outDir = process.argv[2];
const eventSeconds = Number(process.env.MA_EVENT_SECONDS ?? 25);
if (!token || !outDir) { console.error('MA_TOKEN and output dir are required'); process.exit(2); }
mkdirSync(outDir, { recursive: true });

const SECRET_KEYS = /^(token|password|access_token|refresh_token|email|username|display_name|avatar_url|token_hash|api_key)$/i;
const ipRe = /\b\d{1,3}(?:\.\d{1,3}){3}\b/g;
const macRe = /\b[0-9a-f]{2}(?::[0-9a-f]{2}){5}\b/gi;
function anon(v, key) {
  if (Array.isArray(v)) return v.map((x) => anon(x));
  if (v && typeof v === 'object') return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, anon(x, k)]));
  if (typeof v === 'string') {
    if (key && SECRET_KEYS.test(key)) return 'REDACTED';
    return v.replace(macRe, '00:00:00:00:00:00').replace(ipRe, '192.0.2.1').split(token).join('REDACTED');
  }
  return v;
}

const ws = new WebSocket(server.replace(/^http/, 'ws') + '/ws');
const pending = new Map();
const events = [];
let seq = 0;
const send = (command, args = {}) => new Promise((resolve) => {
  const message_id = String(++seq);
  pending.set(message_id, resolve);
  ws.send(JSON.stringify({ message_id, command, args }));
});
const save = (name, data) => {
  writeFileSync(`${outDir}/${name}.json`, JSON.stringify(anon(data), null, 2) + '\n');
  console.log('saved', name);
};
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.message_id && pending.has(m.message_id)) { pending.get(m.message_id)(m); pending.delete(m.message_id); }
  else if (m.event) events.push(m);
  else if (m.server_version) save('server_info', m);
};
await new Promise((r, j) => { ws.onopen = r; ws.onerror = () => j(new Error('ws connect failed')); });

const auth = await send('auth', { token });
save('auth_response', auth);
if (auth.error_code) { console.error('auth failed:', auth.details); process.exit(1); }

async function rec(name, command, args) {
  const r = await send(command, args);
  save(name, r.error_code ? { error: r } : r);
  return r.result;
}

await rec('auth_me', 'auth/me');
await rec('players_all', 'players/all');
const queues = await rec('player_queues_all', 'player_queues/all');
await rec('providers_manifests', 'providers/manifests');
for (const t of ['artists', 'albums', 'tracks', 'playlists', 'radios']) {
  await rec(`library_${t}_page1`, `music/${t}/library_items`, { limit: 5, offset: 0, order_by: 'name' });
  await rec(`library_${t}_page2`, `music/${t}/library_items`, { limit: 5, offset: 5, order_by: 'name' });
  await rec(`library_${t}_favorites`, `music/${t}/library_items`, { limit: 5, favorite: true });
}
await rec('browse_root', 'music/browse');
await rec('recently_played', 'music/recently_played_items', { limit: 10 });
await rec('recommendations', 'music/recommendations');
await rec('search', 'music/search', { search_query: process.env.MA_SEARCH || 'a', limit: 3 });

const album = (await rec('library_albums_one', 'music/albums/library_items', { limit: 1 }))?.[0];
if (album) {
  await rec('album_get', 'music/albums/get', { item_id: album.item_id, provider_instance_id_or_domain: album.provider });
  await rec('album_tracks', 'music/albums/album_tracks', { item_id: album.item_id, provider_instance_id_or_domain: album.provider });
}
const artist = (await rec('library_artists_one', 'music/artists/library_items', { limit: 1 }))?.[0];
if (artist) {
  await rec('artist_albums', 'music/artists/artist_albums', { item_id: artist.item_id, provider_instance_id_or_domain: artist.provider });
  await rec('artist_top_tracks', 'music/artists/top_tracks', { item_id: artist.item_id, provider_instance_id_or_domain: artist.provider });
}
const playlist = (await rec('library_playlists_one', 'music/playlists/library_items', { limit: 1 }))?.[0];
if (playlist) await rec('playlist_tracks', 'music/playlists/playlist_tracks', { item_id: playlist.item_id, provider_instance_id_or_domain: playlist.provider });

const q = queues?.find((x) => x.items > 0) ?? queues?.[0];
if (q) {
  await rec('queue_get', 'player_queues/get', { queue_id: q.queue_id });
  await rec('queue_items', 'player_queues/items', { queue_id: q.queue_id, limit: 10, offset: 0 });
}

console.log(`Listening for events ${eventSeconds}s - press play/pause/next, change shuffle/repeat in MA now...`);
await new Promise((r) => setTimeout(r, eventSeconds * 1000));
save('events', events.slice(0, 60));
ws.close();
process.exit(0);
