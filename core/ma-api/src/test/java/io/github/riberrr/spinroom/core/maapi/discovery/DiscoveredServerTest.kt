package io.github.riberrr.spinroom.core.maapi.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveredServerTest {
    private fun txt(vararg pairs: Pair<String, String>) = pairs.associate { it.first to it.second.toByteArray() }

    @Test fun usesTrimmedBaseUrlFromTxt() {
        val s = DiscoveredServer.from("MA", "192.168.1.10", 8095, txt("base_url" to "   http://192.168.1.10:8095/", "server_id" to "abc"))
        assertEquals(DiscoveredServer("abc", "MA", "http://192.168.1.10:8095"), s)
    }

    @Test fun fallsBackToHostAndPort() {
        assertEquals("192.168.1.10:8095", DiscoveredServer.from("MA", "192.168.1.10", 8095, emptyMap())?.address)
        assertEquals("[fe80::1]:8095", DiscoveredServer.from("MA", "fe80::1", 8095, emptyMap())?.address)
    }

    @Test fun nothingUsable() {
        assertNull(DiscoveredServer.from("MA", null, 0, mapOf("base_url" to null)))
    }
}
