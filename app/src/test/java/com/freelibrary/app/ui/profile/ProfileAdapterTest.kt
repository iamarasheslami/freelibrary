package com.freelibrary.app.ui.profile

import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ProfileCounts
import com.freelibrary.app.domain.model.ProfileSection
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
    ) = ProfileSection(id, 0, 0, books.toList(), false, ViewMoreTarget.WantToRead)

    @Test
    fun `the header comes first and the shelves follow in order`() {
        val state = ProfileState(name = "Arash", sections = listOf(section("a"), section("b")))

        val rows = profileRows(state)

        assertEquals(3, rows.size)
        assertTrue(rows[0] is ProfileRow.Header)
        assertEquals(listOf("a", "b"), rows.drop(1).map { (it as ProfileRow.Slider).section.id })
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
    fun `there is only ever one header`() {
        val diff = ProfileAdapter.DIFF

        assertTrue(diff.areItemsTheSame(ProfileRow.Header(null, null, 0, 0), ProfileRow.Header("Arash", 1L, 2, 3)))
    }
}
