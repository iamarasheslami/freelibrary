package com.freelibrary.shared

enum class SearchScope {
    TITLE,
    AUTHOR,
}

/**
 * What to search for, derived from the user's text.
 *
 * [phrase] is the whole normalized query, kept so ranking can prefer results
 * containing it verbatim. [requiredWords] must all match; [optionalWords] are
 * common words ("the", "of", "de"...) that were left out of matching because
 * other words were present. [matchQuery] is the expression for books_fts.
 */
data class SearchPlan(
    val scope: SearchScope,
    val phrase: String,
    val requiredWords: List<String>,
    val optionalWords: List<String>,
    val matchQuery: String,
)

/**
 * Turns raw user input into a [SearchPlan]. Pure logic with no database
 * access. Column names must match the books_fts table.
 */
object SearchQueryPlanner {
    private const val TITLE_COLUMN = "title"
    private const val AUTHOR_COLUMN = "authorNames"

    // Below this length a word is matched exactly: "b*" would match thousands of words.
    private const val MIN_PREFIX_LENGTH = 2

    // Frequent function words across the catalog's main languages, chosen from
    // real title frequencies. Meaningful frequent words (history, life...) are deliberately absent.
    private val COMMON_WORDS =
        setOf(
            // English
            "the", "of", "and", "a", "an", "in", "on", "at", "to", "for", "from", "with", "by", "or", "as",
            "his", "its", "their",
            // French (l and d are the pieces left by elision, as in l'homme)
            "de", "la", "le", "les", "des", "du", "et", "en", "un", "une", "l", "d",
            // German
            "der", "die", "das", "und", "von", "den", "dem", "im",
            // Spanish, Portuguese, Italian, Dutch, Finnish
            "el", "los", "las", "del", "y", "e", "o", "di", "da", "il", "het", "van", "een", "ja",
        )

    /** Returns null when the text contains no searchable words. */
    fun plan(
        rawQuery: String,
        scope: SearchScope,
    ): SearchPlan? {
        val phrase = SearchText.fold(rawQuery)
        if (phrase.isEmpty()) return null

        val words = phrase.split(' ').distinct()
        val required = words.filterNot { it in COMMON_WORDS }.ifEmpty { words }
        val optional = words.filter { it !in required }

        return SearchPlan(
            scope = scope,
            phrase = phrase,
            requiredWords = required,
            optionalWords = optional,
            matchQuery = matchQueryFor(scope, required),
        )
    }

    /** True for the frequent function words that are optional when matching. */
    fun isCommonWord(word: String): Boolean = word in COMMON_WORDS

    /** The books_fts expression for [words]: every word must match, as a word start. */
    fun matchQueryFor(
        scope: SearchScope,
        words: List<String>,
    ): String {
        val column =
            when (scope) {
                SearchScope.TITLE -> TITLE_COLUMN
                SearchScope.AUTHOR -> AUTHOR_COLUMN
            }
        return words.joinToString(" ") { word ->
            if (word.length >= MIN_PREFIX_LENGTH) "$column:$word*" else "$column:$word"
        }
    }
}
