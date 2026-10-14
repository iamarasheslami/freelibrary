package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.domain.model.BookAuthorCredit
import com.freelibrary.app.ui.cover.CoverTitle
import java.util.Locale

/** A credited person as the page shows them: the name, then their years in brackets when known. */
fun authorLine(credit: BookAuthorCredit): String {
    val years =
        when {
            credit.birthYear != null && credit.deathYear != null -> "${credit.birthYear}–${credit.deathYear}"
            credit.birthYear != null -> "${credit.birthYear}–"
            credit.deathYear != null -> "–${credit.deathYear}"
            else -> null
        }
    return if (years == null) credit.name else "${credit.name} ($years)"
}

/** The language's name in the reader's own language ("English"), or null when the catalog gives none. */
fun languageName(
    code: String?,
    locale: Locale,
): String? {
    val tag = code?.trim().orEmpty()
    if (tag.isEmpty()) return null
    return Locale.forLanguageTag(tag).getDisplayLanguage(locale).ifBlank { null }
}

/** The title in one piece, "Main: Subtitle", cleaned of line breaks and the catalog's markers. */
fun pageTitle(title: String): String {
    val parsed = CoverTitle.parse(title)
    return if (parsed.subtitle == null) parsed.main else "${parsed.main}: ${parsed.subtitle}"
}
