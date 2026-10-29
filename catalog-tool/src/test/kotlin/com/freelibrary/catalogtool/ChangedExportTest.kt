package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.CreatorExport
import com.freelibrary.shared.FormatExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChangedExportTest {
    private fun book(
        lastModified: String,
        title: String = "Persuasion",
        summary: String? = "A novel.",
        formats: List<FormatExport> = listOf(FormatExport(url = "https://example.org/105.epub", formatType = "epub")),
    ) = BookExport(
        externalId = "105",
        title = title,
        issuedDate = "1994-02-01",
        language = "en",
        locc = "PR",
        creators = listOf(CreatorExport(name = "Austen, Jane", birthYear = 1775, deathYear = 1817)),
        subjects = listOf("Love stories"),
        bookshelves = listOf("Category: Novels"),
        formats = formats,
        lastModified = lastModified,
        summary = summary,
        coverUrl = "https://example.org/pg105.cover.medium.jpg",
    )

    @Test
    fun `a book with no file yet is written with today's date`() {
        val fresh = book("2026-10-28")

        assertEquals(fresh, changedExport(fresh, previous = null))
    }

    @Test
    fun `a book that differs from its file only by the date is not written again`() {
        assertNull(changedExport(book("2026-10-28"), previous = book("2026-10-02")))
    }

    @Test
    fun `a changed title is written with today's date`() {
        val fresh = book("2026-10-28", title = "Persuasion (revised)")

        assertEquals(fresh, changedExport(fresh, previous = book("2026-10-02")))
    }

    @Test
    fun `a changed summary is written`() {
        val fresh = book("2026-10-28", summary = "A new summary.")

        assertEquals(fresh, changedExport(fresh, previous = book("2026-10-02")))
    }

    @Test
    fun `a changed list of formats is written`() {
        val fresh = book("2026-10-28", formats = emptyList())

        assertEquals(fresh, changedExport(fresh, previous = book("2026-10-02")))
    }
}
