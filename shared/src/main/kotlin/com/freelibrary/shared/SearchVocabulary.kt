package com.freelibrary.shared

import kotlin.math.abs

/**
 * The words that occur in the search index, each with the number of books
 * that use it, used to suggest corrections for misspelled search words.
 * Built in memory from the indexed text, so it can never drift out of date
 * with the index. Pure logic with no database access.
 */
class SearchVocabulary(wordCounts: Map<String, Int>) {
    private val counts: Map<String, Int> = wordCounts
    private val byFirstLetter: Map<Char, List<Map.Entry<String, Int>>> =
        wordCounts.entries.filter { it.key.isNotEmpty() }.groupBy { it.key[0] }

    private data class Match(
        val distance: Int,
        val bookCount: Int,
        val word: String,
    )

    val size: Int get() = counts.size

    operator fun contains(word: String): Boolean = word in counts

    /**
     * Likely intended spellings of [word], best first, at most [limit].
     * Fast by default: only words with the same first letter are considered.
     * Pass [widen] = true for the slower pass over every word, which also
     * catches a wrong first letter ("bride" for "pride"). Words of up to
     * three letters are never corrected, because nearly everything is
     * within one edit of them.
     */
    fun correctionsFor(
        word: String,
        widen: Boolean = false,
        limit: Int = DEFAULT_LIMIT,
    ): List<String> {
        val maxEdits = maxEditsFor(word.length)
        if (maxEdits == 0) return emptyList()

        val pool: Iterable<Map.Entry<String, Int>> =
            if (widen) counts.entries else byFirstLetter[word[0]].orEmpty()
        val byEdits =
            pool.asSequence()
                .filter { it.key != word && abs(it.key.length - word.length) <= maxEdits }
                .mapNotNull { entry ->
                    val distance = boundedEditDistance(word, entry.key, maxEdits)
                    if (distance <= maxEdits) Match(distance, entry.value, entry.key) else null
                }
                .sortedWith(compareBy<Match>({ it.distance }, { -it.bookCount }, { it.word }))
                .map { it.word }

        return (listOfNotNull(elisionCandidate(word)) + byEdits).distinct().take(limit).toList()
    }

    /**
     * "dartagnan" typed without its apostrophe is d' + "artagnan": when the
     * typed word is unknown but dropping its first letter gives a known word,
     * offer that word. A direct lookup, so it costs nothing.
     */
    private fun elisionCandidate(word: String): String? {
        if (word.length < MIN_ELISION_LENGTH || word in counts) return null
        if (word[0] !in ELISION_LETTERS) return null
        return word.substring(1).takeIf { it in counts }
    }

    private fun maxEditsFor(length: Int): Int =
        when {
            length <= 3 -> 0
            length <= 7 -> 1
            else -> 2
        }

    companion object {
        private const val DEFAULT_LIMIT = 5
        private const val MIN_ELISION_LENGTH = 4
        private const val ELISION_LETTERS = "ldjmnstc"

        /** Counts each word once per text, so counts mean "number of books". */
        fun fromTexts(texts: Sequence<String>): SearchVocabulary {
            val counts = HashMap<String, Int>()
            for (text in texts) {
                for (word in SearchText.tokens(text).toSet()) counts.merge(word, 1, Int::plus)
            }
            return SearchVocabulary(counts)
        }

        /** Like [fromTexts], for text already folded by [SearchText.fold]: it only splits on spaces. */
        fun fromFoldedTexts(texts: Sequence<String>): SearchVocabulary {
            val counts = HashMap<String, Int>()
            for (text in texts) {
                for (word in text.split(' ').filter { it.isNotEmpty() }.toSet()) counts.merge(word, 1, Int::plus)
            }
            return SearchVocabulary(counts)
        }
    }
}

/**
 * Edit distance between [a] and [b] where inserting, deleting, replacing a
 * letter or swapping two neighbouring letters each cost one. Gives up early:
 * returns [maxEdits] + 1 as soon as the answer must exceed [maxEdits].
 */
internal fun boundedEditDistance(
    a: String,
    b: String,
    maxEdits: Int,
): Int {
    if (abs(a.length - b.length) > maxEdits) return maxEdits + 1
    val width = b.length + 1
    var older = IntArray(width)
    var previous = IntArray(width) { it }
    var current = IntArray(width)
    for (i in 1..a.length) {
        current[0] = i
        var rowMinimum = current[0]
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var value = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                value = minOf(value, older[j - 2] + 1)
            }
            current[j] = value
            rowMinimum = minOf(rowMinimum, value)
        }
        if (rowMinimum > maxEdits) return maxEdits + 1
        val recycled = older
        older = previous
        previous = current
        current = recycled
    }
    return previous[b.length]
}
