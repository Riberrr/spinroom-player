package io.github.riberrr.spinroom

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import io.github.riberrr.spinroom.core.data.Appearance
import io.github.riberrr.spinroom.core.data.AppearanceSettings
import io.github.riberrr.spinroom.core.data.StationNumberHub
import javax.inject.Inject
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
    @Inject lateinit var appearanceSettings: AppearanceSettings
    @Inject lateinit var playerNameStore: io.github.riberrr.spinroom.core.data.PlayerNameStore
    @Inject lateinit var keyBindingsRepository: io.github.riberrr.spinroom.core.data.KeyBindingsRepository
    @Inject lateinit var keyCaptureGate: io.github.riberrr.spinroom.core.data.KeyCaptureGate
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
        override fun playPause() = nowPlaying.onPlayPause()
        override fun toggleFavorite() = nowPlaying.onFavorite()
        override fun openRoute(route: String) = shell.requestRoute(route)
        override fun playFavoriteStation(n: Int) { shell.playStationNumber(n) }
        override fun canAssignStationNumber() = StationNumberHub.canAssign
        override fun assignStationNumber(n: Int) = StationNumberHub.assign(n)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpinroomPlaybackService.start(this) // the TV is a Sendspin player whenever the app has run
        lifecycleScope.launch {
            // A new TV name takes effect after a player restart (the first value is the one already in use).
            playerNameStore.name.drop(1).collect { SpinroomPlaybackService.restart(this@MainActivity) }
        }
        lifecycleScope.launch {
            keyBindingsRepository.assignments.collect { remote.bindings = AssignedKeyBindings(it) }
        }
        setContent {
            val look by appearanceSettings.appearance.collectAsState(initial = Appearance())
            SpinroomTheme(theme = look.theme, accent = look.accent) {
                Box {
                    SpinroomNavHost()
                    ScreensaverLayer(inputTick, screensaver, onIdle = { if (look.screensaver) screensaver = true })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppForeground.visible = true
    }

    override fun onStop() {
        AppForeground.visible = false
        super.onStop()
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
        // While settings learns a new key, the existing shortcuts stay quiet so the key reaches the capture overlay.
        if (!keyCaptureGate.active.value && remote.dispatch(KeyPress(event.keyCode, event.action == KeyEvent.ACTION_DOWN, event.repeatCount))) return true
        return super.dispatchKeyEvent(event)
    }
}
