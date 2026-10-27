package io.github.riberrr.spinroom.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Brand colors from the mockups (docs/BRAND.md). Use these instead of raw hex values in feature code. */
object SpinroomColors {
    val Bg = Color(0xFF0B0D12)
    val Surface = Color(0xFF151922)
    val SurfaceHigh = Color(0xFF1E2430)
    val Outline = Color(0xFF2A3140)
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFF9AA3B2)
    val Accent = Color(0xFFF5A524)
    val Success = Color(0xFF3DD68C)
    val Info = Color(0xFF4FC3F7)
}

private val SpinroomColorScheme = darkColorScheme(
    primary = SpinroomColors.Accent,
    onPrimary = SpinroomColors.Bg,
    background = SpinroomColors.Bg,
    onBackground = SpinroomColors.TextPrimary,
    surface = SpinroomColors.Surface,
    onSurface = SpinroomColors.TextPrimary,
    surfaceVariant = SpinroomColors.SurfaceHigh,
    onSurfaceVariant = SpinroomColors.TextSecondary,
    border = SpinroomColors.Outline,
)

/** App theme: brand colors + Inter typography. */
@Composable
fun SpinroomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SpinroomColorScheme, typography = SpinroomTypography) {
        CompositionLocalProvider(LocalContentColor provides SpinroomColors.TextPrimary, content = content)
    }
}
