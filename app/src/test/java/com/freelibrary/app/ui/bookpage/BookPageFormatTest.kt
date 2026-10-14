package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.domain.model.BookAuthorCredit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class BookPageFormatTest {
    @Test
    fun `an author with both years shows the range`() {
        assertEquals("Austen, Jane (1775–1817)", authorLine(BookAuthorCredit("Austen, Jane", 1775, 1817)))
    }

    @Test
    fun `an author with only a birth year shows an open range`() {
        assertEquals("Austen, Jane (1775–)", authorLine(BookAuthorCredit("Austen, Jane", 1775, null)))
    }

    @Test
    fun `an author with only a death year shows an open start`() {
        assertEquals("Austen, Jane (–1817)", authorLine(BookAuthorCredit("Austen, Jane", null, 1817)))
    }

    @Test
    fun `an author with no years shows the name alone`() {
        assertEquals("Anonymous", authorLine(BookAuthorCredit("Anonymous", null, null)))
    }

    @Test
    fun `a language code becomes its name in the reader's language`() {
        assertEquals("English", languageName("en", Locale.ENGLISH))
        assertEquals("French", languageName("fr", Locale.ENGLISH))
        assertEquals("Französisch", languageName("fr", Locale.GERMAN))
    }

    @Test
    fun `a missing or blank language gives nothing`() {
        assertNull(languageName(null, Locale.ENGLISH))
        assertNull(languageName("", Locale.ENGLISH))
        assertNull(languageName("   ", Locale.ENGLISH))
    }
}
