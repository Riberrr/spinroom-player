package io.github.riberrr.spinroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpinroomTheme { HelloScreen() } }
    }
}

/** Placeholder screen from S0: proves the build, theme and launcher entry work on the TV. */
@Composable
private fun HelloScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(SpinroomColors.Bg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(160.dp))
        Text(stringResource(R.string.app_full_name), color = SpinroomColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.hello_subtitle), color = SpinroomColors.TextSecondary, fontSize = 18.sp)
        Spacer(Modifier.height(32.dp))
        Text(
            "v${BuildConfig.VERSION_NAME} · ${stringResource(R.string.unofficial_note)}",
            color = SpinroomColors.TextSecondary,
            fontSize = 14.sp,
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HelloScreenPreview() = SpinroomTheme { HelloScreen() }
