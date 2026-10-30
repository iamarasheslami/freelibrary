package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSearchTest {
    @Test
    fun `a search ignores upper and lower case and numbers every place`() {
        val hits = BookSearch.find("The whale. A WHALE! Whale?", "whale", unit = 3)

        assertEquals(listOf(4, 13, 20), hits.map { it.offset })
        assertEquals(listOf(0, 1, 2), hits.map { it.occurrence })
        assertEquals(listOf(3, 3, 3), hits.map { it.unit })
        assertEquals("WHALE", hits[1].match)
    }

    @Test
    fun `a hit carries the words around it on one line`() {
        val text = "First line\nsecond line has the needle in it\nthird line"

        val hit = BookSearch.find(text, "needle", unit = 0).single()

        assertEquals("First line second line has the ", hit.before)
        assertEquals("needle", hit.match)
        assertEquals(" in it third line", hit.after)
    }

    @Test
    fun `a long text is cut with an ellipsis on the sides that were cut`() {
        val text = "a".repeat(100) + " needle " + "b".repeat(100)

        val hit = BookSearch.find(text, "needle", unit = 0).single()

        assertTrue(hit.before.startsWith("…"))
        assertTrue(hit.after.endsWith("…"))
    }

    @Test
    fun `nothing is found for an empty query or a text without the words`() {
        assertTrue(BookSearch.find("some text", "", unit = 0).isEmpty())
        assertTrue(BookSearch.find("some text", "absent", unit = 0).isEmpty())
    }

    @Test
    fun `the limit stops a search`() {
        val hits = BookSearch.find("ab ".repeat(50), "ab", unit = 0, limit = 7)

        assertEquals(7, hits.size)
    }

    @Test
    fun `a clean query has single spaces and no spaces at the ends`() {
        assertEquals("white whale", BookSearch.clean("  white \n  whale  "))
        assertEquals("", BookSearch.clean("   "))
    }

    @Test
    fun `searching a book lists the units in order`() {
        val units = listOf("no match here", "one love", null, "love and love")

        val results = BookSearch.findAll(units.size, "love") { units[it] }

        assertEquals(listOf(1, 3, 3), results.hits.map { it.unit })
        assertEquals(listOf(0, 0, 1), results.hits.map { it.occurrence })
        assertFalse(results.capped)
    }

    @Test
    fun `a search with too many places is capped at the most it lists`() {
        val units = List(3) { "x ".repeat(100) }

        val results = BookSearch.findAll(units.size, "x") { units[it] }

        assertEquals(BookSearch.MAX_HITS, results.hits.size)
        assertTrue(results.capped)
    }

    @Test
    fun `a search that is no longer wanted can stop between units`() {
        var calls = 0

        val outcome =
            runCatching {
                BookSearch.findAll(10, "x", checkActive = { if (++calls == 3) throw IllegalStateException("stop") }) { "x" }
            }

        assertTrue(outcome.isFailure)
        assertEquals(3, calls)
    }
}
