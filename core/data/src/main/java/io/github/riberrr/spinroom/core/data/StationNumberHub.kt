package io.github.riberrr.spinroom.core.data

/**
 * Bridge between the global remote-key dispatcher (`:app`) and the Radio screen: while a favorite station tile is focused
 * the screen publishes it here, and a held digit key is handed to [onAssign] instead of dialling that number.
 * Main thread only.
 */
object StationNumberHub {
    /** The focused favorite on the Radio screen, null when focus is elsewhere. */
    var focused: Station? = null

    /** Set by the Radio ViewModel: (station, number) -> handled. */
    var onAssign: ((Station, Int) -> Unit)? = null

    val canAssign: Boolean get() = focused != null && onAssign != null

    /** Returns true when the hold was taken (a favorite is focused). */
    fun assign(number: Int): Boolean {
        val station = focused ?: return false
        val handler = onAssign ?: return false
        handler(station, number)
        return true
    }
}
