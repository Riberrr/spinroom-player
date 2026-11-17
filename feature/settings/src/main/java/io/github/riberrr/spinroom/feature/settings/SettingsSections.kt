package io.github.riberrr.spinroom.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.Appearance
import io.github.riberrr.spinroom.core.data.HomeLayout
import io.github.riberrr.spinroom.core.data.HomeRows
import io.github.riberrr.spinroom.core.data.PlayerName
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.SpinroomAccent
import io.github.riberrr.spinroom.core.design.SpinroomColors

private val ThemeLabels = mapOf(
    Appearance.THEME_GRAFIT to R.string.appearance_theme_grafit,
    Appearance.THEME_GRANAT to R.string.appearance_theme_granat,
    Appearance.THEME_OLED to R.string.appearance_theme_oled,
)
private val AccentLabels = mapOf(
    Appearance.ACCENT_AMBER to R.string.appearance_accent_amber,
    Appearance.ACCENT_MINT to R.string.appearance_accent_mint,
    Appearance.ACCENT_SKY to R.string.appearance_accent_sky,
    Appearance.ACCENT_CORAL to R.string.appearance_accent_coral,
    Appearance.ACCENT_LAVENDER to R.string.appearance_accent_lavender,
)
private val BackgroundLabels = mapOf(
    Appearance.BG_BLUR to R.string.appearance_bg_blur,
    Appearance.BG_COLOR to R.string.appearance_bg_color,
    Appearance.BG_SOLID to R.string.appearance_bg_solid,
)
private val RowLabels = mapOf(
    HomeRows.STATIONS to R.string.home_row_stations,
    HomeRows.PLAYLISTS to R.string.home_row_playlists,
    HomeRows.RECENT to R.string.home_row_recent,
)

@Composable
internal fun SectionLabel(text: String) = Text(text, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.labelMedium)

/** A pill-shaped choice; the selected one is filled with the accent. */
@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = if (selected) SpinroomColors.Accent else SpinroomColors.Glass,
        focusedContainerColor = if (selected) SpinroomColors.Accent else SpinroomColors.GlassHigh,
        focusedScale = 1.04f,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (selected) SpinroomColors.Bg else SpinroomColors.TextPrimary,
        )
    }
}

/** SUI8a "Wygląd": player background, theme, accent, screensaver; the preview follows the choices at once. */
@Composable
internal fun AppearanceSection(ui: SettingsUiState, vm: SettingsViewModel) {
    val a = ui.appearance
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(stringResource(R.string.appearance_bg_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Appearance.PLAYER_BACKGROUNDS.forEach { id ->
                    Chip(stringResource(BackgroundLabels.getValue(id)), a.playerBackground == id, { vm.setPlayerBackground(id) })
                }
            }
            SectionLabel(stringResource(R.string.appearance_theme_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Appearance.THEMES.forEach { id ->
                    Chip(stringResource(ThemeLabels.getValue(id)), a.theme == id, { vm.setTheme(id) })
                }
            }
            SectionLabel(stringResource(R.string.appearance_accent_title))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SpinroomAccent.entries.forEach { accent ->
                    val selected = a.accent == accent.id
                    FocusSurface(
                        onClick = { vm.setAccent(accent.id) },
                        shape = CircleShape,
                        containerColor = accent.color,
                        focusedContainerColor = accent.color,
                        focusedScale = 1.12f,
                        modifier = Modifier.size(40.dp).then(
                            if (selected) Modifier.border(BorderStroke(3.dp, SpinroomColors.TextPrimary), CircleShape) else Modifier,
                        ),
                    ) { Box(Modifier.size(40.dp)) }
                }
                Text(stringResource(AccentLabels.getValue(a.accent)), color = SpinroomColors.TextSecondary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(stringResource(R.string.appearance_preview))
            AppearancePreview(a)
            FocusSurface(
                onClick = { vm.setScreensaver(!a.screensaver) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = SpinroomColors.Glass,
                focusedContainerColor = SpinroomColors.GlassHigh,
                focusedScale = 1.02f,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_screensaver), color = SpinroomColors.TextPrimary)
                        Text(
                            stringResource(if (a.screensaver) R.string.appearance_screensaver_on else R.string.appearance_screensaver_off),
                            color = SpinroomColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        stringResource(if (a.screensaver) R.string.toggle_on else R.string.toggle_off),
                        color = if (a.screensaver) SpinroomColors.Accent else SpinroomColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearancePreview(a: Appearance) {
    val blob = SpinroomColors.Accent.copy(alpha = 0.35f)
    val bg = when (a.playerBackground) {
        Appearance.BG_BLUR -> Brush.radialGradient(listOf(blob, SpinroomColors.Bg), radius = 520f)
        Appearance.BG_COLOR -> Brush.linearGradient(listOf(blob, SpinroomColors.Bg))
        else -> Brush.linearGradient(listOf(SpinroomColors.Surface, SpinroomColors.Surface))
    }
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)).background(bg).border(BorderStroke(1.dp, SpinroomColors.GlassBorder), RoundedCornerShape(16.dp))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(SpinroomColors.GlassHigh))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.appearance_preview_now), color = SpinroomColors.Accent, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.appearance_preview_title), color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.appearance_preview_artist), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Box(Modifier.widthIn(min = 160.dp).height(4.dp).clip(CircleShape).background(SpinroomColors.GlassHigh)) {
                    Box(Modifier.fillMaxWidth(0.4f).height(4.dp).background(SpinroomColors.Accent))
                }
            }
        }
    }
}

/** SUI8a "Ekran główny": reorder and hide the Start rows. */
@Composable
internal fun HomeRowsSection(layout: HomeLayout, vm: SettingsViewModel) {
    Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.home_rows_hint), color = SpinroomColors.TextSecondary)
        layout.order.forEachIndexed { index, id ->
            val shown = id !in layout.hidden
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(RowLabels.getValue(id)),
                        modifier = Modifier.weight(1f),
                        color = if (shown) SpinroomColors.TextPrimary else SpinroomColors.TextSecondary,
                    )
                    if (index > 0) Chip(stringResource(R.string.home_row_up), false, { vm.moveRow(id, -1) })
                    if (index < layout.order.lastIndex) Chip(stringResource(R.string.home_row_down), false, { vm.moveRow(id, 1) })
                    Chip(stringResource(if (shown) R.string.toggle_on else R.string.toggle_off), shown, { vm.toggleRow(id) })
                }
            }
        }
    }
}

/** Dialog with a name field, suggestions and Zapisz / Anuluj (makieta 09B). */
@Composable
internal fun PlayerNameDialog(current: String, onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    val result = PlayerName.validate(text)
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(Modifier.widthIn(min = 560.dp, max = 640.dp), high = true) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.name_title), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
                SettingsTextField(stringResource(R.string.name_field), text, { text = it })
                Text(
                    stringResource(
                        when (result) {
                            PlayerName.Result.TooLong -> R.string.name_too_long
                            PlayerName.Result.Empty -> R.string.name_empty
                            is PlayerName.Result.Ok -> R.string.name_hint
                        },
                        PlayerName.MAX_LENGTH,
                    ),
                    color = if (result is PlayerName.Result.Ok) SpinroomColors.TextSecondary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerName.SUGGESTIONS.take(3).forEach { Chip(it, text.trim() == it, { text = it }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerName.SUGGESTIONS.drop(3).forEach { Chip(it, text.trim() == it, { text = it }) }
                        Chip(stringResource(R.string.name_custom), false, { text = "" })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    Chip(stringResource(R.string.name_cancel), false, onDismiss)
                    Chip(stringResource(R.string.name_save), result is PlayerName.Result.Ok, { if (onSave(text)) onDismiss() })
                }
            }
        }
    }
}
