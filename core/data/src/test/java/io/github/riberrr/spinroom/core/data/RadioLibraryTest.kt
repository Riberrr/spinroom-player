package io.github.riberrr.spinroom.core.data

import io.github.riberrr.spinroom.core.maapi.MediaItem
import org.junit.Test
import org.junit.Assert.assertEquals

class RadioLibraryTest {
    private fun item(id: String) = MediaItem(itemId = id, provider = "library", name = id, uri = "library://radio/$id")

    @Test fun appendSkipsDuplicates() {
        val merged = RadioLibraryRepository.append(listOf(item("1"), item("2")), listOf(item("2"), item("3")))
        assertEquals(listOf("1", "2", "3"), merged.map { it.itemId })
    }
}
