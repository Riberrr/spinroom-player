package io.github.riberrr.spinroom.core.design

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

data class SideMenuItem(val id: String, val label: String, val icon: ImageVector)

/**
 * Left navigation rail. Collapsed it shows only icons (88 dp); while any item has focus it expands
 * to show labels. [selectedId] marks the current screen. Pass [firstItemFocusRequester] to move focus
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
    firstItemFocusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
) {
    var hasFocus by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxHeight()
            .width(if (hasFocus) 240.dp else 88.dp)
            .animateContentSize()
            .background(SpinroomColors.Surface)
            .onFocusChanged {
                hasFocus = it.hasFocus
                onFocusChange(it.hasFocus)
            }
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { i, item ->
            val requester = if (i == 0 && firstItemFocusRequester != null) Modifier.focusRequester(firstItemFocusRequester) else Modifier
            MenuEntry(item, item.id == selectedId, hasFocus, { onSelect(item.id) }, requester)
        }
        Box(Modifier.weight(1f))
        bottomItems.forEach { item -> MenuEntry(item, item.id == selectedId, hasFocus, { onSelect(item.id) }, Modifier) }
    }
}

@Composable
private fun MenuEntry(item: SideMenuItem, selected: Boolean, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick,
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        containerColor = if (selected) SpinroomColors.SurfaceHigh else Color.Transparent,
        focusedContainerColor = SpinroomColors.SurfaceHigh,
        focusedScale = 1.0f,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, item.label, Modifier.size(28.dp), tint = if (selected) SpinroomColors.Accent else SpinroomColors.TextSecondary)
            if (expanded) {
                Text(item.label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1)
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
