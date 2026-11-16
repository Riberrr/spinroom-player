package io.github.riberrr.spinroom.core.design

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesignLogicTest {
    @Test fun stationColorIsStableAndCaseInsensitive() {
        assertEquals(stationColor("Jazz Jeden"), stationColor("  jazz jeden "))
        assertNotEquals(stationHue("Jazz Jeden"), stationHue("Klasyka 24"))
        assertTrue(stationHue("RMF FM") in 0f..360f)
    }

    @Test fun initials() {
        assertEquals("JJ", stationInitials("Jazz Jeden"))
        assertEquals("RM", stationInitials("rmf"))
        assertEquals("K2", stationInitials("Klasyka 24"))
        assertEquals("?", stationInitials("  "))
    }

    @Test fun debounceFirstImmediateThenSettles() = runTest {
        val out = flow {
            emit("a")          // t=0, passes at once
            delay(100); emit("b")
            delay(100); emit("c") // b is dropped (superseded within 250 ms)
            delay(1000); emit("d")
        }.settleAfterFirst(250).toList()
        assertEquals(listOf("a", "c", "d"), out)
        assertEquals(1450, currentTime)
    }

    @Test fun boxBlurKeepsFlatColorAndSpreadsSpikes() {
        val n = 8
        val flat = IntArray(n * n) { 0xFF336699.toInt() }
        assertEquals(flat.toList(), boxBlur(flat, n, 2).toList())
        val spike = IntArray(n * n) { 0xFF000000.toInt() }.also { it[3 * n + 3] = 0xFFFFFFFF.toInt() }
        val blurred = boxBlur(spike, n, 1)
        assertTrue((blurred[3 * n + 3] and 0xFF) < 0xFF)
        assertTrue((blurred[3 * n + 4] and 0xFF) > 0)
    }

    @Test fun themeAndAccentIdsFallBack() {
        assertEquals(SpinroomThemeId.Grafit, SpinroomThemeId.fromId("???"))
        assertEquals(SpinroomThemeId.Oled, SpinroomThemeId.fromId("oled"))
        assertEquals(SpinroomAccent.Amber, SpinroomAccent.fromId(""))
        assertEquals(SpinroomAccent.Lavender, SpinroomAccent.fromId("lavender"))
    }
}
