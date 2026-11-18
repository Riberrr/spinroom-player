package io.github.riberrr.spinroom.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch

/**
 * Pressing [key] moves focus straight to [target] (no-op when [target] is null, so an empty list never grabs focus).
 *
 * Explicit on purpose: the geometric search cannot enter a lazy list that sits below a row of controls. The shell's
 * `focusRestorer` hands its `onEnter` to every focus target below it, so the first request into the list is
 * redirected to the control that was last focused (usually the first favorite). The redirect is spent after that,
 * hence the second request one frame later; it is a no-op when the first one already landed.
 */
@Composable
fun Modifier.focusJump(key: Key, target: FocusRequester?): Modifier {
    val scope = rememberCoroutineScope()
    return if (target == null) this else onPreviewKeyEvent { e ->
        if (e.key != key) {
            false
        } else {
            if (e.type == KeyEventType.KeyDown) {
                scope.launch {
                    runCatching { target.requestFocus() }
                    androidx.compose.runtime.withFrameNanos { }
                    runCatching { target.requestFocus() }
                }
            }
            true
        }
    }
}
