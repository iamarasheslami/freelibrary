package com.freelibrary.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryPlannerTest {
    private fun plan(
        query: String,
        scope: SearchScope = SearchScope.TITLE,
    ): SearchPlan {
        val plan = SearchQueryPlanner.plan(query, scope)
        assertNotNull(plan)
        return plan!!
    }

    @Test
    fun `a title query matches each word as a word start and treats common words as optional`() {
        val plan = plan("Pride and Prejudice")

        assertEquals(listOf("pride", "prejudice"), plan.requiredWords)
        assertEquals(listOf("and"), plan.optionalWords)
        assertEquals("title:pride* title:prejudice*", plan.matchQuery)
        assertEquals("pride and prejudice", plan.phrase)
    }

    @Test
    fun `an author query searches the author column`() {
        val plan = plan("Jane Austen", SearchScope.AUTHOR)

        assertEquals("authorNames:jane* authorNames:austen*", plan.matchQuery)
    }

    @Test
    fun `a leading common word is left out of matching`() {
        assertEquals("title:odyssey*", plan("the odyssey").matchQuery)
    }

    @Test
    fun `a query made only of common words keeps all of them`() {
        val plan = plan("the in")

        assertEquals(listOf("the", "in"), plan.requiredWords)
        assertEquals(emptyList<String>(), plan.optionalWords)
        assertEquals("title:the* title:in*", plan.matchQuery)
    }

    @Test
    fun `case and accents are folded before planning`() {
        assertEquals("title:miserables*", plan("  Les MISÉRABLES ").matchQuery)
    }

    @Test
    fun `single character words are matched exactly, not as prefixes`() {
        assertEquals("title:b title:2", plan("b 2").matchQuery)
    }

    @Test
    fun `repeated words are matched once`() {
        assertEquals(listOf("war", "peace"), plan("war war peace").requiredWords)
    }

    @Test
    fun `a French elision leaves the article as an optional word`() {
        val plan = plan("l'homme qui rit")

        assertEquals(listOf("homme", "qui", "rit"), plan.requiredWords)
        assertEquals(listOf("l"), plan.optionalWords)
    }

    @Test
    fun `text with no letters or digits gives no plan`() {
        assertNull(SearchQueryPlanner.plan("  !!! ", SearchScope.TITLE))
        assertNull(SearchQueryPlanner.plan("", SearchScope.AUTHOR))
    }

    @Test
    fun `matchQueryFor builds the expression for a corrected word list`() {
        assertEquals(
            "title:pride* title:prejudice*",
            SearchQueryPlanner.matchQueryFor(SearchScope.TITLE, listOf("pride", "prejudice")),
        )
    }
}
