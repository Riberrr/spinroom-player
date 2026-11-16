package io.github.riberrr.spinroom.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.LibraryFilter
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme

/** Callbacks of [FilterPanel]; every toggle flips one criterion and the results update at once. */
data class FilterPanelActions(
    val onToggleGenre: (FilterOption) -> Unit = {},
    val onToggleDecade: (Int) -> Unit = {},
    val onToggleSource: (String) -> Unit = {},
    val onToggleFavorites: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/**
 * Sliding "Filtry" panel (opened with D-pad left): Gatunek, Dekada, Źródło, Tylko ulubione, plus "Wyczyść filtry".
 * Public so Search (SUI6) can reuse it; it may move to `:core:design` later. Back closes it, focus cannot leave it.
 * [firstFocus] is attached to the first chip: request it right after the panel is shown.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FilterPanel(
    filter: LibraryFilter,
    genres: List<FilterOption>,
    sources: List<SourceOption>,
    actions: FilterPanelActions,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester = FocusRequester(),
) {
    GlassSurface(
        modifier
            .width(380.dp)
            .fillMaxHeight()
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) { actions.onClose(); true }
                else it.key == Key.Back
            }
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup(),
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        color = SpinroomColors.Surface.copy(alpha = 0.97f),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.headlineSmall, color = SpinroomColors.TextPrimary)

            // "Tylko ulubione" is first: a single big switch, and the natural focus start.
            FilterChip(
                stringResource(R.string.filter_favorites_only), filter.favoriteOnly, actions.onToggleFavorites,
                Modifier.focusRequester(firstFocus),
            )

            if (genres.isNotEmpty()) {
                Section(stringResource(R.string.filter_genre))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.forEach { g -> FilterChip(g.name, filter.genreId == g.id, { actions.onToggleGenre(g) }) }
                }
            }

            Section(stringResource(R.string.filter_decade))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilter.Decades.forEach { d ->
                    FilterChip(stringResource(R.string.filter_decade_format, d), filter.decade == d, { actions.onToggleDecade(d) })
                }
            }

            if (sources.isNotEmpty()) {
                Section(stringResource(R.string.filter_source))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.forEach { s -> FilterChip(s.name, filter.provider == s.domain, { actions.onToggleSource(s.domain) }) }
                }
            }

            if (!filter.isEmpty) {
                FilterChip(stringResource(R.string.filter_clear), false, actions.onClear, Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelMedium, color = SpinroomColors.TextSecondary)
}

/** Pill used in the panel and in the library header: accent fill when [selected], glass otherwise. */
@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        containerColor = if (selected) SpinroomColors.Accent else SpinroomColors.Glass,
        focusedContainerColor = if (selected) SpinroomColors.Accent else SpinroomColors.GlassHigh,
        focusedScale = 1.04f,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false,
            color = if (selected) SpinroomColors.Bg else SpinroomColors.TextPrimary,
        )
    }
}

@Preview(widthDp = 480, heightDp = 720, backgroundColor = 0xFF0B0D12, showBackground = true)
@Composable
private fun FilterPanelPreview() {
    SpinroomTheme {
        FilterPanel(
            filter = LibraryFilter(genreId = "2", decade = 1990),
            genres = listOf(FilterOption("1", "Jazz"), FilterOption("2", "Rock"), FilterOption("3", "Elektronika")),
            sources = listOf(SourceOption("spotify", "Spotify"), SourceOption("filesystem_local", "Pliki")),
            actions = FilterPanelActions(),
        )
    }
}
