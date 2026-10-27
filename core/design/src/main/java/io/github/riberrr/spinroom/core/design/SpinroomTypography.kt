package io.github.riberrr.spinroom.core.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/** Inter (OFL) bundled with the app, weights 400/500/600/700. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int = (size * 1.3).toInt()) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

/** TV type scale (readable from ~3 m; nothing below 14 sp). Sizes are dp-equivalent on a 1280x720 dp canvas. */
val SpinroomTypography = Typography(
    displayLarge = style(48, FontWeight.Bold),
    displayMedium = style(40, FontWeight.Bold),
    displaySmall = style(32, FontWeight.Bold),
    headlineLarge = style(32, FontWeight.SemiBold),
    headlineMedium = style(28, FontWeight.SemiBold),
    headlineSmall = style(24, FontWeight.SemiBold),
    titleLarge = style(22, FontWeight.SemiBold),
    titleMedium = style(18, FontWeight.SemiBold),
    titleSmall = style(16, FontWeight.SemiBold),
    bodyLarge = style(18, FontWeight.Normal),
    bodyMedium = style(16, FontWeight.Normal),
    bodySmall = style(14, FontWeight.Normal),
    labelLarge = style(16, FontWeight.Medium),
    labelMedium = style(14, FontWeight.Medium),
    labelSmall = style(14, FontWeight.Medium),
)
