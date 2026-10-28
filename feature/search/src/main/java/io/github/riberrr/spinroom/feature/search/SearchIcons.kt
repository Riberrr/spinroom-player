package io.github.riberrr.spinroom.feature.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Microphone, 24 dp grid, stroked like the other Spinroom icons. */
internal val MicIcon: ImageVector = ImageVector.Builder("Mic", 24.dp, 24.dp, 24f, 24f).apply {
    val stroke = SolidColor(Color.Black)
    path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 3f)
        curveTo(10.3f, 3f, 9f, 4.3f, 9f, 6f)
        lineTo(9f, 12f)
        curveTo(9f, 13.7f, 10.3f, 15f, 12f, 15f)
        curveTo(13.7f, 15f, 15f, 13.7f, 15f, 12f)
        lineTo(15f, 6f)
        curveTo(15f, 4.3f, 13.7f, 3f, 12f, 3f)
        close()
    }
    path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 11f)
        curveTo(6f, 14.3f, 8.7f, 17f, 12f, 17f)
        curveTo(15.3f, 17f, 18f, 14.3f, 18f, 11f)
        moveTo(12f, 17f)
        lineTo(12f, 21f)
    }
}.build()
