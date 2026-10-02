package com.freelibrary.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {
    private fun titleCandidate(
        bookId: Long,
        externalId: String,
        title: String,
        matched: List<String>,
        corrected: Boolean = false,
        prominence: Int = 0,
    ) = SearchCandidate(bookId, externalId, title, emptyList(), matched, corrected, prominence)

    private fun authorCandidate(
        bookId: Long,
        externalId: String,
        title: String,
        authors: List<String>,
        matched: List<String>,
    ) = SearchCandidate(bookId, externalId, title, authors, matched)

    private fun titlePlan(query: String) = SearchQueryPlanner.plan(query, SearchScope.TITLE)!!

    private fun authorPlan(query: String) = SearchQueryPlanner.plan(query, SearchScope.AUTHOR)!!

    private fun ids(ranked: List<SearchCandidate>) = ranked.map { it.bookId }

    @Test
    fun `an exact title beats a long title that merely contains the word`() {
        val long = titleCandidate(1, "100", "Some Remarks on the Tragedy of Hamlet", listOf("hamlet"))
        val exact = titleCandidate(2, "200", "Hamlet", listOf("hamlet"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(titlePlan("hamlet"), listOf(long, exact))))
    }

    @Test
    fun `a whole-word match beats a word-start match`() {
        val prefix = titleCandidate(1, "100", "Warden", listOf("war"))
        val whole = titleCandidate(2, "200", "War", listOf("war"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(titlePlan("war"), listOf(prefix, whole))))
    }

    @Test
    fun `words next to each other in order beat the same words scattered`() {
        val scattered = titleCandidate(1, "100", "Sawyer and Tom", listOf("tom", "sawyer"))
        val together = titleCandidate(2, "200", "Tom Sawyer Abroad", listOf("tom", "sawyer"))

        val ranked = SearchRanker.rank(titlePlan("tom sawyer"), listOf(scattered, together))

        assertEquals(listOf(2L, 1L), ids(ranked))
    }

    @Test
    fun `a leading common word does not hurt an exact title`() {
        val other = titleCandidate(1, "100", "Odyssey of a Hero", listOf("odyssey"))
        val exact = titleCandidate(2, "200", "The Odyssey", listOf("odyssey"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(titlePlan("odyssey"), listOf(other, exact))))
    }

    @Test
    fun `a corrected hit ranks below an exact hit of the same title`() {
        val corrected = titleCandidate(1, "100", "Hamlet", listOf("hamlet"), corrected = true)
        val exact = titleCandidate(2, "200", "Hamlet", listOf("hamlet"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(titlePlan("hamlet"), listOf(corrected, exact))))
    }

    @Test
    fun `the primary author beats a co-author`() {
        val coAuthor =
            authorCandidate(1, "100", "An Anthology", listOf("Gogol, Nikolai", "Dostoyevsky, Fyodor"), listOf("dostoyevsky"))
        val primary = authorCandidate(2, "200", "Poor Folk", listOf("Dostoyevsky, Fyodor"), listOf("dostoyevsky"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(authorPlan("dostoyevsky"), listOf(coAuthor, primary))))
    }

    @Test
    fun `all words in one author beat words spread across two authors`() {
        val spread =
            authorCandidate(1, "100", "Mixed", listOf("Austen, Dorothy", "Jane, Smith"), listOf("jane", "austen"))
        val single = authorCandidate(2, "200", "Emma", listOf("Austen, Jane"), listOf("jane", "austen"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(authorPlan("jane austen"), listOf(spread, single))))
    }

    @Test
    fun `equal scores go to the shorter title even when its number is higher`() {
        val longTitle = authorCandidate(1, "1342", "Pride and Prejudice", listOf("Austen, Jane"), listOf("austen"))
        val shortTitle = authorCandidate(2, "33628", "Emma", listOf("Austen, Jane"), listOf("austen"))

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(authorPlan("austen"), listOf(longTitle, shortTitle))))
    }

    @Test
    fun `identical hits go to the lower Gutenberg number and non-numeric ids come last`() {
        val high = titleCandidate(1, "33628", "Emma", listOf("emma"))
        val low = titleCandidate(2, "158", "Emma", listOf("emma"))
        val odd = titleCandidate(3, "abc", "Emma", listOf("emma"))

        assertEquals(listOf(2L, 1L, 3L), ids(SearchRanker.rank(titlePlan("emma"), listOf(odd, high, low))))
    }

    @Test
    fun `prominence lifts a hit above an otherwise identical one`() {
        val plain = titleCandidate(1, "158", "Emma", listOf("emma"))
        val prominent = titleCandidate(2, "33628", "Emma", listOf("emma"), prominence = 8)

        assertEquals(listOf(2L, 1L), ids(SearchRanker.rank(titlePlan("emma"), listOf(plain, prominent))))
    }

    @Test
    fun `ranking an empty list gives an empty list`() {
        assertTrue(SearchRanker.rank(titlePlan("emma"), emptyList()).isEmpty())
    }
}
