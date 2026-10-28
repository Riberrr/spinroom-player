package io.github.riberrr.spinroom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import io.github.riberrr.spinroom.core.audio.service.SpinroomPlaybackService
import io.github.riberrr.spinroom.core.data.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** After the TV boots, start the player service (not the activity) so the TV is visible in MA. Honours the settings switch. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val enabled = runBlocking { settings.settings.first().autostart }
        if (!enabled) {
            Log.i("Spinroom", "boot: autostart off")
            return
        }
        try {
            SpinroomPlaybackService.start(context)
            Log.i("Spinroom", "boot: player service started")
        } catch (e: Exception) {
            // Android 15+ forbids starting a mediaPlayback foreground service from BOOT_COMPLETED.
            Log.w("Spinroom", "boot: cannot start player service (${e.javaClass.simpleName})")
        }
    }
}
