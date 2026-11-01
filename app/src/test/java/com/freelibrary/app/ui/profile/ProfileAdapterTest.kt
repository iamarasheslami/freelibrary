package com.freelibrary.app.ui.profile

import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ProfileCounts
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.domain.model.ReadingSummary
import com.freelibrary.app.domain.model.ViewMoreTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAdapterTest {
    private fun book(id: Long) = BookCard(id, "$id", "Book $id", null, null)

    private fun section(
        id: String,
        vararg books: BookCard,
    ) = ProfileSection(id, 0, books.toList(), false, ViewMoreTarget.WantToRead)

    @Test
    fun `the header comes first and the shelves follow in order`() {
        val state = ProfileState(name = "Arash", sections = listOf(section("a", book(1)), section("b", book(2))))

        val rows = profileRows(state)

        assertEquals(4, rows.size)
        assertTrue(rows[0] is ProfileRow.Header)
        assertTrue(rows[1] is ProfileRow.Reading)
        assertEquals(listOf("a", "b"), rows.drop(2).map { (it as ProfileRow.Slider).section.id })
    }

    @Test
    fun `a shelf with no books is left out`() {
        val state = ProfileState(sections = listOf(section("a"), section("b", book(1)), section("c")))

        val rows = profileRows(state)

        assertEquals(listOf("b"), rows.drop(2).map { (it as ProfileRow.Slider).section.id })
    }

    @Test
    fun `with no books at all only the header and the reading card are left`() {
        val rows = profileRows(ProfileState(sections = listOf(section("a"), section("b"))))

        assertEquals(2, rows.size)
        assertTrue(rows[0] is ProfileRow.Header)
        assertTrue(rows[1] is ProfileRow.Reading)
    }

    @Test
    fun `the header carries the name picture and totals`() {
        val state = ProfileState(name = "Arash", avatarVersion = 5L, counts = ProfileCounts(2, 3))

        val header = profileRows(state).first() as ProfileRow.Header

        assertEquals(ProfileRow.Header("Arash", 5L, 2, 3), header)
    }

    @Test
    fun `a shelf is the same item while its id is the same`() {
        val diff = ProfileAdapter.DIFF
        val before = ProfileRow.Slider(section("want-to-read"))
        val after = ProfileRow.Slider(section("want-to-read", book(1)))

        assertTrue(diff.areItemsTheSame(before, after))
        assertFalse(diff.areContentsTheSame(before, after))
        assertFalse(diff.areItemsTheSame(before, ProfileRow.Slider(section("finished"))))
    }

    @Test
    fun `the reading card carries the year summary`() {
        val summary = ReadingSummary(year = 2026, finishedThisYear = 3, finishedThisMonth = 1, goal = 12)

        val card = profileRows(ProfileState(reading = summary))[1] as ProfileRow.Reading

        assertEquals(summary, card.summary)
    }

    @Test
    fun `there is only ever one reading card`() {
        val diff = ProfileAdapter.DIFF

        assertTrue(diff.areItemsTheSame(ProfileRow.Reading(ReadingSummary()), ProfileRow.Reading(ReadingSummary(goal = 5))))
        assertFalse(diff.areContentsTheSame(ProfileRow.Reading(ReadingSummary()), ProfileRow.Reading(ReadingSummary(goal = 5))))
        assertFalse(diff.areItemsTheSame(ProfileRow.Reading(ReadingSummary()), ProfileRow.Header(null, null, 0, 0)))
    }

    @Test
    fun `there is only ever one header`() {
        val diff = ProfileAdapter.DIFF

        assertTrue(diff.areItemsTheSame(ProfileRow.Header(null, null, 0, 0), ProfileRow.Header("Arash", 1L, 2, 3)))
    }
}
