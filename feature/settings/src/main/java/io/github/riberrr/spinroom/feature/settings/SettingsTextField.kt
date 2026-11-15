package io.github.riberrr.spinroom.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.riberrr.spinroom.core.design.SpinroomColors

/**
 * Text field for the remote: D-pad focus only highlights it, OK starts editing (and opens the
 * on-screen keyboard). A hardware keyboard can type straight away: the first character starts
 * editing. Back, Done or D-pad up/down end editing.
 */
@Composable
internal fun SettingsTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    secret: Boolean = false,
    /** Keyboard layout for the on-screen keyboard; the address field uses Uri (no spaces, dots and slashes at hand). */
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var editing by remember { mutableStateOf(false) }
    // Only OK opens the on-screen keyboard; typing on a hardware keyboard should not cover the screen.
    var showKeyboard by remember { mutableStateOf(false) }
    // The key that started editing: its KeyUp must not reach the text field (it would open the keyboard).
    var startKey by remember { mutableStateOf<Key?>(null) }
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    val shell = remember { FocusRequester() }
    val input = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun finish(move: FocusDirection? = null) {
        editing = false
        keyboard?.hide()
        shell.requestFocus()
        move?.let(focusManager::moveFocus)
    }

    LaunchedEffect(editing) {
        if (editing) {
            input.requestFocus()
            if (showKeyboard) keyboard?.show()
        }
    }

    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SpinroomColors.TextSecondary)
        Box(
            Modifier
                .fillMaxWidth()
                .focusRequester(shell)
                .onFocusChanged { focused = it.hasFocus }
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyUp && e.key == startKey) { startKey = null; return@onPreviewKeyEvent true }
                    if (editing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        e.isConfirm() -> { startKey = e.key; showKeyboard = true; editing = true; true }
                        e.typedChar() != null -> { startKey = e.key; onChange(value + e.typedChar()); showKeyboard = false; editing = true; true }
                        else -> false
                    }
                }
                .focusable()
                .clip(RoundedCornerShape(10.dp))
                .background(SpinroomColors.Surface)
                .border(
                    BorderStroke(if (focused) 3.dp else 1.dp, if (focused) SpinroomColors.Accent else SpinroomColors.Outline),
                    RoundedCornerShape(10.dp),
                )
                .padding(16.dp),
        ) {
            BasicTextField(
                value = field,
                onValueChange = { field = it; onChange(it.text) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SpinroomColors.TextPrimary),
                cursorBrush = SolidColor(if (editing) SpinroomColors.Accent else SpinroomColors.Surface),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (secret) KeyboardType.Password else keyboardType,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                    showKeyboardOnFocus = false,
                ),
                keyboardActions = KeyboardActions(onDone = { finish() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(input)
                    .focusProperties { canFocus = editing }
                    .onFocusChanged { if (!it.isFocused && editing) editing = false }
                    .onPreviewKeyEvent { e ->
                        if (!editing) return@onPreviewKeyEvent false
                        when (e.key) {
                            // Consume both down and up so Back does not also leave the screen.
                            Key.Back, Key.Escape -> { if (e.type == KeyEventType.KeyUp) finish(); true }
                            Key.DirectionDown -> { if (e.type == KeyEventType.KeyDown) finish(FocusDirection.Down); true }
                            Key.DirectionUp -> { if (e.type == KeyEventType.KeyDown) finish(FocusDirection.Up); true }
                            else -> false
                        }
                    },
                decorationBox = { inner ->
                    if (field.text.isEmpty() && hint.isNotEmpty()) Text(hint, color = SpinroomColors.TextSecondary)
                    inner()
                },
            )
        }
    }
}

private fun KeyEvent.isConfirm() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

/** The character a hardware keyboard typed, or null for navigation/control keys. */
private fun KeyEvent.typedChar(): Char? {
    if (isCtrlPressed || isAltPressed) return null
    val cp = utf16CodePoint
    return if (cp >= 32 && cp != 127 && cp <= Char.MAX_VALUE.code) cp.toChar() else null
}
