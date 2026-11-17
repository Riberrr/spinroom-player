package io.github.riberrr.spinroom.feature.radio

import io.github.riberrr.spinroom.core.maapi.RadioFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioLabelsTest {
    @Test fun polishLanguageShowsPolishName() = assertEquals("polski", filterValueLabel(RadioFilter.Language("polish")))
    @Test fun unknownLanguageUsesServerName() = assertEquals("Maltese", filterValueLabel(RadioFilter.Language("maltese"), "Maltese"))
    @Test fun countryFallsBackToUpperCode() = assertEquals("PL", filterValueLabel(RadioFilter.Country("pl")))
    @Test fun tagFallsBackToCapitalized() = assertEquals("Jazz", filterValueLabel(RadioFilter.Tag("jazz")))
    @Test fun popularHasNoValue() = assertEquals("", filterValueLabel(RadioFilter.Popular))
}
