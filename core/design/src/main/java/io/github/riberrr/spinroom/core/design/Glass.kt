package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Non-focusable translucent surface (white ~6 %, or ~12 % with [high]) with a hairline border, for cards, menus and the player bar.
 * Put it over [ContentBackdrop] (or any background). For clickable glass use [FocusSurface] with `SpinroomColors.Glass*`.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    high: Boolean = false,
    color: Color = if (high) SpinroomColors.GlassHigh else SpinroomColors.Glass,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(color)
            .border(BorderStroke(1.dp, SpinroomColors.GlassBorder), shape),
    ) { content() }
}
