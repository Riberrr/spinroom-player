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
import dagger.hilt.android.AndroidEntryPoint
import io.github.riberrr.spinroom.core.audio.service.SpinroomPlaybackService
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.feature.nowplaying.ScreensaverLayer

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var inputTick by mutableIntStateOf(0)
    private var screensaver by mutableStateOf(false)
    private var swallowKeyUp = false

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
        return super.dispatchKeyEvent(event)
    }
}
