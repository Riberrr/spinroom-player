package io.github.riberrr.spinroom.core.design

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Backdrop debounce: the first value passes immediately, later ones only after [windowMs] without a newer value,
 * so fast D-pad scrolling doesn't trigger a crossfade (and an image load) for every tile.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.settleAfterFirst(windowMs: Long): Flow<T> = flow {
    var first = true
    emitAll(
        transformLatest { v ->
            if (!first) delay(windowMs)
            first = false
            emit(v)
        },
    )
}

/** Backdrop dimming (share of black over the blurred image). */
const val BACKDROP_DIM = 0.6f

/** Edge of the tiny bitmap the backdrop is blurred from. */
const val BACKDROP_BITMAP_PX = 32

/** Box blur on a square ARGB int array ([size]x[size]), [radius] px, [passes] times. Pure, for tests. */
fun boxBlur(pixels: IntArray, size: Int, radius: Int, passes: Int = 2): IntArray {
    var src = pixels.copyOf()
    repeat(passes) {
        src = blurAxis(blurAxis(src, size, radius, horizontal = true), size, radius, horizontal = false)
    }
    return src
}

private fun blurAxis(src: IntArray, size: Int, radius: Int, horizontal: Boolean): IntArray {
    val out = IntArray(src.size)
    for (line in 0 until size) {
        for (i in 0 until size) {
            var a = 0; var r = 0; var g = 0; var b = 0; var n = 0
            for (k in -radius..radius) {
                val j = (i + k).coerceIn(0, size - 1)
                val p = if (horizontal) src[line * size + j] else src[j * size + line]
                a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
            }
            val v = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            if (horizontal) out[line * size + i] = v else out[i * size + line] = v
        }
    }
    return out
}
