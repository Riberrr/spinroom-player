package io.github.riberrr.spinroom.core.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Collapsed width of the rail; the shell reserves the same slot. */
val RAIL_WIDTH = 72.dp

data class SideMenuItem(val id: String, val label: String, val icon: ImageVector)

/** A pinned favorite (station or playlist) in the expanded menu. [isStation] picks initials-on-color as the fallback thumbnail. */
data class PinnedEntry(val id: String, val title: String, val subtitle: String, val imageUrl: String?, val isStation: Boolean)

/**
 * Left navigation rail. Collapsed it shows only icons (72 dp); while any item has focus it expands
 * to show labels. [selectedId] marks the current screen. Pass [enterFocusRequester] to move focus
 * into the menu from outside (e.g. D-pad left on the screen's left edge). [onFocusChange] reports whether
 * focus is inside the menu. [items] sit at the top, [bottomItems] (e.g. settings) are pinned to the bottom.
 */
@Composable
fun SideMenu(
    items: List<SideMenuItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomItems: List<SideMenuItem> = emptyList(),
    enterFocusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onItemFocus: (String) -> Unit = {},
    pinned: List<PinnedEntry> = emptyList(),
    pinnedTitle: String = "",
    onSelectPinned: (PinnedEntry) -> Unit = {},
) {
    var hasFocus by remember { mutableStateOf(false) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val entering = remember { booleanArrayOf(false) }
    val all = items + bottomItems
    val targetId = all.firstOrNull { it.id == selectedId }?.id ?: all.firstOrNull()?.id
    val width by animateDpAsState(if (hasFocus) 232.dp else RAIL_WIDTH, label = "menuWidth")
    // The layout slot stays RAIL_WIDTH; the expanded rail overlays the content instead of squeezing it.
    Box(modifier.fillMaxHeight().width(RAIL_WIDTH).zIndex(1f)) {
        Column(
            Modifier
                .fillMaxHeight()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .width(width)
                .background(SpinroomColors.Bg.copy(alpha = if (hasFocus) 0.94f else 0.88f))
                .background(SpinroomColors.Glass)
                .drawBehind { drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(size.width - 1.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height)) }
                .onFocusChanged {
                    hasFocus = it.hasFocus
                    onFocusChange(it.hasFocus)
                }
                .then(if (enterFocusRequester != null) Modifier.focusRequester(enterFocusRequester) else Modifier)
                .focusProperties {
                    onEnter = {
                        // requestFocus on a child re-triggers onEnter: guard against recursion.
                        if (!entering[0]) {
                            entering[0] = true
                            try { targetId?.let { requesters[it]?.requestFocus() } } finally { entering[0] = false }
                        }
                    }
                }
                .focusGroup()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                MenuEntry(item, item.id == selectedId, hasFocus, { onSelect(item.id) }, Modifier.focusRequester(requesters.getOrPut(item.id) { FocusRequester() }).onFocusChanged { if (it.isFocused) onItemFocus(item.id) })
            }
            if (hasFocus && pinned.isNotEmpty()) {
                Text(
                    pinnedTitle.uppercase(), Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelMedium, color = SpinroomColors.TextSecondary, maxLines = 1,
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    pinned.forEach { entry -> PinnedRow(entry) { onSelectPinned(entry) } }
                }
            }
            Box(Modifier.weight(1f))
            bottomItems.forEach { item ->
                MenuEntry(item, item.id == selectedId, hasFocus, { onSelect(item.id) }, Modifier.focusRequester(requesters.getOrPut(item.id) { FocusRequester() }).onFocusChanged { if (it.isFocused) onItemFocus(item.id) })
            }
        }
    }
}

@Composable
private fun PinnedRow(entry: PinnedEntry, onClick: () -> Unit) {
    FocusSurface(
        onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        containerColor = Color.Transparent, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.0f,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryThumb(entry.title, entry.imageUrl, entry.isStation, Modifier.size(30.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = SpinroomColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MenuEntry(item: SideMenuItem, selected: Boolean, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick,
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        containerColor = if (selected) SpinroomColors.Accent.copy(alpha = 0.16f) else Color.Transparent,
        focusedContainerColor = if (selected) SpinroomColors.Accent.copy(alpha = 0.28f) else SpinroomColors.GlassHigh,
        focusedScale = 1.0f,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, item.label, Modifier.size(26.dp), tint = if (selected) SpinroomColors.Accent else SpinroomColors.TextSecondary)
            if (expanded) {
                Text(item.label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleSmall, color = if (selected) SpinroomColors.Accent else SpinroomColors.TextPrimary, maxLines = 1)
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 560, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun SideMenuPreview() {
    SpinroomTheme {
        SideMenu(
            listOf(SideMenuItem("home", "Start", SpinroomIcons.Home), SideMenuItem("lib", "Biblioteka", SpinroomIcons.Library)),
            "home", {}, bottomItems = listOf(SideMenuItem("set", "Ustawienia", SpinroomIcons.Settings)),
        )
    }
}
