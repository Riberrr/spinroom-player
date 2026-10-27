package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Immutable snapshot rendered by [MiniPlayerBar]. Times in milliseconds. */
data class MiniPlayerState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Bottom bar: cover, title/artist, prev / play-pause / next, progress, "Teraz odtwarzane" button.
 * Stateless; the [cover] slot defaults to a placeholder.
 */
@Composable
fun MiniPlayerBar(
    state: MiniPlayerState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    nowPlayingLabel: String = "Teraz odtwarzane",
    cover: @Composable () -> Unit = { CoverPlaceholder(Modifier.fillMaxSize()) },
) {
    Row(
        modifier.fillMaxWidth().height(80.dp).background(SpinroomColors.Surface).padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp)) { cover() }
        Column(Modifier.padding(start = 20.dp).weight(1f)) {
            Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.artist, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(SpinroomIcons.Previous, "Poprzedni", onPrevious, 40.dp)
            RoundIconButton(
                if (state.isPlaying) SpinroomIcons.Pause else SpinroomIcons.Play,
                if (state.isPlaying) "Pauza" else "Odtwórz",
                onPlayPause, 48.dp, filled = true, modifier = Modifier.padding(horizontal = 16.dp),
            )
            RoundIconButton(SpinroomIcons.Next, "Następny", onNext, 40.dp)
        }
        Text(formatTime(state.positionMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
        ProgressBar(state.positionMs, state.durationMs, Modifier.padding(horizontal = 12.dp).weight(1f))
        Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary)
        FocusSurface(
            onOpenNowPlaying, Modifier.padding(start = 24.dp), shape = RoundedCornerShape(24.dp),
            containerColor = SpinroomColors.SurfaceHigh, focusedContainerColor = SpinroomColors.Outline, focusedScale = 1.04f,
        ) {
            Text(nowPlayingLabel, Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
        }
    }
}

/** Thin accent progress line. Stateless; clamps to 0..1. */
@Composable
fun ProgressBar(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(modifier.height(4.dp).background(SpinroomColors.Outline, CircleShape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(SpinroomColors.Accent, CircleShape))
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    FocusSurface(
        onClick, modifier.size(size), shape = CircleShape,
        containerColor = if (filled) SpinroomColors.TextPrimary else Color.Transparent,
        focusedContainerColor = if (filled) SpinroomColors.TextPrimary else SpinroomColors.SurfaceHigh,
        focusedScale = 1.1f,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, description, Modifier.size(size / 2), tint = if (filled) SpinroomColors.Bg else SpinroomColors.TextPrimary)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 80, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    SpinroomTheme {
        MiniPlayerBar(MiniPlayerState("Latarnie nad rzeką", "Lena Wiatr", true, 84_000, 221_000), {}, {}, {}, {})
    }
}
