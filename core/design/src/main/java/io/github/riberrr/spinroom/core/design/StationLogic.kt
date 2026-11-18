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

/** Muted dark color pairs (start, end) used for generated gradients; picked by a stable hash so a name always maps to the same pair. */
private val GRADIENT_PALETTE: List<Pair<Long, Long>> = listOf(
    0xFF5B2A3A to 0xFF2A1B3F, // plum
    0xFF1F4A5C to 0xFF1B2B52, // deep sea
    0xFF2F5A47 to 0xFF1C3B4A, // pine
    0xFF6A4A1E to 0xFF40222F, // amber dusk
    0xFF3B3470 to 0xFF1D2A55, // indigo
    0xFF2E5A62 to 0xFF2A3F2E, // teal moss
    0xFF6B3347 to 0xFF3A2250, // berry
    0xFF2C4A78 to 0xFF2A2760, // cobalt
)

/** Stable non-negative hash of a trimmed, lowercased key (same algorithm family as [stationHue]). */
fun stableHash(key: String): Int {
    var h = 7
    for (c in key.trim().lowercase()) h = h * 31 + c.code
    return h and Int.MAX_VALUE
}

/** Index into the gradient palette for [key]; deterministic across runs and renders. */
fun gradientIndex(key: String): Int = stableHash(key) % GRADIENT_PALETTE.size

/** Two dark, muted colors for a diagonal gradient (tiles, backdrop) derived from [key]. */
fun gradientColors(key: String): Pair<Color, Color> {
    val (a, b) = GRADIENT_PALETTE[gradientIndex(key)]
    return Color(a) to Color(b)
}

/** Up to [max] letters for a big watermark acronym: first letters of the words, else the first letters of a single word. */
fun stationAcronym(name: String, max: Int = 3): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].filter { it.isLetterOrDigit() }.take(max)
        else -> words.take(max).mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() } }.joinToString("")
    }
    return letters.uppercase().ifEmpty { "?" }
}

/** Backdrop gradient for an item without a cover: from its seed color (itself derived from the name) or a fixed default. */
fun backdropGradient(seed: Color?): Pair<Color, Color> = gradientColors(seed?.value?.toString() ?: "spinroom")
