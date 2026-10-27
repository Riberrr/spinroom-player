package io.github.riberrr.spinroom.feature.details

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
fun DetailsScreen(itemUri: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(SpinroomColors.Bg), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.details_stub_title) + " " + itemUri, color = SpinroomColors.TextPrimary)
    }
}
