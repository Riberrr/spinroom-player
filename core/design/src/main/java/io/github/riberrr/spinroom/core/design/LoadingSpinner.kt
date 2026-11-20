package io.github.riberrr.spinroom.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Small rotating arc shown while music is loading. */
@Composable
fun LoadingSpinner(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = SpinroomColors.Accent) {
    val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
        0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "turn",
    )
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension * 0.12f
        drawArc(
            color, startAngle = turn, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(w / 2, w / 2), size = Size(this.size.width - w, this.size.height - w),
            style = Stroke(w, cap = StrokeCap.Round),
        )
    }
}

/** Spinner followed by [label] (e.g. "Ładowanie…"), used where the progress bar normally is. */
@Composable
fun LoadingLabel(label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LoadingSpinner(size = 20.dp)
        Text(label, Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Accent, maxLines = 1, softWrap = false)
    }
}
