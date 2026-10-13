package com.freelibrary.app.domain.model

import com.freelibrary.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionsTest {
    private val none = BookSlider(emptyList(), 0)
    private val harvard = HomeShelf("harvard-classics", R.string.shelf_harvard_classics, "Harvard Classics")
    private val poetry = HomeShelf("poetry", R.string.shelf_poetry, "Category: Poetry")

    private fun books(count: Int) = List(count) { BookCard(it.toLong(), "$it", "Title $it", null, null) }

    private fun slider(
        shown: Int,
        total: Int = shown,
    ) = BookSlider(books(shown), total)

    private fun build(
        continueReading: BookSlider = slider(2),
        wantToRead: BookSlider = slider(3),
        recentlyAdded: List<BookCard> = books(10),
        shelves: List<Pair<HomeShelf, BookSlider>> = listOf(harvard to slider(10, 51), poetry to slider(10, 40)),
    ) = HomeSections.build(continueReading, wantToRead, recentlyAdded, shelves)

    private fun List<HomeSection>.ids() = map { it.id }

    @Test
    fun `the sliders come in the agreed order`() {
        assertEquals(
            listOf("continue-reading", "want-to-read", "recently-added", "harvard-classics", "poetry"),
            build().ids(),
        )
    }

    @Test
    fun `a reader with nothing saved sees no empty personal sliders`() {
        assertEquals(
            listOf("recently-added", "harvard-classics", "poetry"),
            build(continueReading = none, wantToRead = none).ids(),
        )
    }

    @Test
    fun `a shelf with no books is left out`() {
        assertEquals(
            listOf("continue-reading", "want-to-read", "recently-added", "poetry"),
            build(shelves = listOf(harvard to none, poetry to slider(10, 40))).ids(),
        )
    }

    @Test
    fun `a shelf offers view more only when it holds more books than shown`() {
        val sections = build(shelves = listOf(harvard to slider(10, 51), poetry to slider(7, 7)))

        assertTrue(sections.first { it.id == "harvard-classics" }.hasMore)
        assertFalse(sections.first { it.id == "poetry" }.hasMore)
    }

    @Test
    fun `recently added offers view more when it is full`() {
        assertTrue(build(recentlyAdded = books(10)).first { it.id == "recently-added" }.hasMore)
        assertFalse(build(recentlyAdded = books(4)).first { it.id == "recently-added" }.hasMore)
    }

    @Test
    fun `each slider leads to its own list`() {
        val sections = build()

        assertEquals(ViewMoreTarget.ContinueReading, sections.first { it.id == "continue-reading" }.viewMore)
        assertEquals(ViewMoreTarget.WantToRead, sections.first { it.id == "want-to-read" }.viewMore)
        assertEquals(ViewMoreTarget.RecentlyAdded, sections.first { it.id == "recently-added" }.viewMore)
        assertEquals(ViewMoreTarget.CatalogShelf("Harvard Classics"), sections.first { it.id == "harvard-classics" }.viewMore)
    }

    @Test
    fun `the headings and books are carried over`() {
        val section = build(continueReading = slider(2)).first { it.id == "continue-reading" }

        assertEquals(R.string.home_continue_reading, section.titleRes)
        assertEquals(books(2), section.books)
    }
}
