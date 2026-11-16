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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.maapi.MaConnectionState
import io.github.riberrr.spinroom.core.maapi.Player
import io.github.riberrr.spinroom.core.maapi.discovery.DiscoveredServer
import io.github.riberrr.spinroom.feature.settings.latency.LatencyScreen

/** Server address + login. Only the token is stored; the password is cleared after each attempt. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var showLatency by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val logoutFocus = remember { FocusRequester() }
    var prevLoggedIn by remember { mutableStateOf(ui.loggedIn) }
    LaunchedEffect(ui.loggedIn) {
        // Only on the transition (login just succeeded), not when the screen opens while logged in.
        if (ui.loggedIn && !prevLoggedIn) runCatching { logoutFocus.requestFocus() }
        prevLoggedIn = ui.loggedIn
    }
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
            keyboardType = KeyboardType.Uri,
        )
        if (!ui.loggedIn) {
            SettingsTextField(stringResource(R.string.settings_username), ui.username, viewModel::onUsername)
            SettingsTextField(stringResource(R.string.settings_password), ui.password, viewModel::onPassword, secret = true)
            Button(
                if (ui.status == LoginStatus.Working) stringResource(R.string.settings_logging_in) else stringResource(R.string.settings_login),
                onClick = viewModel::login,
            )
        } else {
            // "Zaloguj" is gone once logged in: move focus to "Wyloguj" instead of letting it fall off the screen.
            Button(stringResource(R.string.settings_logout), onClick = viewModel::logout, focusRequester = logoutFocus)
            PlayerList(ui, viewModel::onPickPlayer)
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
private fun Button(label: String, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 240.dp).then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
    ) {
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

/** "model · address" from MA's device info; null when MA sends neither. */
internal fun playerDetail(player: Player, shortId: String? = null): String? {
    val info = player.deviceInfo
    // A built-in Chromecast shares the TV's name and model: label it as such instead of showing a bare id.
    val first = if (info?.isChromecast == true) "Chromecast" else info?.model
    return listOfNotNull(first, info?.address, shortId?.let { "#$it" })
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" · ")
        .ifEmpty { null }
}

/** MA players; the chosen one is where Play sends music (the TV's own player is picked automatically). */
@Composable
private fun PlayerList(ui: SettingsUiState, onPick: (Player) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_player_title), color = SpinroomColors.TextSecondary)
        val rows = remember(ui.players, ui.ownPlayerId, ui.playerId) { buildPlayerRows(ui.players, ui.ownPlayerId, ui.playerId, android.os.Build.MODEL) }
        if (rows.isEmpty()) Text(stringResource(R.string.settings_player_none), color = SpinroomColors.TextSecondary)
        rows.forEach { row ->
            val player = row.player
            val note = listOfNotNull(
                if (row.isOwn) stringResource(R.string.settings_player_own) else null,
                when {
                    row.isSelected -> stringResource(R.string.settings_player_selected)
                    !player.available -> stringResource(R.string.settings_player_unavailable)
                    else -> null
                },
            ).joinToString(" · ").ifEmpty { null }
            FocusSurface(onClick = { onPick(player) }, modifier = Modifier.widthIn(min = 240.dp, max = 560.dp)) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(player.name.ifBlank { player.playerId }, color = SpinroomColors.TextPrimary)
                    // Second line tells apart players with the same name (e.g. two "Telewizor"), from MA's device info.
                    playerDetail(player, row.shortId)?.let {
                        Text(it, color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    note?.let {
                        Text(
                            it,
                            color = if (row.isSelected) SpinroomColors.Success else SpinroomColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
