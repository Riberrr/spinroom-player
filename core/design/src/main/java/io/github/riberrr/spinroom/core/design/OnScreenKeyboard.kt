package io.github.riberrr.spinroom.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Texts of the bottom row; the caller passes Polish strings. */
data class KeyboardLabels(val space: String, val delete: String, val symbols: String, val letters: String)

private val LetterKeys = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż".map { it.toString() }
private val SymbolKeys = "1234567890.-'’,&!?:;/()+@#%_*".map { it.toString() }
private const val COLUMNS = 7

/**
 * On-screen keyboard (7 columns of Polish letters or symbols + Spacja / Usuń / 123) on glass keys.
 * Stateless: [resultsTarget] (when non-null) is where → on the last column and ↓ on the bottom row jump to. [symbols] says which page to show. [firstKey], when given, is attached to the first letter so the caller can focus it.
 */
@Composable
fun OnScreenKeyboard(
    symbols: Boolean,
    labels: KeyboardLabels,
    onType: (String) -> Unit,
    onBackspace: () -> Unit,
    onToggleSymbols: () -> Unit,
    modifier: Modifier = Modifier,
    firstKey: FocusRequester? = null,
    resultsTarget: FocusRequester? = null,
) {
    val keys = if (symbols) SymbolKeys else LetterKeys
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(COLUMNS).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { col, key ->
                    val focus = (if (firstKey != null && rowIndex == 0 && col == 0) Modifier.focusRequester(firstKey) else Modifier)
                        .then(if (col == row.size - 1 || col == COLUMNS - 1) Modifier.focusJump(Key.DirectionRight, resultsTarget) else Modifier)
                    KeyboardKey(key, { onType(key) }, Modifier.weight(1f).then(focus))
                }
                repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyboardKey(labels.space, { onType(" ") }, Modifier.weight(3.2f).focusJump(Key.DirectionDown, resultsTarget))
            KeyboardKey(labels.delete, onBackspace, Modifier.weight(2f).focusJump(Key.DirectionDown, resultsTarget))
            KeyboardKey(if (symbols) labels.letters else labels.symbols, onToggleSymbols, Modifier.weight(2f).focusJump(Key.DirectionDown, resultsTarget).focusJump(Key.DirectionRight, resultsTarget))
        }
    }
}

@Composable
private fun KeyboardKey(label: String, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick = onClick, modifier = modifier.height(44.dp), shape = RoundedCornerShape(8.dp), focusedScale = 1.1f,
        containerColor = SpinroomColors.Glass, focusedContainerColor = SpinroomColors.GlassHigh,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = SpinroomColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}
