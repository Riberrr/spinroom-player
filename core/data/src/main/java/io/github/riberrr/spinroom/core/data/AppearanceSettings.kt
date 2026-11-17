package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Look of the app. Ids are stable strings; `:core:design` maps them to colors. Unknown/missing values fall back to defaults. */
data class Appearance(
    val theme: String = THEME_GRAFIT,
    val accent: String = ACCENT_AMBER,
    /** true = plain theme background instead of the blurred content backdrop. */
    val solidBackground: Boolean = false,
    /** Now Playing background: [BG_BLUR] (blurred cover), [BG_COLOR] (gradient of the cover color) or [BG_SOLID]. */
    val playerBackground: String = BG_BLUR,
    /** Screensaver after idle; off by default. */
    val screensaver: Boolean = false,
) {
    companion object {
        const val THEME_GRAFIT = "grafit"
        const val THEME_GRANAT = "granat"
        const val THEME_OLED = "oled"
        val THEMES = listOf(THEME_GRAFIT, THEME_GRANAT, THEME_OLED)

        const val BG_BLUR = "blur"
        const val BG_COLOR = "color"
        const val BG_SOLID = "solid"
        val PLAYER_BACKGROUNDS = listOf(BG_BLUR, BG_COLOR, BG_SOLID)

        const val ACCENT_AMBER = "amber"
        const val ACCENT_MINT = "mint"
        const val ACCENT_SKY = "sky"
        const val ACCENT_CORAL = "coral"
        const val ACCENT_LAVENDER = "lavender"
        val ACCENTS = listOf(ACCENT_AMBER, ACCENT_MINT, ACCENT_SKY, ACCENT_CORAL, ACCENT_LAVENDER)
    }
}

/** Pure mapping so it can be unit-tested without DataStore. */
internal fun appearanceFrom(prefs: Preferences): Appearance {
    val d = Appearance()
    return Appearance(
        theme = prefs[THEME]?.takeIf { it in Appearance.THEMES } ?: d.theme,
        accent = prefs[ACCENT]?.takeIf { it in Appearance.ACCENTS } ?: d.accent,
        solidBackground = prefs[SOLID_BG] ?: d.solidBackground,
        playerBackground = prefs[PLAYER_BG]?.takeIf { it in Appearance.PLAYER_BACKGROUNDS } ?: d.playerBackground,
        screensaver = prefs[SCREENSAVER] ?: d.screensaver,
    )
}

private val THEME = stringPreferencesKey("appearance_theme")
private val ACCENT = stringPreferencesKey("appearance_accent")
private val SOLID_BG = booleanPreferencesKey("appearance_solid_bg")
private val PLAYER_BG = stringPreferencesKey("appearance_player_bg")
private val SCREENSAVER =booleanPreferencesKey("appearance_screensaver")

/** Theme, accent, background and screensaver preferences (UI in SUI8a). */
class AppearanceSettings(private val store: DataStore<Preferences>) {
    val appearance: Flow<Appearance> = store.data.map(::appearanceFrom).distinctUntilChanged()

    suspend fun setTheme(id: String) { if (id in Appearance.THEMES) store.edit { it[THEME] = id } }
    suspend fun setAccent(id: String) { if (id in Appearance.ACCENTS) store.edit { it[ACCENT] = id } }
    suspend fun setSolidBackground(value: Boolean) { store.edit { it[SOLID_BG] = value } }
    suspend fun setPlayerBackground(id: String) { if (id in Appearance.PLAYER_BACKGROUNDS) store.edit { it[PLAYER_BG] = id } }
    suspend fun setScreensaver(value: Boolean) { store.edit { it[SCREENSAVER] = value } }
}
