package io.github.riberrr.spinroom.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.discovery.DiscoveredServer
import io.github.riberrr.spinroom.feature.settings.latency.LatencyScreen

/** Server address + login. Only the token is stored; the password is cleared after each attempt. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var showLatency by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    if (showAbout) {
        AboutScreen(onBack = { showAbout = false }, modifier = modifier)
        return
    }
    if (showLatency) {
        LatencyScreen(onBack = { showLatency = false }, modifier = modifier)
        return
    }
    Column(
        modifier.fillMaxSize().background(SpinroomColors.Bg).verticalScroll(rememberScrollState())
            .padding(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
        Text(connectionText(ui), color = connectionColor(ui.connection))

        if (!ui.loggedIn) ServerList(ui, viewModel::onPickServer)
        SettingsTextField(
            stringResource(R.string.settings_address),
            ui.address,
            viewModel::onAddress,
            hint = stringResource(R.string.settings_address_hint),
        )
        if (!ui.loggedIn) {
            SettingsTextField(stringResource(R.string.settings_username), ui.username, viewModel::onUsername)
            SettingsTextField(stringResource(R.string.settings_password), ui.password, viewModel::onPassword, secret = true)
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
        Button(stringResource(R.string.latency_entry), onClick = { showLatency = true })
        Button(stringResource(R.string.about_entry), onClick = { showAbout = true })
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


/** Servers found by mDNS; picking one fills the address field. */
@Composable
private fun ServerList(ui: SettingsUiState, onPick: (DiscoveredServer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_servers_title), color = SpinroomColors.TextSecondary)
        when {
            ui.servers.isNotEmpty() -> ui.servers.forEach { server ->
                FocusSurface(onClick = { onPick(server) }, modifier = Modifier.widthIn(min = 240.dp, max = 560.dp)) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(server.name, color = SpinroomColors.TextPrimary)
                        Text(server.address, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ui.scanFinished -> Text(stringResource(R.string.settings_servers_none), color = SpinroomColors.TextSecondary)
            else -> Text(stringResource(R.string.settings_servers_scanning), color = SpinroomColors.TextSecondary)
        }
    }
}
