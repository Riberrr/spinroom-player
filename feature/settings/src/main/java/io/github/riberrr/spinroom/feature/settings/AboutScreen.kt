package io.github.riberrr.spinroom.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors

/** Settings sub-screen: app version, unofficial-client note and open-source licenses. */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    Column(
        modifier.fillMaxSize().background(SpinroomColors.Bg).verticalScroll(rememberScrollState())
            .padding(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
        Text(stringResource(R.string.about_version, version), color = SpinroomColors.TextPrimary)
        Text(stringResource(R.string.about_unofficial), color = SpinroomColors.TextSecondary, modifier = Modifier.widthIn(max = 760.dp))
        Text(stringResource(R.string.about_licenses_title), style = MaterialTheme.typography.titleMedium, color = SpinroomColors.TextPrimary)
        Text(stringResource(R.string.about_licenses), color = SpinroomColors.TextSecondary, modifier = Modifier.widthIn(max = 760.dp))
        FocusSurface(onClick = onBack, modifier = Modifier.widthIn(min = 240.dp)) {
            Text(stringResource(R.string.about_back), modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
        }
    }
}
