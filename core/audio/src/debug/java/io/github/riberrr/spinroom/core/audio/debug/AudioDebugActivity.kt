package io.github.riberrr.spinroom.core.audio.debug

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.riberrr.spinroom.core.audio.AudioCapabilities
import io.github.riberrr.spinroom.core.audio.SpinroomAudioEngine
import io.github.riberrr.spinroom.core.audio.decode.FlacFrames
import io.github.riberrr.spinroom.core.sendspin.api.AudioChunk
import io.github.riberrr.spinroom.core.sendspin.api.ClockSource
import io.github.riberrr.spinroom.core.sendspin.api.Codec
import io.github.riberrr.spinroom.core.sendspin.api.StreamFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Server clock = local clock; good enough to exercise the engine without a server. */
private object LocalClock : ClockSource {
    override fun localNowUs() = System.nanoTime() / 1000
    override fun serverToLocalUs(serverUs: Long) = serverUs
    override fun localToServerUs(localUs: Long) = localUs
    override val converged: StateFlow<Boolean> = MutableStateFlow(true)
}

/**
 * Debug-only screen: plays assets/spinroom-test-tone.flac through [SpinroomAudioEngine] as if a server
 * streamed it (3 FLAC frames per chunk, timestamps generated, sent up to 1.5 s ahead, looped).
 */
class AudioDebugActivity : Activity() {
    private val engine = SpinroomAudioEngine(LocalClock)
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private val decoders by lazy {
        "FLAC: ${AudioCapabilities.hasDecoder(Codec.FLAC)}, Opus: ${AudioCapabilities.hasDecoder(Codec.OPUS)}"
    }

    @Volatile private var feeder: Thread? = null
    @Volatile private var restartTimeline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (32 * resources.displayMetrics.density).toInt()
        status = TextView(this).apply { textSize = 18f }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val row1 = LinearLayout(this).apply { gravity = Gravity.START }
        val row2 = LinearLayout(this).apply { gravity = Gravity.START }
        fun button(row: LinearLayout, text: Int, action: () -> Unit) =
            row.addView(
                Button(this).apply {
                    setText(text)
                    setOnClickListener { action() }
                    // Visible D-pad focus: accent tint on the focused button.
                    backgroundTintList = android.content.res.ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                        intArrayOf(getColor(io.github.riberrr.spinroom.core.audio.R.color.audio_debug_focus), 0xFF555555.toInt()),
                    )
                },
            )
        button(row1, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_play) { play() }
        button(row1, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_stop) { stopFeeding() }
        button(row1, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_clear) { restartTimeline = true }
        button(row2, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_volume_down) {
            engine.setVolume(engine.playerState.value.volume - 10)
        }
        button(row2, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_volume_up) {
            engine.setVolume(engine.playerState.value.volume + 10)
        }
        button(row2, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_mute) {
            engine.setMuted(!engine.playerState.value.muted)
        }
        button(row2, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_delay_down) {
            engine.setOutputDelayMs(engine.playerState.value.outputDelayMs - 10)
        }
        button(row2, io.github.riberrr.spinroom.core.audio.R.string.audio_debug_delay_up) {
            engine.setOutputDelayMs(engine.playerState.value.outputDelayMs + 10)
        }
        col.addView(row1)
        col.addView(row2)
        col.addView(status)
        setContentView(col)
        row1.getChildAt(0).requestFocus()
        ui.post(refresh)
    }

    private val refresh = object : Runnable {
        override fun run() {
            val p = engine.playerState.value
            status.text = buildString {
                appendLine("Stan: ${engine.state.value}")
                appendLine(decoders)
                appendLine("Głośność ${p.volume}${if (p.muted) " (wyciszone)" else ""}, opóźnienie ${p.outputDelayMs} ms, lead ${p.requiredLeadTimeMs} ms")
                append(engine.stats.value.toString())
            }
            ui.postDelayed(this, 250)
        }
    }

    private fun play() {
        if (feeder != null) return
        val t = Thread({ feed() }, "Spinroom-debug-feed")
        feeder = t
        t.start()
    }

    private fun stopFeeding() {
        feeder?.interrupt()
        feeder = null
        engine.stop()
    }

    private fun feed() {
        val file = assets.open(ASSET).use { it.readBytes() }
        val headerLen = flacHeaderLength(file)
        val header = file.copyOfRange(0, headerLen)
        val body = file.copyOfRange(headerLen, file.size)
        val frames = FlacFrames.split(body)
        val format = StreamFormat(Codec.FLAC, 48_000, 2, 16, codecHeader = header)
        engine.start(format)
        var start = LocalClock.localNowUs() + START_DELAY_US
        var samples = 0L
        try {
            while (feeder === Thread.currentThread()) {
                Log.d("Spinroom", "debug feeder pass, ${frames.size} frames, samples=$samples")
                for (group in frames.chunked(FRAMES_PER_CHUNK)) {
                    if (restartTimeline) {
                        restartTimeline = false
                        engine.clear()
                        start = LocalClock.localNowUs() + START_DELAY_US
                        samples = 0
                    }
                    val ts = start + samples * 1_000_000L / format.sampleRate
                    while (ts > LocalClock.localNowUs() + SEND_AHEAD_US) Thread.sleep(20)
                    val from = group.first().offset
                    val to = group.last().let { it.offset + it.length }
                    engine.submit(
                        AudioChunk(ts, ts - LocalClock.localNowUs(), body.copyOfRange(from, to), LocalClock.localNowUs(), format),
                    )
                    samples += group.sumOf { it.blockSize }
                }
            }
        } catch (e: Throwable) {
            Log.d("Spinroom", "debug feeder stopped: $e")
        }
    }

    /** Length of `fLaC` + metadata blocks, i.e. what Sendspin sends as codec_header. */
    private fun flacHeaderLength(f: ByteArray): Int {
        var p = 4
        while (true) {
            val last = f[p].toInt() and 0x80 != 0
            val len = (f[p + 1].toInt() and 0xFF shl 16) or (f[p + 2].toInt() and 0xFF shl 8) or (f[p + 3].toInt() and 0xFF)
            p += 4 + len
            if (last) return p
        }
    }

    override fun onDestroy() {
        ui.removeCallbacks(refresh)
        feeder?.interrupt()
        feeder = null
        engine.release()
        super.onDestroy()
    }

    private companion object {
        const val ASSET = "spinroom-test-tone.flac"
        const val FRAMES_PER_CHUNK = 3
        const val START_DELAY_US = 300_000L
        const val SEND_AHEAD_US = 1_500_000L
    }
}
