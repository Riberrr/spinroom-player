package io.github.riberrr.spinroom

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.viewModels
import io.github.riberrr.spinroom.feature.nowplaying.NowPlayingViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.riberrr.spinroom.core.audio.service.SpinroomPlaybackService
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.feature.nowplaying.ScreensaverLayer

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var inputTick by mutableIntStateOf(0)
    private var screensaver by mutableStateOf(false)
    private var swallowKeyUp = false
    private val shell: ShellViewModel by viewModels()
    private val nowPlaying: NowPlayingViewModel by viewModels()

    private val remote = RemoteKeyDispatcher(object : RemoteHandler {
        private val ui get() = nowPlaying.state.value
        override val radioPlaying get() = ui.hasTrack && ui.isRadio
        override fun openDrawer() = shell.requestDrawer()
        override fun nextTrack() = nowPlaying.onNext()
        override fun previousTrack() = nowPlaying.onPrevious()
        override fun nextStation() { if (!shell.stepStation(ui.mediaUri, ui.mediaName, true)) nowPlaying.onNext() }
        override fun previousStation() { if (!shell.stepStation(ui.mediaUri, ui.mediaName, false)) nowPlaying.onPrevious() }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpinroomPlaybackService.start(this) // the TV is a Sendspin player whenever the app has run
        setContent {
            SpinroomTheme {
                Box {
                    SpinroomNavHost()
                    ScreensaverLayer(inputTick, screensaver, onIdle = { screensaver = true })
                }
            }
        }
    }

    /** Any key restarts the idle timer; the key that wakes the screensaver is consumed (it only wakes). */
    @SuppressLint("RestrictedApi") // false positive: overriding androidx.core ComponentActivity.dispatchKeyEvent
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        inputTick++
        if (screensaver) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                screensaver = false
                swallowKeyUp = true
            }
            return true
        }
        if (swallowKeyUp && event.action == KeyEvent.ACTION_UP) {
            swallowKeyUp = false
            return true
        }
        if (remote.dispatch(KeyPress(event.keyCode, event.action == KeyEvent.ACTION_DOWN, event.repeatCount))) return true
        return super.dispatchKeyEvent(event)
    }
}
