package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Centered message with optional action button (which takes initial focus so D-pad works). */
@Composable
private fun CenteredMessage(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier,
) {
    val focus = remember { FocusRequester() }
    val hasAction = onAction != null && actionLabel != null
    if (hasAction) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(56.dp), tint = iconTint)
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodyLarge, color = SpinroomColors.TextSecondary, textAlign = TextAlign.Center)
            }
            if (onAction != null && actionLabel != null) {
                FocusSurface(
                    onAction, Modifier.padding(top = 12.dp).focusRequester(focus),
                    containerColor = SpinroomColors.Accent, focusedContainerColor = SpinroomColors.Accent,
                ) {
                    Text(actionLabel, Modifier.padding(horizontal = 28.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge, color = SpinroomColors.Bg)
                }
            }
        }
    }
}

/** Nothing to show (empty library, no results). Short Polish [title] + optional hint and action. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = CenteredMessage(SpinroomIcons.Music, SpinroomColors.TextSecondary, title, message, actionLabel, onAction, modifier)

/** Something failed. Plain-language [message]; the retry button (if [onRetry] given) takes focus. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Coś poszło nie tak",
    retryLabel: String = "Spróbuj ponownie",
    onRetry: (() -> Unit)? = null,
) = CenteredMessage(SpinroomIcons.Sources, SpinroomColors.Accent, title, message, if (onRetry != null) retryLabel else null, onRetry, modifier)

@Preview(widthDp = 640, heightDp = 360, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun ErrorPreview() {
    SpinroomTheme { ErrorState("Nie widzę serwera Music Assistant w sieci", onRetry = {}) }
}

@Preview(widthDp = 640, heightDp = 360, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun EmptyPreview() {
    SpinroomTheme { EmptyState("Pusto tutaj", message = "Dodaj coś do biblioteki") }
}
