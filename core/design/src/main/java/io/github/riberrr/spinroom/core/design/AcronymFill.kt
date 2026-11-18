package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Style A fallback for items without a cover (stations, playlists): diagonal 135° gradient derived from [name],
 * a big acronym at ~28 % opacity cropped in the bottom-right corner and (with [showCenter]) a readable acronym in the center.
 * [centerStyle] is the centered label; the watermark scales with the box height.
 */
@Composable
fun AcronymFill(
    name: String,
    modifier: Modifier = Modifier,
    centerStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showCenter: Boolean = true,
) {
    val (a, b) = remember(name) { gradientColors(name) }
    val acronym = remember(name) { stationAcronym(name) }
    BoxWithConstraints(
        modifier.clipToBounds().background(Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        val density = LocalDensity.current
        val heightDp = maxHeight
        val watermarkSp = with(density) { (heightDp * 0.62f).toSp() }
        Text(
            acronym,
            Modifier.align(Alignment.BottomEnd).wrapContentSize(Alignment.BottomEnd, unbounded = true).offset(x = heightDp * 0.14f, y = heightDp * 0.20f),
            style = TextStyle(fontSize = watermarkSp, lineHeight = watermarkSp, fontWeight = FontWeight.ExtraBold),
            color = SpinroomColors.TextPrimary.copy(alpha = 0.28f), maxLines = 1, softWrap = false,
        )
        if (showCenter) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(acronym, style = centerStyle, color = SpinroomColors.TextPrimary, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
            }
        }
    }
}
