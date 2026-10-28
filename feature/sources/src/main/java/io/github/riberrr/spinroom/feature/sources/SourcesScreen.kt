package io.github.riberrr.spinroom.feature.sources

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.riberrr.spinroom.core.design.ItemMenuHost
import io.github.riberrr.spinroom.core.design.LocalItemMenu
import io.github.riberrr.spinroom.core.design.EmptyState
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.MediaItem

/** Actions the screen can trigger; the ViewModel implements them, previews pass no-ops. */
data class SourcesActions(
    val onSelectProvider: (Int) -> Unit = {},
    val onItemClick: (SourceRow) -> Unit = {},
    val onToggleFavorite: (SourceRow) -> Unit = {},
    val onCrumb: (Int) -> Unit = {},
    val onBack: () -> Boolean = { false },
    val onRetry: () -> Unit = {},
) {
    companion object {
        fun of(vm: SourcesViewModel) = SourcesActions(
            vm::onSelectProvider, vm::onItemClick, vm::onToggleFavorite, vm::onCrumb, vm::onBack, vm::onRetry,
        )
    }
}

/** "Źródła" screen: provider list + browse tree. Stateful wrapper around [SourcesContent]. */
@Composable
fun SourcesScreen(modifier: Modifier = Modifier, viewModel: SourcesViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    ItemMenuHost { SourcesContent(ui, SourcesActions.of(viewModel), modifier) }
}

@Composable
fun SourcesContent(state: SourcesUiState, actions: SourcesActions, modifier: Modifier = Modifier) {
    BackHandler(enabled = state.canGoUp) { actions.onBack() }
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        when {
            state.providersFailed || state.connection is MaConnectionState.Failed ->
                ErrorState(stringResource(R.string.sources_error), onRetry = actions.onRetry,
                    title = stringResource(R.string.sources_error_title), retryLabel = stringResource(R.string.sources_retry))
            state.connection !is MaConnectionState.Connected || state.loadingProviders ->
                Text(stringResource(R.string.sources_loading), color = SpinroomColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
            state.providers.isEmpty() ->
                EmptyState(stringResource(R.string.sources_empty_title), message = stringResource(R.string.sources_empty_message))
            else -> Row(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                ProviderColumn(state, actions, Modifier.width(300.dp).fillMaxHeight())
                BrowsePane(state, actions, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ProviderColumn(state: SourcesUiState, actions: SourcesActions, modifier: Modifier) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(modifier) {
        Text(stringResource(R.string.sources_title), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary)
        Text(stringResource(R.string.sources_subtitle), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.providers, key = { _, r -> r.key }) { i, row ->
                val selected = i == state.selectedProvider
                FocusSurface(
                    onClick = { actions.onSelectProvider(i) },
                    modifier = Modifier.fillMaxWidth()
                        .then(if (i == state.selectedProvider) Modifier.focusRequester(first) else Modifier)
                        .onFocusChanged { if (it.isFocused) actions.onSelectProvider(i) },
                    shape = RoundedCornerShape(10.dp),
                    containerColor = if (selected) SpinroomColors.SurfaceHigh else Color.Transparent,
                    focusedScale = 1.02f,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Badge(row, 40)
                        Column(Modifier.weight(1f)) {
                            Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (row.subtitle.isNotBlank()) {
                                Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowsePane(state: SourcesUiState, actions: SourcesActions, modifier: Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Breadcrumbs(state.trail, actions.onCrumb, Modifier.weight(1f))
            if (state.items.isNotEmpty()) {
                Text(stringResource(R.string.sources_count, state.items.size), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.itemsFailed -> ErrorState(stringResource(R.string.sources_items_error), onRetry = actions.onRetry,
                    title = stringResource(R.string.sources_error_title), retryLabel = stringResource(R.string.sources_retry))
                state.loadingItems -> Text(stringResource(R.string.sources_loading), color = SpinroomColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
                state.items.isEmpty() -> EmptyState(stringResource(R.string.sources_folder_empty))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.items, key = { _, r -> r.key }) { _, row -> ItemRow(row, actions) }
                }
            }
        }
        val hint = when {
            state.noPlayer -> stringResource(R.string.sources_no_player)
            state.playFailed -> stringResource(R.string.sources_play_failed)
            state.playedName != null -> stringResource(R.string.sources_playing, state.playedName)
            else -> stringResource(R.string.sources_hint)
        }
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Breadcrumbs(trail: List<Crumb>, onCrumb: (Int) -> Unit, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        trail.forEachIndexed { i, crumb ->
            val last = i == trail.lastIndex
            if (i > 0) Text("›", color = SpinroomColors.TextSecondary)
            if (last) {
                Text(crumb.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SpinroomColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                FocusSurface({ onCrumb(i) }, shape = RoundedCornerShape(8.dp), containerColor = Color.Transparent, focusedScale = 1.04f) {
                    Text(crumb.name, style = MaterialTheme.typography.titleSmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ItemRow(row: SourceRow, actions: SourcesActions) {
    val menu = LocalItemMenu.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusSurface(
            { actions.onItemClick(row) }, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), focusedScale = 1.01f,
            onLongClick = if (row.canFavorite) ({ row.item.uri?.let { menu(it, row.name) } }) else null,
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Badge(row, 42)
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (row.subtitle.isNotBlank()) {
                        Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.isFolder) Text("›", style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextSecondary)
            }
        }
        if (row.canFavorite) {
            FocusSurface({ actions.onToggleFavorite(row) }, shape = RoundedCornerShape(10.dp), focusedScale = 1.08f) {
                Text(
                    if (row.favorite) "♥" else "♡",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (row.favorite) SpinroomColors.Accent else SpinroomColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private val badgeColors = listOf(Color(0xFF7D3F4A), Color(0xFF2E4A7D), Color(0xFF3F7D5C), Color(0xFF7D6A2E), Color(0xFF5F3F7D), Color(0xFF2E6A7D))

/** Cover when the server has one, otherwise coloured initials (the colour is stable per name). */
@Composable
private fun Badge(row: SourceRow, sizeDp: Int) {
    val color = badgeColors[(row.name.hashCode() and 0x7fffffff) % badgeColors.size]
    val shape = RoundedCornerShape(6.dp)
    Box(Modifier.size(sizeDp.dp).clip(shape).background(color, shape), contentAlignment = Alignment.Center) {
        Text(row.initials, style = MaterialTheme.typography.labelMedium, color = SpinroomColors.TextPrimary)
        if (row.coverUrl != null) {
            SubcomposeAsyncImage(
                model = row.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                loading = {}, error = {},
            )
        }
    }
}

private fun previewRow(name: String, sub: String, folder: Boolean = false, fav: Boolean = false) = SourceRow(
    item = MediaItem(
        itemId = name, provider = "radiobrowser", name = name, uri = "radiobrowser://radio/$name",
        mediaType = if (folder) "folder" else "radio", path = if (folder) "radiobrowser://$name" else null, isPlayable = !folder,
    ),
    subtitle = sub, coverUrl = null, favorite = fav,
)

private val previewState = SourcesUiState(
    connection = MaConnectionState.Connected(io.github.riberrr.spinroom.core.maapi.ServerInfo(), true),
    loadingProviders = false,
    providers = listOf(previewRow("Radio Browser", "Radio internetowe", true), previewRow("Tuneln", "Radio i podcasty", true), previewRow("YouTube Music", "Muzyka i miksy", true)),
    trail = listOf(Crumb("Radio Browser", "a"), Crumb("Według kraju", "b"), Crumb("Polska", "c")),
    items = listOf(previewRow("Jazz Jeden", "Jazz · Kraków"), previewRow("Klasyka 24", "Muzyka klasyczna · Warszawa", fav = true), previewRow("Lo-fi Studio", "Lo-fi · Wrocław"), previewRow("Popularne", "", true)),
)

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun SourcesPreview() = SpinroomTheme { SourcesContent(previewState, SourcesActions()) }
