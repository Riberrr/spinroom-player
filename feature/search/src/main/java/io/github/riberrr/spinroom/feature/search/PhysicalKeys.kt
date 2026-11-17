package io.github.riberrr.spinroom.feature.search

/** What a hardware-keyboard key press means for the query field. */
sealed interface PhysicalInput {
    data class Text(val text: String) : PhysicalInput
    data object Backspace : PhysicalInput
    data object Submit : PhysicalInput
}

private const val PUNCTUATION = " .-'’,&!?:;/()+@#%_*"

/**
 * Maps a key-down from a USB/Bluetooth keyboard to query input. [codePoint] comes from `KeyEvent.utf16CodePoint`;
 * it is 0 for D-pad and media keys, so those return null and keep navigating. Letters are lower-cased like the on-screen keyboard.
 */
fun mapPhysicalKey(isBackspace: Boolean, isEnter: Boolean, codePoint: Int, hasModifier: Boolean): PhysicalInput? = when {
    isBackspace -> PhysicalInput.Backspace
    isEnter -> PhysicalInput.Submit
    hasModifier || codePoint <= 0 || !Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint) -> null
    Character.isLetterOrDigit(codePoint) || (codePoint < 0x10000 && PUNCTUATION.indexOf(codePoint.toChar()) >= 0) ->
        PhysicalInput.Text(String(Character.toChars(codePoint)).lowercase())
    else -> null
}
