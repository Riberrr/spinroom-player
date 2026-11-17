package io.github.riberrr.spinroom

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.riberrr.spinroom.core.audio.service.PlaybackActivity
import io.github.riberrr.spinroom.core.data.KeyAssignments
import io.github.riberrr.spinroom.core.data.KeyBindingsRepository
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Grabs the remote keys the user assigned "wszędzie" while something plays in Spinroom and another app is in front (SUI9).
 * It reads no window content (`canRetrieveWindowContent=false`, no events subscribed): it only filters key events,
 * and [GlobalKeyPolicy] lets every other key through unchanged.
 */
@AndroidEntryPoint
class GlobalKeysService : AccessibilityService() {
    @Inject lateinit var controller: GlobalKeyController
    @Inject lateinit var keyBindings: KeyBindingsRepository

    private val scope = MainScope()
    private lateinit var card: CornerCard

    @Volatile private var assignments = KeyAssignments()
    @Volatile private var enabled = false

    private val filter = GlobalKeyFilter { code ->
        GlobalKeyPolicy.interceptedAction(code, assignments, enabled, PlaybackActivity.active.value, AppForeground.visible)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        card = CornerCard(this)
        scope.launch { keyBindings.assignments.collect { assignments = it } }
        scope.launch { keyBindings.globalEnabled.collect { enabled = it } }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val result = filter.onKey(event.keyCode, event.action == KeyEvent.ACTION_DOWN, event.repeatCount)
        result.run?.let { action ->
            scope.launch {
                val info = controller.run(action) ?: return@launch
                showCard(event.keyCode, info)
            }
        }
        return result.consume
    }

    private fun showCard(keyCode: Int, info: CardInfo) {
        val key = when (keyCode) {
            KeyAssignments.KEYCODE_CHANNEL_UP -> getString(R.string.global_key_ch_up)
            KeyAssignments.KEYCODE_CHANNEL_DOWN -> getString(R.string.global_key_ch_down)
            else -> getString(R.string.global_key_other)
        }
        val fallbackTitle = CornerCard.nowText().ifBlank { getString(R.string.app_name) }
        val (caption, title) = when (info.kind) {
            CardInfo.Kind.STATION -> getString(R.string.global_card_station, info.stationNumber, info.stationCount) to info.stationName
            CardInfo.Kind.NEXT -> getString(R.string.global_card_next) to fallbackTitle
            CardInfo.Kind.PREVIOUS -> getString(R.string.global_card_previous) to fallbackTitle
            CardInfo.Kind.PAUSED -> getString(R.string.global_card_paused) to getString(R.string.app_name)
            CardInfo.Kind.RESUMED -> getString(R.string.global_card_resumed) to getString(R.string.app_name)
        }
        val image = info.imageUrl ?: PlaybackActivity.now.value?.artworkUrl.takeIf { info.kind != CardInfo.Kind.STATION }
        card.show(key, caption, title, image, CornerCard::nowText)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::card.isInitialized) card.dismiss()
        scope.cancel()
        super.onDestroy()
    }
}
