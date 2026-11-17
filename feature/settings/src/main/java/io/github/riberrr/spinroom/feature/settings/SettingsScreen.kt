package io.github.riberrr.spinroom.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
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
    var category by rememberSaveable { mutableStateOf(SettingsCategory.Connection) }
    var showName by rememberSaveable { mutableStateOf(false) }
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
    Row(
        modifier.fillMaxSize().background(SpinroomColors.Bg).padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = SpinroomColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            SettingsCategory.entries.forEach { c ->
                CategoryItem(c, selected = c == category, onFocus = { category = c })
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(category.title), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            when (category) {
                SettingsCategory.Connection -> ConnectionSection(ui, viewModel, logoutFocus, onRename = { showName = true })
                SettingsCategory.Playback -> {
                    Button(
                        stringResource(if (ui.autostart) R.string.settings_autostart_on else R.string.settings_autostart_off),
                        onClick = viewModel::onAutostart,
                    )
                    Text(stringResource(R.string.settings_autostart_hint), color = SpinroomColors.TextSecondary)
                    Button(stringResource(R.string.latency_entry), onClick = { showLatency = true })
                }
                SettingsCategory.Appearance -> AppearanceSection(ui, viewModel)
                SettingsCategory.Remote -> Text(stringResource(R.string.remote_soon), color = SpinroomColors.TextSecondary)
                SettingsCategory.Home -> HomeRowsSection(ui.homeLayout, viewModel)
                SettingsCategory.About -> Button(stringResource(R.string.about_open), onClick = { showAbout = true })
            }
        }
    }
    if (showName) PlayerNameDialog(ui.playerName.orEmpty(), onSave = viewModel::renamePlayer, onDismiss = { showName = false })
}

enum class SettingsCategory(val title: Int, val subtitle: Int) {
    Connection(R.string.cat_connection, R.string.cat_connection_sub),
    Playback(R.string.cat_playback, R.string.cat_playback_sub),
    Appearance(R.string.cat_appearance, R.string.cat_appearance_sub),
    Remote(R.string.cat_remote, R.string.cat_remote_sub),
    Home(R.string.cat_home, R.string.cat_home_sub),
    About(R.string.cat_about, R.string.cat_about_sub),
}

/** Left-hand category; focusing it shows its content on the right. */
@Composable
private fun CategoryItem(c: SettingsCategory, selected: Boolean, onFocus: () -> Unit) {
    FocusSurface(
        onClick = onFocus,
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.hasFocus) onFocus() },
        focusedScale = 1.02f,
        containerColor = if (selected) SpinroomColors.GlassHigh else SpinroomColors.Glass,
        focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(stringResource(c.title), color = if (selected) SpinroomColors.Accent else SpinroomColors.TextPrimary)
            Text(stringResource(c.subtitle), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Server, login, TV player name and the MA player list (everything that used to be on the single page). */
@Composable
private fun ConnectionSection(ui: SettingsUiState, viewModel: SettingsViewModel, logoutFocus: FocusRequester, onRename: () -> Unit) {
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
    }
    FocusSurface(
        onClick = onRename,
        modifier = Modifier.widthIn(min = 240.dp, max = 560.dp),
        containerColor = SpinroomColors.Glass,
        focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.name_row_title), color = SpinroomColors.TextPrimary)
                Text(
                    ui.playerName ?: stringResource(R.string.name_row_default),
                    color = SpinroomColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(stringResource(R.string.name_row_change), color = SpinroomColors.TextSecondary)
        }
    }
    if (ui.loggedIn) PlayerList(ui, viewModel::onPickPlayer)
    statusText(ui.status)?.let { Text(it, color = if (ui.status is LoginStatus.Done) SpinroomColors.Success else SpinroomColors.TextPrimary) }
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
internal fun Button(label: String, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
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
