package com.freelibrary.shared

import java.text.Normalizer
import java.util.Locale

/**
 * The single definition of how text is normalized for searching, shared by
 * the catalog tool (which builds the search index) and the app (which
 * queries it and keeps it current during sync). Both sides MUST use this
 * same code: if they ever normalized differently, search would silently
 * stop matching.
 *
 * Accents are stripped only from Latin and Greek letters. In other scripts a
 * combining mark can change the letter itself (Cyrillic й is и plus a mark;
 * Japanese が is か plus a mark), so those scripts are left intact.
 *
 * Apostrophes join the word (king's → kings) except in French/Italian
 * elisions, where they separate two words (l'homme → l homme).
 *
 * Known limitation: scripts written without spaces (e.g. Chinese) are kept
 * as one run of letters, so they only match as whole runs.
 */
object SearchText {
    private val APOSTROPHES = Regex("['’‘ʼ`]")

    // French/Italian elision (l'homme, d'artagnan, dell'arte): the apostrophe
    // separates two words. Elsewhere (king's, don't, o'brien) it joins them.
    private val ELISION =
        Regex(
            "(?<![\\p{L}\\p{N}\\p{M}])" +
                "(l|d|j|m|n|s|t|c|qu|jusqu|lorsqu|puisqu|dell|all|nell|sull|dall|un)['’‘ʼ`]",
        )

    private val NOT_LETTER_DIGIT_OR_MARK = Regex("[^\\p{L}\\p{N}\\p{M}]+")

    private val ACCENT_STRIPPING_SCRIPTS =
        setOf(Character.UnicodeScript.LATIN, Character.UnicodeScript.GREEK)

    // Letters that Unicode decomposition does not reduce to a base letter.
    private val SPECIAL_LETTERS =
        mapOf(
            'ß' to "ss",
            'æ' to "ae",
            'œ' to "oe",
            'ø' to "o",
            'ł' to "l",
            'đ' to "d",
            'ı' to "i",
            'þ' to "th",
        )

    /** Lowercased, accent-free text where every run of other characters is one space. */
    fun fold(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val stripped = StringBuilder(decomposed.length)
        var baseAcceptsStripping = false
        for (char in decomposed) {
            if (Character.getType(char) == Character.NON_SPACING_MARK.toInt()) {
                if (!baseAcceptsStripping) stripped.append(char)
            } else {
                baseAcceptsStripping = Character.UnicodeScript.of(char.code) in ACCENT_STRIPPING_SCRIPTS
                stripped.append(SPECIAL_LETTERS[char] ?: char.toString())
            }
        }
        val recomposed = Normalizer.normalize(stripped, Normalizer.Form.NFC)
        val withElisionSplit = ELISION.replace(recomposed, "\$1 ")
        val withoutApostrophes = APOSTROPHES.replace(withElisionSplit, "")
        return NOT_LETTER_DIGIT_OR_MARK.replace(withoutApostrophes, " ").trim()
    }

    /** The folded words of [text], in order; empty if there are none. */
    fun tokens(text: String): List<String> {
        val folded = fold(text)
        return if (folded.isEmpty()) emptyList() else folded.split(' ')
    }
}
