package io.github.riberrr.spinroom

/** What a remote key can do. Kept small on purpose; SUI8b lets the user remap keys to these. */
enum class RemoteAction { OPEN_DRAWER, NEXT, PREVIOUS }

/** A key press as the dispatcher sees it (plain ints so the logic is testable on the JVM). [repeatCount] 0 = first down. */
data class KeyPress(val keyCode: Int, val down: Boolean, val repeatCount: Int) {
    /** First auto-repeat of a held key (Android sends it ~500 ms after the press). */
    val isHold: Boolean get() = down && repeatCount == 1
}

/** Key -> action mapping. The default is [DefaultKeyBindings]; settings (SUI8b) will provide their own. */
interface KeyBindings {
    /** Action for a key *hold* ([KeyPress.isHold]) or null. */
    fun onHold(keyCode: Int): RemoteAction?

    /** Action for a plain key *press* (fires on key down, repeats ignored) or null. */
    fun onPress(keyCode: Int): RemoteAction?
}

object DefaultKeyBindings : KeyBindings {
    // android.view.KeyEvent constants, inlined so the mapping needs no Android runtime.
    const val KEYCODE_MEDIA_PLAY_PAUSE = 85
    const val KEYCODE_CHANNEL_UP = 166
    const val KEYCODE_CHANNEL_DOWN = 167

    override fun onHold(keyCode: Int) = if (keyCode == KEYCODE_MEDIA_PLAY_PAUSE) RemoteAction.OPEN_DRAWER else null

    override fun onPress(keyCode: Int) = when (keyCode) {
        KEYCODE_CHANNEL_UP -> RemoteAction.NEXT
        KEYCODE_CHANNEL_DOWN -> RemoteAction.PREVIOUS
        else -> null
    }
}

/** What the app does for each [RemoteAction]; radio vs music is decided here, not by the bindings. */
interface RemoteHandler {
    val radioPlaying: Boolean
    fun openDrawer()
    fun nextTrack()
    fun previousTrack()
    fun nextStation()
    fun previousStation()
}

/**
 * The one place that turns remote keys into app actions (called from `MainActivity.dispatchKeyEvent`).
 * Returns true when it consumed the event. Short Play/Pause is never consumed (it goes on to the player as before);
 * a hold opens the drawer and the matching key-up is swallowed so releasing the key doesn't also toggle playback.
 */
class RemoteKeyDispatcher(
    private val handler: RemoteHandler,
    var bindings: KeyBindings = DefaultKeyBindings,
) {
    private var swallowUpFor: Int? = null

    fun dispatch(press: KeyPress): Boolean {
        if (!press.down) {
            if (swallowUpFor == press.keyCode) {
                swallowUpFor = null
                return true
            }
            return false
        }
        if (press.isHold) {
            bindings.onHold(press.keyCode)?.let { action ->
                swallowUpFor = press.keyCode
                run(action)
                return true
            }
        }
        if (swallowUpFor == press.keyCode) return true // further repeats of a hold that already fired
        if (press.repeatCount > 0) return bindings.onPress(press.keyCode) != null
        val action = bindings.onPress(press.keyCode) ?: return false
        run(action)
        return true
    }

    private fun run(action: RemoteAction) = when (action) {
        RemoteAction.OPEN_DRAWER -> handler.openDrawer()
        RemoteAction.NEXT -> if (handler.radioPlaying) handler.nextStation() else handler.nextTrack()
        RemoteAction.PREVIOUS -> if (handler.radioPlaying) handler.previousStation() else handler.previousTrack()
    }
}

/** Next/previous index in a list of [size] items, wrapping; [current] -1 (not found) goes to the first (next) or last (previous). */
fun cycleIndex(current: Int, size: Int, forward: Boolean): Int {
    if (size <= 0) return -1
    return when {
        current < 0 -> if (forward) 0 else size - 1
        forward -> (current + 1) % size
        else -> (current - 1 + size) % size
    }
}
