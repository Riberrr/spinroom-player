package io.github.riberrr.spinroom.feature.settings

import io.github.riberrr.spinroom.core.maapi.MaJson
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

    // Shapes seen on MA 2.10.12 (anonymised, artificial uuid/ip): the TV's Sendspin client and its built-in Chromecast
    // share name and model; only the Chromecast has cast_uuid and ip_address.
    private fun json(s: String) = MaJson.decodeFromString(Player.serializer(), s)

    private val sendspinTv = json(
        """{"player_id":"up6b461d4c","provider":"universal_player","name":"Telewizor","available":true,
        "device_info":{"model":"TV Model X","manufacturer":"Maker","identifiers":{}}}""",
    )
    private val castTv = json(
        """{"player_id":"up31735a7e","provider":"universal_player","name":"Telewizor","available":true,
        "device_info":{"model":"TV Model X","ip_address":"192.0.2.6","identifiers":{"cast_uuid":"aaaa","uuid":"bbbb","mac":"00:00:00:00:00:00"}}}""",
    )
    private val key = "zBb7jeAbCdEfGhIjKlMnOpQrStUvWxYz0xCvSk"

    @Test fun realIdsOwnIsSendspinNotChromecast() {
        for (order in listOf(listOf(castTv, sendspinTv), listOf(sendspinTv, castTv))) {
            val rows = buildPlayerRows(order, ownId = key, selectedId = "up31735a7e", ownModel = "TV Model X")
            assertEquals("up6b461d4c", rows[0].player.playerId)
            assertTrue(rows[0].isOwn)
            assertFalse(rows[1].isOwn)
            assertEquals(null, rows[1].shortId)
            assertEquals("Chromecast · 192.0.2.6", playerDetail(castTv, rows[1].shortId))
        }
    }

    @Test fun heuristicNeedsModel() {
        assertEquals(null, resolveOwnPlayerId(listOf(sendspinTv), key, "", ""))
        assertEquals(null, resolveOwnPlayerId(listOf(sendspinTv), key, "Other", ""))
    }

    @Test fun idMatchBeatsHeuristic() {
        val keyed = Player(playerId = key, name = "Telewizor", provider = "sendspin")
        assertEquals(key, resolveOwnPlayerId(listOf(sendspinTv, keyed), key, "TV Model X", ""))
    }
}
