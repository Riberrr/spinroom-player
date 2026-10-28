package io.github.riberrr.spinroom.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.maapi.MaConnectionState

/** Server address + login. Only the token is stored; the password is cleared after each attempt. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier.fillMaxSize().background(SpinroomColors.Bg).padding(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
        Text(connectionText(ui), color = connectionColor(ui.connection))

        Field(stringResource(R.string.settings_address), ui.address, viewModel::onAddress, hint = "10.0.1.50")
        if (!ui.loggedIn) {
            Field(stringResource(R.string.settings_username), ui.username, viewModel::onUsername)
            Field(stringResource(R.string.settings_password), ui.password, viewModel::onPassword, secret = true)
            Button(
                if (ui.status == LoginStatus.Working) stringResource(R.string.settings_logging_in) else stringResource(R.string.settings_login),
                onClick = viewModel::login,
            )
        } else {
            Button(stringResource(R.string.settings_logout), onClick = viewModel::logout)
        }
        Button(
            stringResource(if (ui.autostart) R.string.settings_autostart_on else R.string.settings_autostart_off),
            onClick = viewModel::onAutostart,
        )
        Text(stringResource(R.string.settings_autostart_hint), color = SpinroomColors.TextSecondary)
        statusText(ui.status)?.let { Text(it, color = if (ui.status is LoginStatus.Done) SpinroomColors.Success else SpinroomColors.TextPrimary) }
    }
}

@Composable
private fun connectionText(ui: SettingsUiState): String = when (val c = ui.connection) {
    is MaConnectionState.Connected -> stringResource(R.string.settings_connected, c.server.serverVersion)
    is MaConnectionState.Connecting, is MaConnectionState.Reconnecting -> stringResource(R.string.settings_connecting)
    is MaConnectionState.Failed -> stringResource(R.string.settings_session_expired)
    MaConnectionState.Disconnected -> stringResource(R.string.settings_disconnected)
}

private fun connectionColor(c: MaConnectionState) = when (c) {
    is MaConnectionState.Connected -> SpinroomColors.Success
    else -> SpinroomColors.TextSecondary
}

@Composable
private fun statusText(s: LoginStatus): String? = when (s) {
    LoginStatus.WrongCredentials -> stringResource(R.string.settings_wrong_credentials)
    LoginStatus.ServerUnreachable -> stringResource(R.string.settings_unreachable)
    is LoginStatus.Done -> stringResource(R.string.settings_done, s.albums)
    else -> null
}

@Composable
private fun Button(label: String, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, modifier = Modifier.widthIn(min = 240.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    secret: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SpinroomColors.TextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = SpinroomColors.TextPrimary),
            cursorBrush = SolidColor(SpinroomColors.Accent),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpinroomColors.Surface)
                .border(
                    BorderStroke(if (focused) 3.dp else 1.dp, if (focused) SpinroomColors.Accent else SpinroomColors.Outline),
                    RoundedCornerShape(10.dp),
                )
                .onFocusChanged { focused = it.isFocused }
                .padding(16.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = SpinroomColors.TextSecondary)
                inner()
            },
        )
    }
}
