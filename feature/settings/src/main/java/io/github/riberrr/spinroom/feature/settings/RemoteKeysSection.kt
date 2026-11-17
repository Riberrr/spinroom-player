package io.github.riberrr.spinroom.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.riberrr.spinroom.core.data.GlobalKeysSupport
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.data.KeyAction
import io.github.riberrr.spinroom.core.data.KeyAssignment
import io.github.riberrr.spinroom.core.data.KeyGestureDetector
import io.github.riberrr.spinroom.core.data.KeyRules
import io.github.riberrr.spinroom.core.data.STATION_RANGE
import io.github.riberrr.spinroom.core.design.FocusSurface
import io.github.riberrr.spinroom.core.design.GlassSurface
import io.github.riberrr.spinroom.core.design.SpinroomColors

/** Where "Dodaj skrót" is: not started, choosing the action, or waiting for a remote key. */
private sealed interface AddStep {
    data object Pick : AddStep
    data class Capture(val action: KeyAction, val arg: Int) : AddStep

    /** A key was caught but needs a decision: a warning and/or a shortcut it would replace. */
    data class Confirm(val candidate: KeyAssignment, val warn: Boolean, val replaces: KeyAssignment?) : AddStep
}

private val ActionLabels = mapOf(
    KeyAction.NEXT to R.string.action_next,
    KeyAction.PREVIOUS to R.string.action_previous,
    KeyAction.PLAY_PAUSE to R.string.action_play_pause,
    KeyAction.OPEN_DRAWER to R.string.action_drawer,
    KeyAction.FAVORITE to R.string.action_favorite,
    KeyAction.OPEN_RADIO to R.string.action_radio,
    KeyAction.OPEN_PLAYLISTS to R.string.action_playlists,
    KeyAction.OPEN_SEARCH to R.string.action_search,
)

private val KeyNames = mapOf(
    23 to R.string.key_ok, 66 to R.string.key_ok, 160 to R.string.key_ok,
    85 to R.string.key_play_pause, 126 to R.string.key_play, 127 to R.string.key_pause, 86 to R.string.key_stop,
    87 to R.string.key_media_next, 88 to R.string.key_media_previous, 89 to R.string.key_rewind, 90 to R.string.key_forward,
    166 to R.string.key_ch_up, 167 to R.string.key_ch_down,
    183 to R.string.key_red, 184 to R.string.key_green, 185 to R.string.key_yellow, 186 to R.string.key_blue,
    82 to R.string.key_menu, 84 to R.string.key_search, 165 to R.string.key_info, 172 to R.string.key_guide,
    170 to R.string.key_tv, 176 to R.string.key_settings, 178 to R.string.key_input,
)

@Composable
private fun keyName(code: Int): String = when {
    code in 7..16 -> (code - 7).toString()
    else -> KeyNames[code]?.let { stringResource(it) } ?: stringResource(R.string.key_other, code)
}

@Composable
private fun gestureName(code: Int, long: Boolean): String =
    if (long) stringResource(R.string.key_long, keyName(code)) else keyName(code)

@Composable
private fun actionName(a: KeyAssignment): String =
    if (a.action == KeyAction.STATION) stringResource(R.string.action_station, a.arg) else stringResource(ActionLabels.getValue(a.action))

/** SUI8b "Przyciski pilota": the shortcut list, "Dodaj skrót" (pick action -> press a key) and the future "outside the app" switch. */
@Composable
internal fun RemoteKeysSection(ui: SettingsUiState, vm: SettingsViewModel) {
    var step by remember { mutableStateOf<AddStep?>(null) }
    var edited by remember { mutableStateOf<KeyAssignment?>(null) }
    // The activity ignores shortcuts only while a key is being learned.
    val capturing = step is AddStep.Capture
    DisposableEffect(capturing) {
        vm.setKeyCapture(capturing)
        onDispose { vm.setKeyCapture(false) }
    }
    when (val s = step) {
        is AddStep.Capture -> CaptureStep(
            onGesture = { code, long ->
                val candidate = KeyAssignment(code, long, s.action, s.arg)
                step = AddStep.Confirm(candidate, KeyRules.classify(code, long) == KeyRules.Verdict.Warn, ui.keys.collidingWith(candidate))
                    .takeIf { it.warn || it.replaces != null }
                    ?: run { vm.addKey(candidate); null }
            },
            onCancel = { step = null },
            blockedCheck = { code, long -> KeyRules.classify(code, long) == KeyRules.Verdict.Blocked },
        )
        else -> {
            Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlobalSwitchRow(ui, vm)
                ui.keys.items.forEach { a -> KeyRow(a, onClick = { edited = a }) }
                AddRow(onClick = { step = AddStep.Pick })
                if (ui.keys.items != io.github.riberrr.spinroom.core.data.KeyAssignments.DEFAULTS) {
                    Chip(stringResource(R.string.keys_reset), false, vm::resetKeys)
                }
            }
        }
    }
    edited?.let { a ->
        EditDialog(a, onToggleScope = { vm.toggleKeyScope(a); edited = null }, onRemove = { vm.removeKey(a); edited = null }, onDismiss = { edited = null })
    }
    when (val s = step) {
        AddStep.Pick -> PickActionDialog(
            onPick = { action, arg -> step = AddStep.Capture(action, arg) },
            onDismiss = { step = null },
        )
        is AddStep.Confirm -> ConfirmDialog(
            s,
            onUse = { vm.addKey(s.candidate); step = null },
            onOther = { step = AddStep.Capture(s.candidate.action, s.candidate.arg) },
            onDismiss = { step = null },
        )
        else -> Unit
    }
}

/** True when the SUI9 accessibility service is switched on in the system settings. */
private fun serviceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    val me = ComponentName(context.packageName, GlobalKeysSupport.SERVICE_CLASS)
    return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
}

/** Opens the system accessibility settings (TVs differ: fall back to the main settings screen). */
private fun openAccessibilitySettings(context: Context) {
    val intents = listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    for (i in intents) if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
}

/** "Działaj poza aplikacją" (SUI9): the preference plus the system switch of the accessibility service, re-read on return. */
@Composable
private fun GlobalSwitchRow(ui: SettingsUiState, vm: SettingsViewModel) {
    if (!GlobalKeysSupport.available) return
    val context = LocalContext.current
    var serviceOn by remember { mutableStateOf(serviceEnabled(context)) }
    var showHelp by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { serviceOn = serviceEnabled(context) }
    val active = ui.keysGlobal && serviceOn
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusSurface(
            onClick = {
                if (active) {
                    vm.setKeysGlobal(false); showHelp = false
                } else {
                    vm.setKeysGlobal(true)
                    if (!serviceOn) { showHelp = true; openAccessibilitySettings(context) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = SpinroomColors.Glass,
            focusedContainerColor = SpinroomColors.GlassHigh,
            focusedScale = 1.01f,
        ) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.keys_global_title), color = SpinroomColors.TextPrimary)
                    Text(stringResource(R.string.keys_global_hint), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Pill(
                    stringResource(
                        when {
                            active -> R.string.keys_global_on
                            ui.keysGlobal -> R.string.keys_global_needs_service
                            else -> R.string.keys_global_off
                        },
                    ),
                    highlighted = active,
                )
            }
        }
        if (showHelp || (ui.keysGlobal && !serviceOn)) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.keys_global_help),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    color = SpinroomColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Pill(text: String, highlighted: Boolean) {
    Box(
        Modifier.clip(CircleShape).border(BorderStroke(1.dp, SpinroomColors.GlassBorder), CircleShape)
            .background(if (highlighted) SpinroomColors.GlassHigh else SpinroomColors.Glass),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (highlighted) SpinroomColors.Success else SpinroomColors.TextSecondary,
        )
    }
}

@Composable
private fun KeyRow(a: KeyAssignment, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, modifier = Modifier.fillMaxWidth(), containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.01f) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.width(150.dp)) { Pill(gestureName(a.keyCode, a.long), highlighted = false) }
            Text(actionName(a), modifier = Modifier.weight(1f), color = SpinroomColors.TextPrimary)
            Pill(stringResource(if (a.global) R.string.keys_scope_global else R.string.keys_scope_app), highlighted = a.global)
        }
    }
}

@Composable
private fun AddRow(onClick: () -> Unit) {
    FocusSurface(onClick = onClick, modifier = Modifier.fillMaxWidth(), containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh, focusedScale = 1.01f) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("+", color = SpinroomColors.Accent)
            Text(stringResource(R.string.keys_add), color = SpinroomColors.TextSecondary)
        }
    }
}

/** Waits for a remote key. Tap = short, hold = long. Back cancels; keys that can't be assigned just explain why. */
@Composable
private fun CaptureStep(onGesture: (Int, Boolean) -> Unit, onCancel: () -> Unit, blockedCheck: (Int, Boolean) -> Boolean) {
    val focus = remember { FocusRequester() }
    val detector = remember { KeyGestureDetector() }
    var blocked by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    GlassSurface(
        Modifier
            .widthIn(max = 760.dp).fillMaxWidth().heightIn(min = 240.dp)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val n = event.nativeKeyEvent
                val down = event.type == KeyEventType.KeyDown
                if (event.type != KeyEventType.KeyDown && event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (n.keyCode == KeyRules.KEYCODE_BACK) {
                    if (!down) onCancel()
                    return@onPreviewKeyEvent true
                }
                detector.onKey(n.keyCode, down, n.repeatCount)?.let { g ->
                    if (blockedCheck(g.keyCode, g.long)) blocked = g.keyCode else onGesture(g.keyCode, g.long)
                }
                true
            },
    ) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.keys_press_title), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            Text(stringResource(R.string.keys_press_hint), color = SpinroomColors.TextSecondary)
            blocked?.let {
                Text(stringResource(R.string.keys_blocked, keyName(it)), color = MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.keys_press_cancel), color = SpinroomColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DialogFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.width(640.dp).heightIn(max = 560.dp).clip(RoundedCornerShape(20.dp)).background(SpinroomColors.SurfaceHigh)
                .border(BorderStroke(1.dp, SpinroomColors.GlassBorder), RoundedCornerShape(20.dp)),
        ) { content() }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PickActionDialog(onPick: (KeyAction, Int) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    DialogFrame(onDismiss) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.keys_pick_title), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            ActionLabels.entries.forEachIndexed { i, (action, label) ->
                FocusSurface(
                    onClick = { onPick(action, 0) },
                    modifier = Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(first) else Modifier),
                    containerColor = SpinroomColors.Glass,
                    focusedContainerColor = SpinroomColors.GlassHigh,
                    focusedScale = 1.01f,
                ) { Text(stringResource(label), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            }
            Text(stringResource(R.string.keys_pick_station), color = SpinroomColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                STATION_RANGE.forEach { n -> Chip(n.toString(), false, { onPick(KeyAction.STATION, n) }) }
            }
        }
    }
}

@Composable
private fun EditDialog(a: KeyAssignment, onToggleScope: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    DialogFrame(onDismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${gestureName(a.keyCode, a.long)} — ${actionName(a)}", style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            Button(
                stringResource(if (a.global) R.string.keys_scope_make_app else R.string.keys_scope_make_global),
                onClick = onToggleScope,
                focusRequester = first,
            )
            Button(stringResource(R.string.keys_remove), onClick = onRemove)
            Button(stringResource(R.string.keys_cancel), onClick = onDismiss)
        }
    }
}

@Composable
private fun ConfirmDialog(s: AddStep.Confirm, onUse: () -> Unit, onOther: () -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val c = s.candidate
    DialogFrame(onDismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(gestureName(c.keyCode, c.long), style = MaterialTheme.typography.titleLarge, color = SpinroomColors.TextPrimary)
            if (s.warn) Text(stringResource(R.string.keys_warn_tv), color = SpinroomColors.TextSecondary)
            s.replaces?.let { Text(stringResource(R.string.keys_replaces, actionName(it)), color = SpinroomColors.TextSecondary) }
            Button(stringResource(R.string.keys_use_anyway), onClick = onUse, focusRequester = first)
            Button(stringResource(R.string.keys_other_key), onClick = onOther)
            Button(stringResource(R.string.keys_cancel), onClick = onDismiss)
        }
    }
}
