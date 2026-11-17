package io.github.riberrr.spinroom.feature.radio

import io.github.riberrr.spinroom.core.maapi.RadioFilter

private val LANGUAGES = mapOf(
    "polish" to "polski", "english" to "angielski", "german" to "niemiecki", "spanish" to "hiszpański",
    "french" to "francuski", "russian" to "rosyjski", "italian" to "włoski", "portuguese" to "portugalski",
    "dutch" to "niderlandzki", "czech" to "czeski", "slovak" to "słowacki", "ukrainian" to "ukraiński",
    "greek" to "grecki", "chinese" to "chiński", "turkish" to "turecki", "swedish" to "szwedzki",
)

/** Short display text of a filter value: Polish names for common languages, otherwise the server's name. */
fun filterValueLabel(filter: RadioFilter, serverName: String? = null): String = when (filter) {
    is RadioFilter.Language -> LANGUAGES[filter.key.lowercase()] ?: serverName ?: filter.key.replaceFirstChar { it.uppercase() }
    is RadioFilter.Country -> serverName ?: filter.key.uppercase()
    is RadioFilter.Tag -> serverName ?: filter.key.replaceFirstChar { it.uppercase() }
    RadioFilter.Popular -> ""
}

/** Option label in a picker (the server lists languages in English). */
fun languageOptionLabel(serverName: String): String = LANGUAGES[serverName.lowercase()] ?: serverName
