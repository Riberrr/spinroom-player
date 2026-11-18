package io.github.riberrr.spinroom.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradientLogicTest {
    @Test fun gradientIsDeterministicAndCaseInsensitive() {
        assertEquals(gradientColors("Jazz Jeden"), gradientColors("  jazz jeden "))
        assertEquals(gradientIndex("Radio 357"), gradientIndex("Radio 357"))
    }

    @Test fun gradientIndexStaysInPaletteAndSpreads() {
        val names = listOf("Jazz Jeden", "Klasyka 24", "Lo-fi Studio", "Rock Wieczorem", "Info Non-Stop", "Spokojne Fale", "Radio 357", "Nowy Świat", "", "ąęś")
        names.forEach { assertTrue(gradientIndex(it) in 0 until 8) }
        assertTrue(names.map { gradientIndex(it) }.toSet().size >= 4)
    }

    @Test fun gradientColorsAreDark() {
        (0 until 50).forEach { i ->
            val (a, b) = gradientColors("name$i")
            assertTrue(a.red + a.green + a.blue < 1.5f && b.red + b.green + b.blue < 1.5f)
        }
    }

    @Test fun backdropGradientDefaultsAndFollowsSeed() {
        assertEquals(backdropGradient(null), backdropGradient(null))
        assertEquals(backdropGradient(stationColor("A")), backdropGradient(stationColor("A")))
        assertNotEquals(backdropGradient(Color.Red).first, Color.Unspecified)
    }

    @Test fun acronymTakesUpToThreeLetters() {
        assertEquals("JJ", stationAcronym("Jazz Jeden"))
        assertEquals("RMF", stationAcronym("rmf"))
        assertEquals("RZW", stationAcronym("Radio Zet Wieczór Extra"))
        assertEquals("?", stationAcronym("  "))
        assertEquals("K2", stationAcronym("Klasyka 24"))
    }
}
