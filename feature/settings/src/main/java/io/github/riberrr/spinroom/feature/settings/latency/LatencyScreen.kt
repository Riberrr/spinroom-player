package io.github.riberrr.spinroom.feature.settings.latency

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.LatencyStore
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.feature.settings.R
import kotlin.math.abs

/** Settings sub-screen: latency slider (←/→ = 10 ms, live) and a test signal played on the TV's MA group. */
@Composable
fun LatencyScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: LatencyViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val sliderFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos {} // after the first layout, so the slider exists
        runCatching { sliderFocus.requestFocus() }
    }

    Column(
        modifier.fillMaxSize().background(SpinroomColors.Bg).padding(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.latency_title), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
        Text(stringResource(R.string.latency_intro), color = SpinroomColors.TextSecondary, modifier = Modifier.widthIn(max = 760.dp))

        Slider(
            valueMs = ui.offsetMs,
            onNudge = viewModel::nudge,
            modifier = Modifier.focusRequester(sliderFocus),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            when (ui.signal) {
                SignalState.Playing -> Button(stringResource(R.string.latency_stop), viewModel::stopSignal)
                SignalState.Starting -> Button(stringResource(R.string.latency_starting)) {}
                else -> if (ui.group != null) Button(stringResource(R.string.latency_play), viewModel::playSignal)
            }
            Button(stringResource(R.string.latency_reset), viewModel::reset)
        }

        val group = ui.group
        Text(
            if (group == null) stringResource(R.string.latency_no_group)
            else stringResource(R.string.latency_group, group.otherSpeakers.joinToString(", ")),
            color = if (group == null) SpinroomColors.TextSecondary else SpinroomColors.Success,
        )
        when (ui.signal) {
            SignalState.Playing -> Text(stringResource(R.string.latency_hint), color = SpinroomColors.TextPrimary, modifier = Modifier.widthIn(max = 760.dp))
            SignalState.Failed -> Text(stringResource(R.string.latency_failed), color = SpinroomColors.TextPrimary)
            else -> Unit
        }
    }
}

@Composable
private fun Slider(valueMs: Int, onNudge: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.widthIn(max = TRACK_WIDTH), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            when {
                valueMs > 0 -> stringResource(R.string.latency_value_earlier, valueMs)
                valueMs < 0 -> stringResource(R.string.latency_value_later, abs(valueMs))
                else -> stringResource(R.string.latency_value_zero)
            },
            style = MaterialTheme.typography.titleLarge,
            color = SpinroomColors.TextPrimary,
        )
        FocusSurface(
            onClick = {},
            focusedScale = 1f,
            modifier = modifier.fillMaxWidth().onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent e.key == Key.DirectionLeft || e.key == Key.DirectionRight
                when (e.key) {
                    Key.DirectionLeft -> { onNudge(-1); true }
                    Key.DirectionRight -> { onNudge(1); true }
                    else -> false
                }
            },
        ) {
            Track(valueMs, Modifier.padding(horizontal = 20.dp, vertical = 22.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("← ${stringResource(R.string.latency_left)}", color = SpinroomColors.TextSecondary)
            Text("${stringResource(R.string.latency_right)} →", color = SpinroomColors.TextSecondary)
        }
    }
}

/** Track centred on 0: the accent bar grows from the middle towards the value. Fits any width. */
@Composable
private fun Track(valueMs: Int, modifier: Modifier = Modifier) {
    val fraction = (valueMs.toFloat() / LatencyStore.MAX_MS).coerceIn(-1f, 1f)
    Box(modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(SpinroomColors.Outline)) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (fraction < 0) Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(-fraction).fillMaxHeight().background(SpinroomColors.Accent))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (fraction > 0) Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(fraction).fillMaxHeight().background(SpinroomColors.Accent))
            }
        }
        Box(Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().background(SpinroomColors.TextPrimary))
    }
}

@Composable
private fun Button(label: String, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, modifier = Modifier.widthIn(min = 200.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
    }
}

private val TRACK_WIDTH = 640.dp
