# :core:audio

Plays timestamped Sendspin audio (S04): decode → timed buffer → AudioTrack, with device delay compensation.
Plus the playback service (S05, below). Its build.gradle.kts is the template for new library modules (drop the media3 lines).

## Public API
```kotlin
val engine: AudioEngine = SpinroomAudioEngine(clock /* ClockSource from S02 */)
playerStream.events.collect(engine::handle)            // PlayerEvent -> start/submit/clear/stop/commands
engine.playerState.collect(reporter::report)            // client/state player object
val caps: PlayerCapabilities = AudioCapabilities.probe() // client/hello player@v1_support
```
- `AudioEngine.start(format)`, `submit(AudioChunk)`, `clear()`, `stop()`, `release()` — non-blocking, run in call order.
- `setVolume(0..100)` (amplitude `(v/100)^1.5`), `setMuted`, `setOutputDelayMs(0..5000)` (S14c calibration).
- `state: StateFlow<AudioEngineState>` — `Idle`, `Buffering(format)`, `Playing(format)`, `Error(msg)`.
- `stats: StateFlow<AudioStats>` — buffered ms, output latency, sync error µs, dropped/corrected frames, resyncs, underruns.
- `playerState: StateFlow<PlayerState>` — volume, muted, output delay, required lead time, min buffer.

## How it works
- `decode/`: FLAC and Opus via synchronous `MediaCodec` (FLAC chunks split into single frames by `FlacFrames`, CRC-checked;
  Opus gets a generated `OpusHead`); PCM converted in Kotlin. Output PTS = server time of each frame.
- `buffer/`: `TimedPcmBuffer` (PCM16 blocks in server time) and `PlaybackScheduler` (pure): silence until due, drop late
  frames, then ±1 frame per 200 (≤0.5 %) to keep the error inside 100 µs; >10 ms error → realign.
- `output/`: `AudioTrackOutput` (150 ms buffer, blocking writes, 10 ms per fill) + `OutputClockEstimator`
  (uses `getTimestamp` only when it agrees with `playbackHeadPosition`, otherwise a min-filtered head clock).
- Threads: `Spinroom-decode` (all calls, in order), `Spinroom-audio` (urgent audio priority).

## Debug screen (debug builds only)
`adb shell am start -n io.github.riberrr.spinroom.debug/io.github.riberrr.spinroom.core.audio.debug.AudioDebugActivity`
Plays `src/debug/assets/spinroom-test-tone.flac` (6 s, 440/554 Hz) with generated timestamps, looped; buttons for
clear, stop, volume, mute, output delay; live stats. The asset is written by `FlacFramesTest.writeDebugAsset`
(to `build/test-asset/`).

## Pitfalls
- `ClockSource.localNowUs()` must use the `System.nanoTime()` base (AudioTrack timestamps do).
- Platform FLAC decoder exists only from API 27; on API 26 `AudioCapabilities` offers Opus + PCM only.
- The platform FLAC decoder goes silent after a frame-number jump; `MediaCodecDecoder` flushes the codec when it sees one.
- A format change with a different sample rate/channels recreates the AudioTrack (short cut of ~150 ms of old audio).

## Playback service (`service/`, S05)
The TV as a Sendspin player, running as a foreground `mediaPlayback` service from app start until the process dies.
```kotlin
class App : Application(), PlaybackHost { ... }      // credentials, version, notification icon, onSendspinConnected
SpinroomPlaybackService.start(activity)               // from a visible activity (FGS start rules)
```
- `SpinroomPlaybackService` (`MediaSessionService`): owns `SendspinReceiver`, a Media3 `MediaSession` and its **own**
  notification (Media3's drops the foreground while idle; a receiver must stay reachable). `onTaskRemoved` keeps running.
  Notification buttons send `io.github.riberrr.spinroom.action.PLAY|PAUSE|NEXT|PREVIOUS` to the service.
- `SendspinReceiver`: `SendspinSession` + `SpinroomAudioEngine` + `SendspinNetwork.startServer(deviceName)`;
  `events → engine.handle`, `engine.playerState → report`, `setAvailable(true)`; Wi-Fi lock (high-perf, + low-latency on
  API 29+) while running, partial wake lock while audio plays; restarts server + mDNS when the default network changes.
  `snapshot: StateFlow<PlaybackSnapshot>`, `send(ControllerCommand)`, `serverNowUs()`. `start`/`stop` block (off main).
- `SendspinMediaPlayer` (`SimpleBasePlayer`): one item = current track (title/artist/album/cover, extrapolated position);
  play/pause → `Play`/`Pause`, next/previous → `Next`/`Previous` (also with a single item: `BasePlayer` routes them to
  `handleSeek`), seek → `Seek`. Commands offered = server's `supported_commands`.
- `PlaybackMapper`: the pure mapping (JVM tests in `PlaybackMapperTest`).

Pitfalls: Android sends media keys to the app that last played audio — our session takes over only once our
AudioTrack plays. Start the service only from the foreground (Android 12+ forbids FGS starts from the background).
