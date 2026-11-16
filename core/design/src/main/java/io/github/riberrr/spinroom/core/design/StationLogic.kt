package io.github.riberrr.spinroom.core.design

import androidx.compose.ui.graphics.Color

/** Stable hue (0..360) derived from a station name; same name gives the same color on every screen. */
fun stationHue(name: String): Float {
    var h = 0
    for (c in name.trim().lowercase()) h = h * 31 + c.code
    return (((h % 360) + 360) % 360).toFloat()
}

/** Tile background for a station without a logo: fixed saturation/value so white initials stay readable. */
fun stationColor(name: String): Color = Color.hsv(stationHue(name), 0.45f, 0.45f)

/** Up to two initials: first letters of the first two words, else first two letters. Blank gives "?". */
fun stationInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].filter { it.isLetterOrDigit() }.take(2)
        else -> words.take(2).mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() } }.joinToString("")
    }
    return letters.uppercase().ifEmpty { "?" }
}
