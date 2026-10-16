package com.freelibrary.app.domain.model

/**
 * What the reader narrowed the catalog down to. An empty choice means "any".
 *
 * No [language] chosen means the catalog default on Explore ([browseLanguage]) and every language
 * in Search. [formats] are format types ("epub", "pdf", "txt") and [genres] are catalog bookshelf
 * names ("Category: Fiction"); a book matches when it has any chosen format and is on any chosen
 * genre shelf.
 */
data class BookFilter(
    val language: String? = null,
    val formats: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
) {
    /** The language Explore lists books in: the chosen one, or the catalog default. */
    val browseLanguage: String get() = language ?: CatalogDefaults.LANGUAGE

    /** How many separate choices are on; shown next to the filter button. */
    val activeCount: Int get() = (if (language == null) 0 else 1) + formats.size + genres.size

    val isActive: Boolean get() = activeCount > 0
}
