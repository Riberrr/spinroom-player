package io.github.riberrr.spinroom

import io.github.riberrr.spinroom.core.data.KeyAssignments
import io.github.riberrr.spinroom.core.data.KeyRules

/** Decides which remote keys the accessibility service grabs outside the app (SUI9). Pure, so it runs on the JVM. */
object GlobalKeyPolicy {
    /** Only transport-like actions make sense on another screen; "open Radio" etc. need the app in front. */
    private val globalActions = setOf(RemoteAction.NEXT, RemoteAction.PREVIOUS, RemoteAction.PLAY_PAUSE)

    /**
     * The action to run for [keyCode], or null = let the key through untouched. Intercepts only when the user enabled
     * "Działaj poza aplikacją", something plays or is paused, Spinroom is not in front (it handles its own keys),
     * and the key has a short-press shortcut with scope "wszędzie". A key that also carries a hold shortcut is left
     * alone: telling tap from hold would mean delaying the key for every other app.
     */
    fun interceptedAction(
        keyCode: Int,
        assignments: KeyAssignments,
        enabled: Boolean,
        playbackActive: Boolean,
        appInForeground: Boolean,
    ): RemoteAction? {
        if (!enabled || !playbackActive || appInForeground) return null
        if (KeyRules.classify(keyCode, false) == KeyRules.Verdict.Blocked) return null
        if (assignments.find(keyCode, true) != null) return null
        val assignment = assignments.find(keyCode, false)?.takeIf { it.global } ?: return null
        val action = assignment.toRemote()
        return action.takeIf { it in globalActions || it.stationNumber != null }
    }
}

/**
 * Down/up bookkeeping for [GlobalKeyPolicy]: a key whose down was grabbed has its repeats and its up swallowed too,
 * otherwise the foreground app would see a lone key-up.
 */
class GlobalKeyFilter(private val lookup: (keyCode: Int) -> RemoteAction?) {
    /** [consume] = swallow this event; [run] = start this action (only on the first down). */
    data class Result(val consume: Boolean, val run: RemoteAction? = null)

    private val grabbed = mutableSetOf<Int>()

    fun onKey(keyCode: Int, down: Boolean, repeatCount: Int): Result {
        if (!down) return Result(grabbed.remove(keyCode))
        if (repeatCount > 0) return Result(keyCode in grabbed)
        val action = lookup(keyCode) ?: return Result(false)
        grabbed += keyCode
        return Result(true, action)
    }
}
