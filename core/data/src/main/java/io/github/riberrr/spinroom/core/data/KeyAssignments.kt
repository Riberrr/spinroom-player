package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** What a remote key can do (SUI8b). [STATION] carries the number 1-9 in [KeyAssignment.arg]. Names are stored: don't rename. */
enum class KeyAction { NEXT, PREVIOUS, PLAY_PAUSE, OPEN_DRAWER, FAVORITE, OPEN_RADIO, OPEN_PLAYLISTS, OPEN_SEARCH, STATION }

val STATION_RANGE = 1..9

/** One shortcut: [keyCode] (android.view.KeyEvent code), short press or [long] hold, the action, and its scope. */
data class KeyAssignment(
    val keyCode: Int,
    val long: Boolean,
    val action: KeyAction,
    val arg: Int = 0,
    /** true = "wszędzie" (also outside the app; the service comes with SUI9), false = "w aplikacji". */
    val global: Boolean = false,
) {
    /** Two assignments collide when they sit on the same physical gesture. */
    fun sameGesture(other: KeyAssignment) = keyCode == other.keyCode && long == other.long

    internal fun encode() = "$keyCode:${if (long) "L" else "S"}:${action.name}:$arg:${if (global) "G" else "A"}"

    companion object {
        internal fun decode(raw: String): KeyAssignment? {
            val p = raw.split(":")
            if (p.size != 5) return null
            val code = p[0].toIntOrNull() ?: return null
            val action = KeyAction.entries.firstOrNull { it.name == p[2] } ?: return null
            val arg = p[3].toIntOrNull() ?: return null
            if (action == KeyAction.STATION && arg !in STATION_RANGE) return null
            val long = when (p[1]) {
                "L" -> true
                "S" -> false
                else -> return null
            }
            if (KeyRules.classify(code, long) == KeyRules.Verdict.Blocked) return null
            return KeyAssignment(code, long, action, if (action == KeyAction.STATION) arg else 0, p[4] == "G")
        }
    }
}

/** Which keys may be assigned. Navigation, Home, Back and volume are never overridable; some others get a warning. */
object KeyRules {
    // android.view.KeyEvent constants, inlined so this stays JVM-testable.
    const val KEYCODE_HOME = 3
    const val KEYCODE_BACK = 4
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val KEYCODE_POWER = 26
    const val KEYCODE_ENTER = 66
    const val KEYCODE_NUMPAD_ENTER = 160
    const val KEYCODE_VOLUME_MUTE = 164
    const val KEYCODE_APP_SWITCH = 187
    const val KEYCODE_MEDIA_PLAY_PAUSE = 85

    /** Keys that must keep working: always blocked. */
    private val systemKeys = setOf(
        KEYCODE_HOME, KEYCODE_BACK, KEYCODE_POWER, KEYCODE_VOLUME_UP, KEYCODE_VOLUME_DOWN, KEYCODE_VOLUME_MUTE, KEYCODE_APP_SWITCH,
    )

    /** D-pad moves: blocked. */
    private val dpadMoves = setOf(KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT)

    /** OK/Enter: blocked for a short press, only a hold may carry a shortcut. */
    private val dpadOk = setOf(KEYCODE_DPAD_CENTER, KEYCODE_ENTER, KEYCODE_NUMPAD_ENTER)

    /** MENU 82, SEARCH 84, PLAY_PAUSE 85, PLAY 126, PAUSE 127, INFO 165, TV 170, SETTINGS 176, TV_INPUT 178, GUIDE 172, ASSIST 219. */
    private val tvFunctionKeys = setOf(82, 84, 85, 126, 127, 165, 170, 172, 176, 178, 219)

    enum class Verdict { Ok, Warn, Blocked }

    fun classify(keyCode: Int, long: Boolean): Verdict = when {
        keyCode in systemKeys || keyCode in dpadMoves -> Verdict.Blocked
        keyCode in dpadOk -> if (long) Verdict.Ok else Verdict.Blocked
        // A hold of Play/Pause is the drawer's default; only its short press has another job.
        keyCode in tvFunctionKeys -> if (long && keyCode == KEYCODE_MEDIA_PLAY_PAUSE) Verdict.Ok else Verdict.Warn
        else -> Verdict.Ok
    }
}

/** The whole shortcut list; at most one assignment per gesture. */
data class KeyAssignments(val items: List<KeyAssignment> = DEFAULTS) {
    /** Adds [a]; an existing one on the same gesture is replaced. Blocked keys are ignored. */
    fun with(a: KeyAssignment): KeyAssignments {
        if (KeyRules.classify(a.keyCode, a.long) == KeyRules.Verdict.Blocked) return this
        return KeyAssignments(items.filterNot { it.sameGesture(a) } + a)
    }

    fun without(a: KeyAssignment) = KeyAssignments(items - a)

    /** The assignment [a] would push out, or null. */
    fun collidingWith(a: KeyAssignment): KeyAssignment? = items.firstOrNull { it.sameGesture(a) }

    fun find(keyCode: Int, long: Boolean): KeyAssignment? = items.firstOrNull { it.keyCode == keyCode && it.long == long }

    internal fun encode() = items.joinToString(";") { it.encode() }

    companion object {
        const val KEYCODE_CHANNEL_UP = 166
        const val KEYCODE_CHANNEL_DOWN = 167
        const val KEYCODE_PROG_RED = 183

        val DEFAULTS = listOf(
            KeyAssignment(KEYCODE_CHANNEL_UP, false, KeyAction.NEXT, global = true),
            KeyAssignment(KEYCODE_CHANNEL_DOWN, false, KeyAction.PREVIOUS, global = true),
            KeyAssignment(KeyRules.KEYCODE_MEDIA_PLAY_PAUSE, true, KeyAction.OPEN_DRAWER),
            KeyAssignment(KeyRules.KEYCODE_DPAD_CENTER, true, KeyAction.FAVORITE),
            KeyAssignment(KEYCODE_PROG_RED, false, KeyAction.OPEN_RADIO),
        )

        internal const val EMPTY = "-"

        /** Tolerant: broken or blocked entries are dropped, later duplicates of a gesture win. null = defaults; "-" = empty list. */
        internal fun decode(raw: String?): KeyAssignments {
            if (raw == null) return KeyAssignments()
            if (raw == EMPTY) return KeyAssignments(emptyList())
            return raw.split(";").mapNotNull { KeyAssignment.decode(it) }
                .fold(KeyAssignments(emptyList())) { acc, a -> acc.with(a) }
        }
    }
}

/** Shortcuts + the "Działaj poza aplikacją" preference (the service that honours it is SUI9). */
class KeyBindingsRepository(private val store: DataStore<Preferences>) {
    val assignments: Flow<KeyAssignments> = store.data.map { KeyAssignments.decode(it[KEY]) }.distinctUntilChanged()
    val globalEnabled: Flow<Boolean> = store.data.map { it[GLOBAL] ?: false }.distinctUntilChanged()

    suspend fun update(transform: (KeyAssignments) -> KeyAssignments) {
        store.edit {
            val next = transform(KeyAssignments.decode(it[KEY]))
            it[KEY] = if (next.items.isEmpty()) KeyAssignments.EMPTY else next.encode()
        }
    }

    suspend fun reset() { store.edit { it.remove(KEY) } }
    suspend fun setGlobalEnabled(on: Boolean) { store.edit { it[GLOBAL] = on } }

    private companion object {
        val KEY = stringPreferencesKey("remote_key_assignments")
        val GLOBAL = booleanPreferencesKey("remote_global_enabled")
    }
}

/** While the "Naciśnij przycisk…" overlay is up the activity must not run the existing shortcuts (the key is being learned). */
class KeyCaptureGate {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active
    fun set(on: Boolean) { _active.value = on }
}
