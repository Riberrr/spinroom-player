package io.github.riberrr.spinroom

import io.github.riberrr.spinroom.core.data.KeyAction
import io.github.riberrr.spinroom.core.data.KeyAssignment
import io.github.riberrr.spinroom.core.data.KeyAssignments
import io.github.riberrr.spinroom.core.data.KeyRules

/** What a remote key can do in the app (mapped from the stored [KeyAction] by [AssignedKeyBindings]). */
enum class RemoteAction {
    OPEN_DRAWER, NEXT, PREVIOUS, PLAY_PAUSE, FAVORITE, OPEN_RADIO, OPEN_PLAYLISTS, OPEN_SEARCH,
    STATION_1, STATION_2, STATION_3, STATION_4, STATION_5, STATION_6, STATION_7, STATION_8, STATION_9;

    /** 1-9 for the STATION_n actions, else null. */
    val stationNumber: Int? get() = if (name.startsWith("STATION_")) name.removePrefix("STATION_").toInt() else null
}

/** A key press as the dispatcher sees it (plain ints so the logic is testable on the JVM). [repeatCount] 0 = first down. */
data class KeyPress(val keyCode: Int, val down: Boolean, val repeatCount: Int) {
    /** First auto-repeat of a held key (Android sends it ~500 ms after the press). */
    val isHold: Boolean get() = down && repeatCount == 1
}

/** Key -> action mapping. The default is [DefaultKeyBindings]; the user's own come from settings ([AssignedKeyBindings]). */
interface KeyBindings {
    /** Action for a key *hold* ([KeyPress.isHold]) or null. */
    fun onHold(keyCode: Int): RemoteAction?

    /** Action for a plain key *press* or null. */
    fun onPress(keyCode: Int): RemoteAction?
}

/** [KeyBindings] backed by the user's shortcut list (SUI8b). Scope ("w aplikacji"/"wszędzie") is the background service's concern (SUI9). */
class AssignedKeyBindings(private val assignments: KeyAssignments) : KeyBindings {
    override fun onHold(keyCode: Int) = assignments.find(keyCode, true)?.toRemote()
    override fun onPress(keyCode: Int) = assignments.find(keyCode, false)?.toRemote()
}

internal fun KeyAssignment.toRemote(): RemoteAction = when (action) {
    KeyAction.NEXT -> RemoteAction.NEXT
    KeyAction.PREVIOUS -> RemoteAction.PREVIOUS
    KeyAction.PLAY_PAUSE -> RemoteAction.PLAY_PAUSE
    KeyAction.OPEN_DRAWER -> RemoteAction.OPEN_DRAWER
    KeyAction.FAVORITE -> RemoteAction.FAVORITE
    KeyAction.OPEN_RADIO -> RemoteAction.OPEN_RADIO
    KeyAction.OPEN_PLAYLISTS -> RemoteAction.OPEN_PLAYLISTS
    KeyAction.OPEN_SEARCH -> RemoteAction.OPEN_SEARCH
    KeyAction.STATION -> RemoteAction.entries.first { it.stationNumber == arg }
}

/** Factory defaults (what settings start with). */
object DefaultKeyBindings : KeyBindings {
    const val KEYCODE_MEDIA_PLAY_PAUSE = KeyRules.KEYCODE_MEDIA_PLAY_PAUSE
    const val KEYCODE_CHANNEL_UP = KeyAssignments.KEYCODE_CHANNEL_UP
    const val KEYCODE_CHANNEL_DOWN = KeyAssignments.KEYCODE_CHANNEL_DOWN

    private val delegate = AssignedKeyBindings(KeyAssignments())
    override fun onHold(keyCode: Int) = delegate.onHold(keyCode)
    override fun onPress(keyCode: Int) = delegate.onPress(keyCode)
}

/** What the app does for each [RemoteAction]; radio vs music is decided here, not by the bindings. */
interface RemoteHandler {
    val radioPlaying: Boolean
    fun openDrawer()
    fun nextTrack()
    fun previousTrack()
    fun nextStation()
    fun previousStation()
    fun playPause()
    fun toggleFavorite()
    fun openRoute(route: String)

    /** Plays favorite station number [n] (1-9); nothing when there is no such favorite. */
    fun playFavoriteStation(n: Int)
}

/**
 * The one place that turns remote keys into app actions (called from `MainActivity.dispatchKeyEvent`).
 * Returns true when it consumed the event. A key that only has a hold action still passes its short press on
 * (Play/Pause, OK); the hold fires once and the matching key-up is swallowed. A key with both a short and a hold
 * action runs the short one on release, unless the key was held.
 */
class RemoteKeyDispatcher(
    private val handler: RemoteHandler,
    var bindings: KeyBindings = DefaultKeyBindings,
) {
    private var swallowUpFor: Int? = null
    private var pendingPress: Int? = null

    fun dispatch(press: KeyPress): Boolean {
        val code = press.keyCode
        if (!press.down) {
            if (swallowUpFor == code) {
                swallowUpFor = null
                return true
            }
            if (pendingPress == code) {
                pendingPress = null
                shortAction(code)?.let { run(it) }
                return true
            }
            return false
        }
        if (press.isHold) {
            bindings.onHold(code)?.let { action ->
                pendingPress = null
                swallowUpFor = code
                run(action)
                return true
            }
        }
        if (swallowUpFor == code) return true // further repeats of a hold that already fired
        if (press.repeatCount > 0) return shortAction(code) != null
        val action = shortAction(code) ?: return false
        if (bindings.onHold(code) != null) {
            pendingPress = code // decide on release: short press or the start of a hold
            return true
        }
        run(action)
        return true
    }

    /**
     * The short-press action for [code]. Play/Pause with only a hold shortcut (the default: hold = drawer) still toggles
     * playback on a short press, but *we* do it on release. Letting the down through would hand it to the system media
     * session, which pauses at once, so holding the key paused the music before the drawer opened.
     */
    private fun shortAction(code: Int): RemoteAction? =
        bindings.onPress(code)
            ?: RemoteAction.PLAY_PAUSE.takeIf { code == KeyRules.KEYCODE_MEDIA_PLAY_PAUSE && bindings.onHold(code) != null }

    private fun run(action: RemoteAction) {
        action.stationNumber?.let { return handler.playFavoriteStation(it) }
        when (action) {
            RemoteAction.OPEN_DRAWER -> handler.openDrawer()
            RemoteAction.NEXT -> if (handler.radioPlaying) handler.nextStation() else handler.nextTrack()
            RemoteAction.PREVIOUS -> if (handler.radioPlaying) handler.previousStation() else handler.previousTrack()
            RemoteAction.PLAY_PAUSE -> handler.playPause()
            RemoteAction.FAVORITE -> handler.toggleFavorite()
            RemoteAction.OPEN_RADIO -> handler.openRoute(Routes.RADIO)
            RemoteAction.OPEN_PLAYLISTS -> handler.openRoute(Routes.PLAYLISTS)
            RemoteAction.OPEN_SEARCH -> handler.openRoute(Routes.SEARCH)
            else -> Unit
        }
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
