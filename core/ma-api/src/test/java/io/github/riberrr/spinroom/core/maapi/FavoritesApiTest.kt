package io.github.riberrr.spinroom.core.maapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesApiTest {
    @Test fun `parses library uri`() {
        assertEquals(ParsedUri("library", "album", "12"), parseMediaUri("library://album/12"))
    }

    @Test fun `keeps slashes in provider item id`() {
        assertEquals(ParsedUri("ytmusic--abc", "track", "x/y"), parseMediaUri("ytmusic--abc://track/x/y"))
    }

    @Test fun `rejects non uri`() {
        assertNull(parseMediaUri("nonsense"))
    }
}
