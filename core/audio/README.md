# :core:audio

Plays timestamped Sendspin audio (S04): decode → timed buffer → AudioTrack, with device delay compensation.
MediaSessionService comes in S05. Its build.gradle.kts is the template for new library modules.

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
