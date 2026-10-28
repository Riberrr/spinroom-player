package io.github.riberrr.spinroom.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.SpinroomColors
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Idle time without a key press before the screensaver appears. */
const val SCREENSAVER_IDLE_MS = 60_000L

private const val DRIFT_PERIOD_X_MS = 170_000.0
private const val DRIFT_PERIOD_Y_MS = 110_000.0

/** Slow burn-in protection: offset (px) after [elapsedMs] of screensaver, a lazy Lissajous path within ±[rangeX]/±[rangeY]. */
internal fun driftOffset(elapsedMs: Long, rangeX: Float, rangeY: Float): Pair<Float, Float> =
    Pair(
        (rangeX * sin(2 * PI * elapsedMs / DRIFT_PERIOD_X_MS)).toFloat(),
        (rangeY * sin(2 * PI * elapsedMs / DRIFT_PERIOD_Y_MS)).toFloat(),
    )

/**
 * Full-screen overlay for the activity: after [SCREENSAVER_IDLE_MS] without input (every key press bumps [inputTick])
 * [onIdle] fires, and while [visible] and a track is known it draws the cover and title on black.
 * The caller swallows the key press that dismisses it.
 */
@Composable
fun ScreensaverLayer(
    inputTick: Int,
    visible: Boolean,
    onIdle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = activityNowPlayingViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(inputTick, ui.hasTrack) {
        if (ui.hasTrack) {
            delay(SCREENSAVER_IDLE_MS)
            onIdle()
        }
    }
    if (visible && ui.hasTrack) ScreensaverContent(ui, modifier)
}

/** Stateless screensaver: dimmed cover + title/artist, drifting slowly so no pixel stays lit in one place. */
@Composable
fun ScreensaverContent(state: NowPlayingUiState, modifier: Modifier = Modifier) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            elapsed += 200
        }
    }
    val (dx, dy) = driftOffset(elapsed, rangeX = 160f, rangeY = 60f)
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg), contentAlignment = Alignment.Center) {
        Column(
            Modifier.graphicsLayer { translationX = dx; translationY = dy }.alpha(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cover(state.coverUrl, Modifier.size(320.dp).clip(RoundedCornerShape(12.dp)))
            Text(
                state.title, style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Text(state.artist, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
