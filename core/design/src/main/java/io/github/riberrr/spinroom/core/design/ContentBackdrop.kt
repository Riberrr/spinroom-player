package io.github.riberrr.spinroom.core.design

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Size
import coil3.transform.Transformation

private const val CROSSFADE_MS = 300
private const val SETTLE_MS = 250L

/** What the backdrop shows: a cover/logo URL, or just a seed color (e.g. station without logo). */
private data class BackdropSource(val imageUrl: String?, val seed: Color?)

/**
 * Full-screen blurred, ~60 % dimmed version of the focused item's cover (or a seed color), crossfaded in ~300 ms.
 * Changes are debounced so fast scrolling doesn't flicker. With `solidBackground` (default: [LocalSolidBackground]) it is the plain theme background.
 *
 * Blur: Coil downsizes the image to a 32 px bitmap and blurs it once ([TinyBlurTransformation]); Coil caches the result per URL.
 * The tiny bitmap is stretched with bilinear filtering. No `Modifier.blur` (API 31+).
 *
 * Usage: `Box { ContentBackdrop(imageUrl = focusedCoverUrl); /* screen content */ }`.
 */
@Composable
fun ContentBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    seedColor: Color? = null,
    solidBackground: Boolean = LocalSolidBackground.current,
) {
    val bg = SpinroomColors.Bg
    if (solidBackground) {
        Box(modifier.fillMaxSize().background(bg))
        return
    }
    val latest = remember { mutableStateOf(BackdropSource(imageUrl, seedColor)) }
    latest.value = BackdropSource(imageUrl, seedColor)
    val settled by produceState(latest.value) {
        snapshotFlow { latest.value }.settleAfterFirst(SETTLE_MS).collect { value = it }
    }
    Box(modifier.fillMaxSize().background(bg)) {
        Crossfade(settled, animationSpec = tween(CROSSFADE_MS), label = "backdrop") { src ->
            Box(Modifier.fillMaxSize()) {
                if (src.imageUrl != null) BlurredImage(src.imageUrl)
                else {
                    val (c1, c2) = backdropGradient(src.seed)
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2), start = Offset.Zero, end = Offset.Infinite)))
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = BACKDROP_DIM)))
            }
        }
    }
}

@Composable
private fun BlurredImage(url: String) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(BACKDROP_BITMAP_PX, BACKDROP_BITMAP_PX)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .transformations(TinyBlurTransformation)
            .build()
    }
    Image(rememberAsyncImagePainter(request), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

/** Scales to [BACKDROP_BITMAP_PX] and box-blurs; cheap and API-26 safe. The cache key is stable so Coil reuses the result per URL. */
object TinyBlurTransformation : Transformation() {
    override val cacheKey = "spinroom-tiny-blur-$BACKDROP_BITMAP_PX"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val n = BACKDROP_BITMAP_PX
        val scaled = Bitmap.createScaledBitmap(input, n, n, true)
        val px = IntArray(n * n)
        scaled.getPixels(px, 0, n, 0, 0, n, n)
        return Bitmap.createBitmap(boxBlur(px, n, radius = 3), n, n, Bitmap.Config.ARGB_8888)
    }
}
