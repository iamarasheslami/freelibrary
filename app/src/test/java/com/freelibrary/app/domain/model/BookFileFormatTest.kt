package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookFileFormatTest {
    @Test
    fun `catalog names are matched without caring about case or spaces`() {
        assertEquals(BookFileFormat.EPUB, BookFileFormat.fromCatalog("EPUB"))
        assertEquals(BookFileFormat.PDF, BookFileFormat.fromCatalog(" pdf "))
        assertEquals(BookFileFormat.TEXT, BookFileFormat.fromCatalog("txt"))
    }

    @Test
    fun `a format the app cannot open is not known`() {
        assertNull(BookFileFormat.fromCatalog("mobi"))
    }

    @Test
    fun `the formats are offered with EPUB first, then PDF, then text, whatever order the catalog lists them`() {
        assertEquals(
            listOf(BookFileFormat.EPUB, BookFileFormat.PDF, BookFileFormat.TEXT),
            BookFileFormat.offered(listOf("txt", "pdf", "epub")),
        )
    }

    @Test
    fun `only formats the app can open are offered, each once`() {
        assertEquals(
            listOf(BookFileFormat.PDF, BookFileFormat.TEXT),
            BookFileFormat.offered(listOf("txt", "mobi", "pdf", "txt")),
        )
    }

    @Test
    fun `a book with no known format offers nothing`() {
        assertEquals(emptyList<BookFileFormat>(), BookFileFormat.offered(listOf("mobi")))
    }
}
