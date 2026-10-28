package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Clickable surface with the shared focus look: slight scale + accent outline. */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = SpinroomColors.Surface,
    focusedContainerColor: Color = SpinroomColors.SurfaceHigh,
    focusedScale: Float = 1.05f,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = focusedContainerColor,
            contentColor = SpinroomColors.TextPrimary,
            focusedContentColor = SpinroomColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, SpinroomColors.Accent), shape = shape),
        ),
        content = { content() },
    )
}

/** Placeholder cover: tinted square with a music note. Default for [CoverTile] and the mini player. */
@Composable
fun CoverPlaceholder(modifier: Modifier = Modifier, tint: Color = Color(0xFF2E4A7D)) {
    Box(modifier.background(tint), contentAlignment = Alignment.Center) {
        Icon(SpinroomIcons.Music, contentDescription = null, tint = SpinroomColors.TextSecondary, modifier = Modifier.size(40.dp))
    }
}

/**
 * Album/playlist/artist tile: square cover (via [cover] slot, e.g. a Coil AsyncImage) with title and subtitle below.
 * Focus: scale 1.08 + accent outline around the cover.
 */
@Composable
fun CoverTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    onLongClick: (() -> Unit)? = null,
    cover: @Composable () -> Unit = { CoverPlaceholder(Modifier.fillMaxSize()) },
) {
    Column(modifier.width(size)) {
        FocusSurface(onClick, Modifier.size(size), focusedScale = 1.08f, onLongClick = onLongClick) { cover() }
        Text(title, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Section title followed by a horizontally scrolling row. Fill [content] with `item { }` / `items(...)`. */
@Composable
fun SectionRow(
    title: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier) {
        Text(title, Modifier.padding(start = 48.dp, bottom = 12.dp), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

/** Radio station / source chip: small colored badge with [initials], name and provider. */
@Composable
fun StationTile(
    name: String,
    provider: String,
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFF7D3F4A),
    onLongClick: (() -> Unit)? = null,
) {
    FocusSurface(onClick, modifier.width(206.dp), shape = RoundedCornerShape(10.dp), focusedScale = 1.06f, onLongClick = onLongClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(badgeColor, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.labelMedium, color = SpinroomColors.TextPrimary)
            }
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(provider, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 360, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun TilesPreview() {
    SpinroomTheme {
        Column {
            SectionRow("Ostatnio odtwarzane") {
                item { CoverTile("Nocne światła", "Lena Wiatr", {}) }
                item { CoverTile("Port 14", "Kwartet Północ", {}) }
            }
            SectionRow("Ulubione stacje") {
                item { StationTile("Jazz Jeden", "Radio Browser", "J1", {}) }
                item { StationTile("Klasyka 24", "TuneIn", "KL", {}, badgeColor = Color(0xFF2E4A7D)) }
            }
        }
    }
}
