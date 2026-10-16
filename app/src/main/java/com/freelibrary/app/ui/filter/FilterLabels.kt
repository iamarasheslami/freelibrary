package com.freelibrary.app.ui.filter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.freelibrary.app.R
import java.util.Locale

/** The words and icons of the filter, decided apart from any view so they can be tested. */
object FilterLabels {
    private const val GENRE_PREFIX = "Category: "

    /** A language code as a reader's language name ("fr" is "French"); an unknown code is shown in capitals. */
    fun languageLabel(
        code: String,
        displayLocale: Locale = Locale.getDefault(),
    ): String {
        val name = Locale.forLanguageTag(code).getDisplayLanguage(displayLocale)
        return if (name.isBlank() || name.equals(code, ignoreCase = true)) {
            code.uppercase(Locale.ROOT)
        } else {
            name.replaceFirstChar { it.titlecase(displayLocale) }
        }
    }

    /** A genre shelf name without its "Category: " prefix. */
    fun genreLabel(shelfName: String): String = shelfName.removePrefix(GENRE_PREFIX)

    /** The name of a known file format, or null for one without a translated name (show it in capitals then). */
    @StringRes
    fun formatLabelRes(formatType: String): Int? =
        when (formatType) {
            "epub" -> R.string.format_epub
            "pdf" -> R.string.format_pdf
            "txt" -> R.string.format_txt
            else -> null
        }

    @DrawableRes
    fun iconRes(active: Boolean): Int = if (active) R.drawable.ic_filter_active else R.drawable.ic_filter

    @StringRes
    fun descriptionRes(active: Boolean): Int = if (active) R.string.filter_title_active else R.string.filter_title
}
