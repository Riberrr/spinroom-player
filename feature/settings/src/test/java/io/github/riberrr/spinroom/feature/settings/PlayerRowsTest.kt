package io.github.riberrr.spinroom.feature.settings

import io.github.riberrr.spinroom.core.maapi.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRowsTest {
    private fun p(id: String, name: String, available: Boolean = true) = Player(playerId = id, name = name, available = available)

    @Test fun ownFirstThenByName() {
        val rows = buildPlayerRows(listOf(p("b", "Kuchnia"), p("tv", "Telewizor"), p("a", "Salon")), ownId = "tv", selectedId = "tv")
        assertEquals(listOf("tv", "b", "a"), rows.map { it.player.playerId })
        assertTrue(rows[0].isOwn)
        assertFalse(rows[1].isOwn)
    }

    @Test fun staleUnavailableHidden() {
        val rows = buildPlayerRows(listOf(p("tv", "Telewizor"), p("old", "Web", available = false)), ownId = "tv", selectedId = "tv")
        assertEquals(listOf("tv"), rows.map { it.player.playerId })
    }

    @Test fun selectedUnavailableKeptLast() {
        val rows = buildPlayerRows(listOf(p("x", "Aaa", available = false), p("y", "Bbb")), ownId = "", selectedId = "x")
        assertEquals(listOf("y", "x"), rows.map { it.player.playerId })
        assertTrue(rows[1].isSelected)
    }

    @Test fun ownUnavailableStillShownFirst() {
        val rows = buildPlayerRows(listOf(p("a", "Salon"), p("tv", "Telewizor", available = false)), ownId = "tv", selectedId = "a")
        assertEquals(listOf("tv", "a"), rows.map { it.player.playerId })
    }

    @Test fun blankOwnIdMarksNothing() {
        val rows = buildPlayerRows(listOf(p("", "Bez id")), ownId = "", selectedId = "z")
        assertFalse(rows.single().isOwn)
    }
}
