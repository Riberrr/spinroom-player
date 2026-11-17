package io.github.riberrr.spinroom.feature.radio

import io.github.riberrr.spinroom.core.maapi.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioSearchTest {
    private fun st(id: String, name: String) = MediaItem(itemId = id, name = name, uri = "radiobrowser://radio/$id", mediaType = "radio")

    @Test fun dedupesByNameAndUri() {
        val items = listOf(st("1", "- 0 N - Jazz on Radio"), st("2", "- 0 N - Jazz on Radio"), st("3", "Other"), st("3", "Other 2"))
        assertEquals(listOf("1", "3"), orderSearchResults(items, { false }, emptySet()).map { it.item.itemId })
    }

    @Test fun favoritesThenPolishThenRest() {
        val items = listOf(st("a", "Foreign"), st("b", "Radio Jazz"), st("c", "Fav"))
        val out = orderSearchResults(items, { it.itemId == "c" }, setOf("b"))
        assertEquals(listOf("c", "b", "a"), out.map { it.item.itemId })
    }
}
