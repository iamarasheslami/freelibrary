package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookFtsDao
import com.freelibrary.shared.SearchCandidate
import com.freelibrary.shared.SearchPlan
import com.freelibrary.shared.SearchQueryPlanner
import com.freelibrary.shared.SearchRanker
import com.freelibrary.shared.SearchScope
import com.freelibrary.shared.SearchVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CANDIDATE_LIMIT = 1000
private const val MIN_EXACT_RESULTS = 3
private const val MAX_CORRECTION_COMBINATIONS = 36
private const val DEFAULT_RESULT_LIMIT = 50

/** A search hit as the UI needs it. [authors] are in credit order. */
data class SearchResult(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authors: List<String>,
)

/**
 * Searches the catalog by title or by author, tolerating typos.
 *
 * First an exact search (each word as a word start). If that finds fewer
 * than [MIN_EXACT_RESULTS] books, likely corrections of each word are tried
 * in combination; and if that still finds nothing, a slower pass also
 * corrects a wrong first letter. Everything found is ranked best-first.
 * Corrected hits always rank below exact ones.
 */
@Singleton
class SearchRepository
    @Inject
    constructor(
        private val bookFtsDao: BookFtsDao,
    ) {
        private val vocabularyLock = Mutex()
        private val vocabularies = mutableMapOf<SearchScope, SearchVocabulary>()

        suspend fun search(
            query: String,
            scope: SearchScope,
            limit: Int = DEFAULT_RESULT_LIMIT,
        ): List<SearchResult> {
            val plan = SearchQueryPlanner.plan(query, scope) ?: return emptyList()
            val found = LinkedHashMap<Long, SearchCandidate>()

            collect(found, plan.matchQuery, plan.requiredWords, corrected = false)
            if (found.size < MIN_EXACT_RESULTS) {
                val vocabulary = vocabularyFor(scope)
                tryCorrections(plan, vocabulary, widen = false, found = found)
                if (found.isEmpty()) tryCorrections(plan, vocabulary, widen = true, found = found)
            }

            val ranked = withContext(Dispatchers.Default) { SearchRanker.rank(plan, found.values.toList()) }
            return ranked.take(limit).map { SearchResult(it.bookId, it.externalId, it.title, it.authors) }
        }

        /** Call after the catalog changes (e.g. a sync) so spelling corrections see the new words. */
        suspend fun invalidateVocabularies() {
            vocabularyLock.withLock { vocabularies.clear() }
        }

        private suspend fun collect(
            found: MutableMap<Long, SearchCandidate>,
            matchQuery: String,
            matchedWords: List<String>,
            corrected: Boolean,
        ) {
            val rows = bookFtsDao.findCandidates(matchQuery, CANDIDATE_LIMIT)
            rows.groupBy { it.bookId }.forEach { (bookId, bookRows) ->
                if (bookId !in found) {
                    val first = bookRows.first()
                    found[bookId] =
                        SearchCandidate(
                            bookId = bookId,
                            externalId = first.externalId,
                            title = first.title,
                            authors = bookRows.mapNotNull { it.authorName },
                            matchedWords = matchedWords,
                            corrected = corrected,
                        )
                }
            }
        }

        private suspend fun tryCorrections(
            plan: SearchPlan,
            vocabulary: SearchVocabulary,
            widen: Boolean,
            found: MutableMap<Long, SearchCandidate>,
        ) {
            val alternatives =
                withContext(Dispatchers.Default) {
                    plan.requiredWords.map { word -> listOf(word) + vocabulary.correctionsFor(word, widen) }
                }
            for (combination in combinations(alternatives).take(MAX_CORRECTION_COMBINATIONS)) {
                currentCoroutineContext().ensureActive()
                if (combination == plan.requiredWords) continue
                collect(found, SearchQueryPlanner.matchQueryFor(plan.scope, combination), combination, corrected = true)
            }
        }

        /** Every way of picking one option per word, the user's own words first. */
        private fun combinations(alternatives: List<List<String>>): Sequence<List<String>> =
            alternatives.fold(sequenceOf(emptyList())) { prefixes, options ->
                prefixes.flatMap { prefix -> options.asSequence().map { prefix + it } }
            }

        private suspend fun vocabularyFor(scope: SearchScope): SearchVocabulary =
            vocabularyLock.withLock {
                vocabularies[scope] ?: buildVocabulary(scope).also { vocabularies[scope] = it }
            }

        private suspend fun buildVocabulary(scope: SearchScope): SearchVocabulary {
            val texts =
                when (scope) {
                    SearchScope.TITLE -> bookFtsDao.allIndexedTitles()
                    SearchScope.AUTHOR -> bookFtsDao.allIndexedAuthorNames()
                }
            return withContext(Dispatchers.Default) { SearchVocabulary.fromFoldedTexts(texts.asSequence()) }
        }
    }
