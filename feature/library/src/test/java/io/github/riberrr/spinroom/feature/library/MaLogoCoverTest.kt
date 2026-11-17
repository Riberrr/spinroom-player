package io.github.riberrr.spinroom.feature.library

import io.github.riberrr.spinroom.core.maapi.MediaItem
import io.github.riberrr.spinroom.core.maapi.MediaItemImage
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MaLogoCoverTest {
    private fun playlist(image: MediaItemImage) =
        MediaItem(itemId = "1", mediaType = "playlist", name = "All favorited tracks", image = image)

    @Test fun system_playlist_with_ma_logo_has_no_cover() {
        val row = playlist(MediaItemImage(type = "thumb", path = "logo.png", provider = "builtin")).toRow("10.0.1.50:8095")
        assertNull(row.coverUrl)
    }

    @Test fun ordinary_playlist_keeps_its_cover() {
        val row = playlist(MediaItemImage(type = "thumb", path = "https://img/x.jpg", provider = "ytmusic_free--abc", remotelyAccessible = true)).toRow("10.0.1.50:8095")
        assertNotNull(row.coverUrl)
    }
}
