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

    @Test fun ownIdMatchedWithPrefixOrSuffix() {
        val key = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCdE"
        for (id in listOf(key, "sendspin-$key", "ma_${key}_1")) {
            val rows = buildPlayerRows(listOf(p("other", "Salon"), p(id, "Telewizor")), ownId = key, selectedId = "other")
            assertEquals(id, rows[0].player.playerId)
            assertTrue(rows[0].isOwn)
            assertFalse(rows[1].isOwn)
        }
    }

    @Test fun unrelatedIdNotOwn() {
        assertFalse(isOwnPlayerId("abc", "abcd"))
        assertFalse(isOwnPlayerId("abc", ""))
    }

    @Test fun duplicateNamesGetShortId() {
        val rows = buildPlayerRows(listOf(p("id-111111", "Telewizor"), p("id-222222", "Telewizor"), p("x", "Salon")), ownId = "", selectedId = "")
        assertEquals(listOf("111111", "222222"), rows.filter { it.player.name == "Telewizor" }.map { it.shortId })
        assertEquals(null, rows.first { it.player.name == "Salon" }.shortId)
    }

    @Test fun blankOwnIdMarksNothing() {
        val rows = buildPlayerRows(listOf(p("", "Bez id")), ownId = "", selectedId = "z")
        assertFalse(rows.single().isOwn)
    }
}
