package io.github.riberrr.spinroom.core.data

/** A recognised remote gesture: which key, and whether it was held. */
data class KeyGesture(val keyCode: Int, val long: Boolean)

/**
 * Turns raw key events into gestures for the "Naciśnij przycisk na pilocie…" step: a hold (Android's first auto-repeat,
 * repeatCount 1) reports at once as long; a tap reports on release. One report per physical press.
 */
class KeyGestureDetector {
    private var down: Int? = null
    private var held = false

    /** Feed every key event; returns the gesture once it is known, else null. */
    fun onKey(keyCode: Int, isDown: Boolean, repeatCount: Int): KeyGesture? {
        if (isDown) {
            if (repeatCount == 0 || down != keyCode) {
                down = keyCode
                held = false
                return null
            }
            if (!held && repeatCount >= 1) {
                held = true
                return KeyGesture(keyCode, long = true)
            }
            return null
        }
        val wasTap = down == keyCode && !held
        if (down == keyCode) {
            down = null
            held = false
        }
        return if (wasTap) KeyGesture(keyCode, long = false) else null
    }
}
