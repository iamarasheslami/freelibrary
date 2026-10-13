package com.freelibrary.app.ui.cover

/**
 * A book title split the way a generated cover shows it: the main title large and an optional
 * subtitle smaller. Gutenberg titles often read "Title; Or, Subtitle", and wrapping such a title
 * as one string would leave the semicolon at the start of a line.
 */
data class CoverTitle(
    val main: String,
    val subtitle: String?,
) {
    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** A semicolon, or a colon followed by a space or the end of the title ("12:30" stays whole). */
        private val SEPARATOR = Regex(";|:(?=\\s|$)")

        /**
         * Some catalog titles carry a cataloguing subfield code such as "$b" ("Title : $b Subtitle").
         * A standalone code, with any colon or semicolon in front of it, reads as a plain colon.
         */
        private val SUBFIELD_CODE = Regex("(?:\\s*[:;]\\s*)?(?<![^\\s])\\$[a-z](?![^\\s])\\s*")

        /** Splits at the first separator. Never throws, and never returns a blank subtitle. */
        fun parse(title: String): CoverTitle {
            val clean = title.trim().replace(WHITESPACE, " ").replace(SUBFIELD_CODE, ": ").trim()
            val separator = SEPARATOR.find(clean) ?: return CoverTitle(clean, null)
            val main = clean.substring(0, separator.range.first).trim()
            val subtitle = clean.substring(separator.range.last + 1).trim()
            return when {
                main.isEmpty() -> CoverTitle(subtitle, null)
                subtitle.isEmpty() -> CoverTitle(main, null)
                else -> CoverTitle(main, subtitle)
            }
        }
    }
}
