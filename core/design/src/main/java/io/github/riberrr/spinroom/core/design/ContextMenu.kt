package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.riberrr.spinroom.core.data.ItemMenuViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Long-press (hold OK) menu shared by all screens: play next, add to queue, favorite toggle.
 * [favorite] null = not known yet (the entry then reads "add"). Back closes it via [onDismiss].
 * Texts live in this module's `strings.xml` so every screen shows the same wording.
 */
@Composable
fun ContextMenu(
    title: String,
    favorite: Boolean?,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(360.dp).background(SpinroomColors.Surface, RoundedCornerShape(16.dp)).padding(20.dp),
        ) {
            Text(title, Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MenuEntry(stringResource(R.string.menu_play_next), onPlayNext, Modifier.focusRequester(first))
            MenuEntry(stringResource(R.string.menu_add_to_queue), onAddToQueue)
            MenuEntry(
                stringResource(if (favorite == true) R.string.menu_favorite_remove else R.string.menu_favorite_add),
                onToggleFavorite,
            )
        }
    }
}

@Composable
private fun MenuEntry(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick,
        modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = SpinroomColors.SurfaceHigh,
        focusedScale = 1.02f,
    ) {
        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Tiles call this (uri, title) on hold-OK; no-op outside an [ItemMenuHost] (previews). */
val LocalItemMenu = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

/**
 * Wrap a screen's content once: provides [LocalItemMenu] and shows the shared [ContextMenu] for the held item.
 * The default `viewModel()` resolves to the Hilt factory of the activity.
 */
@Composable
fun ItemMenuHost(menu: ItemMenuViewModel = viewModel(), content: @Composable () -> Unit) {
    val target by menu.target.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalItemMenu provides { uri, title -> menu.open(uri, title) }) { content() }
    target?.let {
        ContextMenu(it.title, it.favorite, menu::playNext, menu::addToQueue, menu::toggleFavorite, menu::dismiss)
    }
}
