package com.freelibrary.shared

import kotlin.math.max
import kotlin.math.min

/**
 * One search hit with the data ranking needs. [authors] are in credit order
 * (the first is the primary author). [matchedWords] are the words that
 * actually produced this hit: the user's words, or the corrected ones when
 * [corrected] is true. [prominence] is an optional bonus for well-known books.
 */
data class SearchCandidate(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authors: List<String>,
    val matchedWords: List<String>,
    val corrected: Boolean = false,
    val prominence: Int = 0,
)

/**
 * Orders search hits best-first. Pure logic, tuned against the real catalog:
 * whole-word matches beat word-start matches, the exact phrase and titles
 * that begin with it score higher, extra unmatched title words cost a little,
 * and corrected (typo-fixed) hits rank below exact ones. For author searches
 * the primary author counts most. Ties go to the shorter title, then the
 * lower Gutenberg number.
 */
object SearchRanker {
    private const val WHOLE_WORD_POINTS = 10.0
    private const val WORD_START_POINTS = 4.0
    private const val CONTIGUOUS_PHRASE_POINTS = 15.0
    private const val NEAR_START_POINTS = 10.0
    private const val EXACT_TITLE_POINTS = 40.0
    private const val MAX_EXTRA_TITLE_WORDS_PENALTY = 30.0
    private const val CORRECTION_PENALTY = 20.0
    private const val ALL_WORDS_IN_ONE_AUTHOR_POINTS = 20.0
    private const val EXACT_AUTHOR_NAME_POINTS = 25.0
    private const val PRIMARY_AUTHOR_POINTS = 15.0
    private const val MAX_EXTRA_NAME_WORDS = 10
    private const val EXTRA_NAME_WORD_PENALTY = 0.5
    private const val NO_AUTHOR_SCORE = -1000.0

    private data class Scored(
        val candidate: SearchCandidate,
        val score: Double,
        val titleWordCount: Int,
        val numericId: Long,
    )

    fun rank(
        plan: SearchPlan,
        candidates: List<SearchCandidate>,
    ): List<SearchCandidate> =
        candidates
            .map { candidate ->
                Scored(
                    candidate = candidate,
                    score = score(plan, candidate),
                    titleWordCount = SearchText.tokens(candidate.title).size,
                    numericId = candidate.externalId.toLongOrNull() ?: Long.MAX_VALUE,
                )
            }
            .sortedWith(
                compareByDescending<Scored> { it.score }
                    .thenBy { it.titleWordCount }
                    .thenBy { it.numericId }
                    .thenBy { it.candidate.bookId },
            )
            .map { it.candidate }

    fun score(
        plan: SearchPlan,
        candidate: SearchCandidate,
    ): Double {
        val base =
            when (plan.scope) {
                SearchScope.TITLE -> titleScore(plan, candidate)
                SearchScope.AUTHOR -> authorScore(candidate)
            }
        val penalty = if (candidate.corrected) CORRECTION_PENALTY else 0.0
        return base - penalty + candidate.prominence
    }

    private fun titleScore(
        plan: SearchPlan,
        candidate: SearchCandidate,
    ): Double {
        val words = SearchText.tokens(candidate.title)
        val queryWords = candidate.matchedWords
        val (whole, prefixOnly) = wordMatches(queryWords, words)
        var score = WHOLE_WORD_POINTS * whole + WORD_START_POINTS * prefixOnly

        val start = contiguousStart(queryWords, words)
        if (start >= 0 && queryWords.size > 1) score += CONTIGUOUS_PHRASE_POINTS
        val leadingCommonWords = words.takeWhile(SearchQueryPlanner::isCommonWord).size
        if (start in 0..leadingCommonWords) score += NEAR_START_POINTS

        val phraseWords = plan.phrase.split(' ')
        val titleCore = words.filterNot(SearchQueryPlanner::isCommonWord)
        val phraseCore = phraseWords.filterNot(SearchQueryPlanner::isCommonWord)
        val isExactTitle = if (phraseCore.isEmpty()) words == phraseWords else titleCore == phraseCore
        if (isExactTitle) score += EXACT_TITLE_POINTS

        val extraWords = max(0, words.size - queryWords.size).toDouble()
        return score - min(MAX_EXTRA_TITLE_WORDS_PENALTY, extraWords)
    }

    private fun authorScore(candidate: SearchCandidate): Double {
        val queryWords = candidate.matchedWords
        var best = NO_AUTHOR_SCORE
        candidate.authors.forEachIndexed { position, author ->
            val words = SearchText.tokens(author)
            val (whole, prefixOnly) = wordMatches(queryWords, words)
            var score = WHOLE_WORD_POINTS * whole + WORD_START_POINTS * prefixOnly
            if (whole + prefixOnly == queryWords.size) score += ALL_WORDS_IN_ONE_AUTHOR_POINTS
            if (words.toSet() == queryWords.toSet()) score += EXACT_AUTHOR_NAME_POINTS
            score += if (position == 0) PRIMARY_AUTHOR_POINTS else max(0, 8 - 2 * position).toDouble()
            val extraWords = min(MAX_EXTRA_NAME_WORDS, max(0, words.size - queryWords.size))
            score -= EXTRA_NAME_WORD_PENALTY * extraWords
            best = max(best, score)
        }
        return best
    }

    /** How many query words equal a whole word, and how many only start one. */
    private fun wordMatches(
        queryWords: List<String>,
        words: List<String>,
    ): Pair<Int, Int> {
        val whole = queryWords.count { it in words }
        val prefixOnly = queryWords.count { query -> query !in words && words.any { it.startsWith(query) } }
        return whole to prefixOnly
    }

    /** Index where the query words appear next to each other, in order; -1 if they do not. */
    private fun contiguousStart(
        queryWords: List<String>,
        words: List<String>,
    ): Int {
        val count = queryWords.size
        for (start in 0..words.size - count) {
            val fits =
                (0 until count).all { offset ->
                    val word = words[start + offset]
                    val query = queryWords[offset]
                    word == query || (offset == count - 1 && word.startsWith(query))
                }
            if (fits) return start
        }
        return -1
    }
}
