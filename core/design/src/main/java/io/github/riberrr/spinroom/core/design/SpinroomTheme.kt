package io.github.riberrr.spinroom.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.riberrr.spinroom.core.data.Appearance

/** Theme background family (surfaces behind content). */
enum class SpinroomThemeId(val id: String, val bg: Color, val surface: Color, val surfaceHigh: Color, val outline: Color) {
    Grafit(Appearance.THEME_GRAFIT, Color(0xFF0B0D12), Color(0xFF151922), Color(0xFF1E2430), Color(0xFF2A3140)),
    Granat(Appearance.THEME_GRANAT, Color(0xFF070D1F), Color(0xFF0F1830), Color(0xFF18244A), Color(0xFF263562)),
    Oled(Appearance.THEME_OLED, Color(0xFF000000), Color(0xFF0C0C0E), Color(0xFF17171B), Color(0xFF26262C));

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: Grafit
    }
}

/** Selectable accent colors. */
enum class SpinroomAccent(val id: String, val color: Color) {
    Amber(Appearance.ACCENT_AMBER, Color(0xFFF5A524)),
    Mint(Appearance.ACCENT_MINT, Color(0xFF3DD68C)),
    Sky(Appearance.ACCENT_SKY, Color(0xFF4FC3F7)),
    Coral(Appearance.ACCENT_CORAL, Color(0xFFF07178)),
    Lavender(Appearance.ACCENT_LAVENDER, Color(0xFFB39DFF));

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: Amber
    }
}

/** Resolved palette for the current theme + accent. Read through [SpinroomColors]. */
@Immutable
data class SpinroomPalette(val theme: SpinroomThemeId, val accent: Color)

internal val LocalSpinroomPalette = staticCompositionLocalOf { SpinroomPalette(SpinroomThemeId.Grafit, SpinroomAccent.Amber.color) }

/** True when the user wants a plain theme background instead of [ContentBackdrop]. */
val LocalSolidBackground = staticCompositionLocalOf { false }

/**
 * Brand colors (docs/BRAND.md). Use these instead of raw hex in feature code.
 * Theme-dependent ones (Bg, Surface*, Outline, Accent) are composable getters and follow [SpinroomTheme];
 * the defaults equal the original look.
 */
object SpinroomColors {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalSpinroomPalette.current.theme.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalSpinroomPalette.current.theme.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalSpinroomPalette.current.theme.surfaceHigh
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalSpinroomPalette.current.theme.outline
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalSpinroomPalette.current.accent
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFF9AA3B2)
    val Success = Color(0xFF3DD68C)
    val Info = Color(0xFF4FC3F7)

    /** Glass: translucent white over the backdrop. Cards, menus, player bar. */
    val Glass = Color.White.copy(alpha = 0.06f)
    val GlassHigh = Color.White.copy(alpha = 0.12f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)
}

private fun schemeOf(p: SpinroomPalette) = darkColorScheme(
    primary = p.accent,
    onPrimary = p.theme.bg,
    background = p.theme.bg,
    onBackground = SpinroomColors.TextPrimary,
    surface = p.theme.surface,
    onSurface = SpinroomColors.TextPrimary,
    surfaceVariant = p.theme.surfaceHigh,
    onSurfaceVariant = SpinroomColors.TextSecondary,
    border = p.theme.outline,
)

/** App theme: [theme] and [accent] (ids from `Appearance`), Inter typography. Defaults give the original Grafit/amber look. */
@Composable
fun SpinroomTheme(
    theme: String = Appearance.THEME_GRAFIT,
    accent: String = Appearance.ACCENT_AMBER,
    solidBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = SpinroomPalette(SpinroomThemeId.fromId(theme), SpinroomAccent.fromId(accent).color)
    CompositionLocalProvider(LocalSpinroomPalette provides palette, LocalSolidBackground provides solidBackground) {
        MaterialTheme(colorScheme = schemeOf(palette), typography = SpinroomTypography) {
            CompositionLocalProvider(LocalContentColor provides SpinroomColors.TextPrimary, content = content)
        }
    }
}
