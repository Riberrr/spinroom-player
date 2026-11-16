package io.github.riberrr.spinroom.feature.playlists

import io.github.riberrr.spinroom.core.data.PlaylistSort
import io.github.riberrr.spinroom.core.data.SourceOption
import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.ProviderMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistsLogicTest {
    private val sources = listOf(SourceOption("spotify", "Spotify"), SourceOption("ytmusic", "YouTube Music"))

    @Test fun `sort toggles between recent and alphabetical and uses the right order_by`() {
        assertEquals(PlaylistSort.Alphabetical, nextSort(PlaylistSort.Recent))
        assertEquals(PlaylistSort.Recent, nextSort(PlaylistSort.Alphabetical))
        assertEquals("last_played_desc", PlaylistSort.Recent.orderBy)
    }

    @Test fun `source cycles through all sources and back to all`() {
        assertEquals("spotify", nextProvider(null, sources))
        assertEquals("ytmusic", nextProvider("spotify", sources))
        assertNull(nextProvider("ytmusic", sources))
        assertNull(nextProvider(null, emptyList()))
        assertNull(nextProvider("gone", sources))
    }

    @Test fun `row shows the source name of the first provider mapping`() {
        val item = MediaItem(
            itemId = "7", name = "Mix", uri = "ytmusic://playlist/7",
            providerMappings = listOf(ProviderMapping(providerDomain = "ytmusic")),
        )
        val row = item.toRow("10.0.1.50:8095", sources)
        assertEquals("YouTube Music", row.source)
        assertEquals("ytmusic://playlist/7", row.uri)
        assertNull(row.coverUrl)
        assertEquals("", MediaItem(itemId = "1", name = "X").toRow("a", sources).source)
    }
}
