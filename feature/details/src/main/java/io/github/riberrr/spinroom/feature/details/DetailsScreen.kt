package io.github.riberrr.spinroom.feature.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import io.github.riberrr.spinroom.core.design.ItemMenuHost
import io.github.riberrr.spinroom.core.design.LocalItemMenu
import io.github.riberrr.spinroom.core.design.CoverPlaceholder
import io.github.riberrr.spinroom.core.design.CoverTile
import io.github.riberrr.spinroom.core.design.ErrorState
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomIcons
import io.github.riberrr.spinroom.core.design.SpinroomTheme

data class DetailsActions(
    val onPlay: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onAddToQueue: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onPlayTrack: (TrackRow) -> Unit = {},
    val onOpenAlbum: (AlbumRow) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Album / artist / playlist details (route `details/{itemUri}`). [onOpenItem] gets an MA URI
 * (used for the albums of an artist); Back is handled by the nav host.
 */
@Composable
fun DetailsScreen(
    itemUri: String,
    modifier: Modifier = Modifier,
    onOpenItem: (String) -> Unit = {},
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(itemUri) { viewModel.load(itemUri) }

    val context = LocalContext.current
    val messageText = state.message?.let { stringResource(it.labelRes) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    ItemMenuHost { DetailsContent(
        state = state,
        actions = DetailsActions(
            onPlay = { viewModel.onPlay() },
            onShuffle = { viewModel.onPlay(shuffle = true) },
            onAddToQueue = viewModel::onAddToQueue,
            onToggleFavorite = viewModel::onToggleFavorite,
            onPlayTrack = { viewModel.onPlay(track = it) },
            onOpenAlbum = { onOpenItem(it.uri) },
            onRetry = viewModel::retry,
        ),
        modifier = modifier,
    ) }
}

private val DetailsMessage.labelRes: Int
    get() = when (this) {
        DetailsMessage.Playing -> R.string.details_msg_playing
        DetailsMessage.Queued -> R.string.details_msg_queued
        DetailsMessage.NoPlayer -> R.string.details_msg_no_player
        DetailsMessage.Failed -> R.string.details_msg_failed
    }

@Composable
fun DetailsContent(state: DetailsUiState, actions: DetailsActions, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg)) {
        when (state.status) {
            DetailsStatus.Loading -> Text(
                stringResource(R.string.details_loading),
                Modifier.align(Alignment.Center),
                color = SpinroomColors.TextSecondary,
            )
            DetailsStatus.Error -> ErrorState(
                message = stringResource(R.string.details_error_message),
                retryLabel = stringResource(R.string.details_retry),
                onRetry = actions.onRetry,
            )
            DetailsStatus.Content -> Loaded(state, actions)
        }
    }
}

@Composable
private fun Loaded(state: DetailsUiState, actions: DetailsActions) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }

    Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 48.dp)) {
        Column(Modifier.width(300.dp)) {
            Cover(state.coverUrl, 600, Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)))
            Text(
                stringResource(state.kind.labelRes),
                Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.labelMedium,
                color = SpinroomColors.Accent,
            )
            Text(state.title, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.displaySmall, color = SpinroomColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val meta = metaText(state)
            if (meta.isNotBlank()) {
                Text(meta, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
            }
        }
        Column(Modifier.padding(start = 56.dp).weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionButton(stringResource(R.string.details_play), SpinroomIcons.Play, actions.onPlay, primary = true, modifier = Modifier.focusRequester(playFocus))
                ActionButton(stringResource(R.string.details_shuffle), ShuffleIcon, actions.onShuffle)
                ActionButton(stringResource(R.string.details_add_to_queue), null, actions.onAddToQueue)
                FocusSurface(
                    onClick = actions.onToggleFavorite,
                    shape = CircleShape,
                    focusedScale = 1.08f,
                ) {
                    Icon(
                        if (state.favorite) HeartFilledIcon else HeartIcon,
                        contentDescription = stringResource(R.string.details_favorite),
                        tint = if (state.favorite) SpinroomColors.Accent else SpinroomColors.TextPrimary,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                    )
                }
            }
            Lists(state, actions)
        }
    }
}

@Composable
private fun Lists(state: DetailsUiState, actions: DetailsActions) {
    val menu = LocalItemMenu.current
    LazyColumn(Modifier.fillMaxSize().padding(top = 16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (state.kind == DetailsKind.Artist && state.albums.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.details_albums))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)) {
                    items(state.albums, key = { it.uri }) { album ->
                        CoverTile(
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = { actions.onOpenAlbum(album) },
                            onLongClick = { menu(album.uri, album.title) },
                            cover = { Cover(album.coverUrl, TILE_PX, Modifier.fillMaxSize()) },
                        )
                    }
                }
            }
        }
        if (state.kind == DetailsKind.Artist && state.tracks.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.details_top_tracks)) }
        }
        items(state.tracks, key = { it.number }) { track -> TrackLine(track, state.kind, actions.onPlayTrack) { menu(track.uri, track.title) } }
        if (state.tracks.isEmpty() && state.albums.isEmpty()) {
            item { Text(stringResource(R.string.details_empty_message), Modifier.padding(top = 24.dp), color = SpinroomColors.TextSecondary) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
}

@Composable
private fun TrackLine(track: TrackRow, kind: DetailsKind, onClick: (TrackRow) -> Unit, onLongClick: () -> Unit) {
    FocusSurface(
        onClick = { onClick(track) },
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = SpinroomColors.Bg,
        focusedScale = 1.01f,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(track.number.toString(), Modifier.width(40.dp), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (kind != DetailsKind.Album && track.subtitle.isNotBlank()) {
                    Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(formatDuration(track.durationMs), style = MaterialTheme.typography.bodyMedium, color = SpinroomColors.TextSecondary)
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        containerColor = if (primary) SpinroomColors.Accent else SpinroomColors.Surface,
        focusedContainerColor = if (primary) SpinroomColors.Accent else SpinroomColors.SurfaceHigh,
        focusedScale = 1.06f,
    ) {
        val color = if (primary) SpinroomColors.Bg else SpinroomColors.TextPrimary
        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

@Composable
private fun Cover(url: String?, px: Int, modifier: Modifier) {
    if (url == null) {
        CoverPlaceholder(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).size(Size(px, px)).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { CoverPlaceholder(Modifier.fillMaxSize()) },
        error = { CoverPlaceholder(Modifier.fillMaxSize()) },
    )
}

private val DetailsKind.labelRes: Int
    get() = when (this) {
        DetailsKind.Album -> R.string.details_kind_album
        DetailsKind.Artist -> R.string.details_kind_artist
        DetailsKind.Playlist -> R.string.details_kind_playlist
    }

@Composable
private fun metaText(state: DetailsUiState): String = listOfNotNull(
    state.byline.takeIf { it.isNotBlank() },
    state.year?.toString(),
    stringResource(R.string.details_tracks_count, state.trackCount).takeIf { state.trackCount > 0 && state.kind != DetailsKind.Artist },
    stringResource(R.string.details_minutes, state.totalMinutes).takeIf { state.totalMinutes > 0 && state.kind != DetailsKind.Artist },
).joinToString(" · ")

internal fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

private fun icon(name: String, pathData: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()

private val ShuffleIcon = icon("Shuffle",
    "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5V4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z")

private val HeartIcon = icon("Heart",
    "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5 4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z")

private val HeartFilledIcon = icon("HeartFilled",
    "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z")

private val previewTracks = listOf("Przypływ", "Mapa bez granic", "Światło latarni", "Północny wiatr", "Port 14").mapIndexed { i, t ->
    TrackRow("library://track/$i", i + 1, t, "Kwartet Północ", 240_000L + i * 13_000)
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun AlbumPreview() {
    SpinroomTheme {
        DetailsContent(
            DetailsUiState(status = DetailsStatus.Content, title = "Horyzont", byline = "Kwartet Północ", year = 2024, trackCount = 11, totalMinutes = 46, tracks = previewTracks),
            DetailsActions(),
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun ArtistPreview() {
    SpinroomTheme {
        DetailsContent(
            DetailsUiState(
                status = DetailsStatus.Content, kind = DetailsKind.Artist, title = "Kwartet Północ", favorite = true, tracks = previewTracks,
                albums = listOf(AlbumRow("library://album/1", "Horyzont", "2024", null), AlbumRow("library://album/2", "Port 14", "2021", null)),
            ),
            DetailsActions(),
        )
    }
}
