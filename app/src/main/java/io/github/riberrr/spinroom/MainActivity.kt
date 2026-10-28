package io.github.riberrr.spinroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.riberrr.spinroom.core.audio.service.SpinroomPlaybackService
import io.github.riberrr.spinroom.core.design.SpinroomTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpinroomPlaybackService.start(this) // the TV is a Sendspin player whenever the app has run
        setContent { SpinroomTheme { SpinroomNavHost() } }
    }
}
