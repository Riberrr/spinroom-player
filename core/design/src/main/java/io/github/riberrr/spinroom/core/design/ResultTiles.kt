package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

/**
 * Square radio-station tile, same rhythm as [CoverTile]. Without [logoUrl]/[logo] it shows the initials on a color derived from the name.
 * Name below, optional [subtitle] (e.g. country or provider).
 */
@Composable
fun StationTile(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    logoUrl: String? = null,
    onLongClick: (() -> Unit)? = null,
    logo: (@Composable () -> Unit)? = null,
) {
    Column(modifier.width(size)) {
        FocusSurface(
            onClick, Modifier.size(size), focusedScale = 1.08f, onLongClick = onLongClick,
            containerColor = stationColor(name), focusedContainerColor = stationColor(name),
        ) {
            when {
                logo != null -> logo()
                logoUrl != null -> AsyncImage(logoUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stationInitials(name), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
                }
            }
        }
        Text(name, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 2x2 mosaic of the first four [imageUrls]; 1-3 URLs show the first one full-size, none shows the placeholder. */
@Composable
fun PlaylistMosaic(imageUrls: List<String>, modifier: Modifier = Modifier) {
    val urls = imageUrls.filter { it.isNotBlank() }.take(4)
    Box(modifier.background(Color(0xFF2E4A7D))) {
        when {
            urls.isEmpty() -> CoverPlaceholder(Modifier.fillMaxSize())
            urls.size < 4 -> AsyncImage(urls[0], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Column(Modifier.fillMaxSize()) {
                for (r in 0..1) Row(Modifier.weight(1f)) {
                    for (c in 0..1) AsyncImage(urls[r * 2 + c], null, Modifier.weight(1f).fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

/** Filled or empty heart (24 dp vector); filled uses the accent by default. */
@Composable
fun FavoriteHeart(
    filled: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = if (filled) SpinroomColors.Accent else SpinroomColors.TextSecondary,
) {
    Icon(
        if (filled) SpinroomIcons.HeartFilled else SpinroomIcons.HeartOutline,
        contentDescription = null, tint = tint, modifier = modifier.size(24.dp),
    )
}

/** Search/list row on glass: [thumbnail] slot (48 dp square), title, subtitle, and a heart when [favorite] is non-null. */
@Composable
fun ResultRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    thumbnail: @Composable () -> Unit = { CoverPlaceholder(Modifier.fillMaxSize()) },
) {
    FocusSurface(
        onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), focusedScale = 1.02f, onLongClick = onLongClick,
        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) { thumbnail() }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (favorite != null) FavoriteHeart(favorite)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 420, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun ResultTilesPreview() {
    SpinroomTheme {
        Box {
            ContentBackdrop(imageUrl = null, seedColor = stationColor("Jazz Jeden"))
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StationTile("Jazz Jeden", "Radio Browser", {})
                    StationTile("Klasyka 24", "TuneIn", {})
                    PlaylistMosaic(emptyList(), Modifier.size(148.dp))
                }
                ResultRow("Nocne światła", "Lena Wiatr · Album", {}, Modifier.width(600.dp), favorite = true)
                GlassSurface(Modifier.width(600.dp)) { Text("Szkło", Modifier.padding(16.dp)) }
            }
        }
    }
}
