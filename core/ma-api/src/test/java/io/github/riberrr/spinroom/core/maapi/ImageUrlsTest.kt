package io.github.riberrr.spinroom.core.maapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlsTest {
    private val base = "192.168.1.10"

    @Test fun usesProxyIdWhenPresent() {
        val img = MediaItemImage(path = "https://cdn/x.jpg", provider = "yt", remotelyAccessible = true, proxyId = "abc123")
        assertEquals("http://192.168.1.10:8095/imageproxy/abc123?size=256", ImageUrls.url(base, img, 256))
    }

    @Test fun publicUrlWithoutProxyId() {
        val img = MediaItemImage(path = "https://cdn/x.jpg", provider = "yt", remotelyAccessible = true)
        assertEquals("https://cdn/x.jpg", ImageUrls.url(base, img, 256))
    }

    @Test fun localPathFallsBackToQueryForm() {
        val img = MediaItemImage(path = "logo.png", provider = "builtin")
        assertEquals("http://192.168.1.10:8095/imageproxy?path=logo.png&provider=builtin&size=256", ImageUrls.url(base, img, 64))
    }

    @Test fun sizeRoundsUpToWhatTheProxyServes() {
        assertEquals(256, ImageUrls.proxySize(64))
        assertEquals(256, ImageUrls.proxySize(256))
        assertEquals(512, ImageUrls.proxySize(320))
        assertEquals(1024, ImageUrls.proxySize(600))
        assertEquals(1024, ImageUrls.proxySize(4000))
        val img = MediaItemImage(path = "x", proxyId = "id")
        assertEquals("http://192.168.1.10:8095/imageproxy/id?size=512", ImageUrls.url(base, img, 400))
    }

    @Test fun nothingToShow() {
        assertNull(ImageUrls.url(base, null, 64))
        assertNull(ImageUrls.url(base, MediaItemImage(path = ""), 64))
    }
}
