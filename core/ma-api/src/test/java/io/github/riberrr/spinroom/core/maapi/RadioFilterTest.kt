package io.github.riberrr.spinroom.core.maapi

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioFilterTest {
    @Test fun languageMapsToLowercasePath() =
        assertEquals("radiobrowser://category/language/polish", RadioFilter.Language("Polish").path)

    @Test fun countryMapsToCodePath() =
        assertEquals("radiobrowser://category/country/pl", RadioFilter.Country("PL").path)

    @Test fun tagKeepsSpaces() =
        assertEquals("radiobrowser://category/tag/adult contemporary", RadioFilter.Tag("Adult Contemporary").path)

    @Test fun popularHasOwnPath() =
        assertEquals("radiobrowser://popularity/popular", RadioFilter.Popular.path)

    @Test fun listPathsMatchServer() {
        assertEquals("radiobrowser://category/language", RadioFilterKind.LANGUAGE.listPath)
        assertEquals("radiobrowser://category/country", RadioFilterKind.COUNTRY.listPath)
        assertEquals("radiobrowser://category/tag", RadioFilterKind.TAG.listPath)
    }
}
