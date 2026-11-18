package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.SubcomposeAsyncImage

/**
 * Small square thumbnail for list rows: the image, else initials on a stable color ([isStation]) or the cover placeholder.
 * Give it a size through [modifier].
 */
@Composable
fun EntryThumb(name: String, imageUrl: String?, isStation: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val fallback: @Composable () -> Unit = {
        if (isStation) {
            AcronymFill(name, Modifier.fillMaxSize(), MaterialTheme.typography.labelMedium)
        } else CoverPlaceholder(Modifier.fillMaxSize())
    }
    Box(modifier.clip(shape)) {
        if (imageUrl == null) fallback() else SubcomposeAsyncImage(
            model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            loading = { fallback() }, error = { fallback() },
        )
    }
}
