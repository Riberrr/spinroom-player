package io.github.riberrr.spinroom.feature.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.SpinroomColors

/** Placeholder until the real screen is built. */
@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.sources_stub_title), color = SpinroomColors.TextPrimary)
    }
}
