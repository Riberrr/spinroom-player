package io.github.riberrr.spinroom.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.riberrr.spinroom.core.design.SpinroomColors

/** Stub (SUI0). Replaced by the real playlists screen in SUI2; keep this signature. */
@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    Box(
        modifier.fillMaxSize().background(SpinroomColors.Bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.playlists_title), color = SpinroomColors.TextPrimary)
    }
}
