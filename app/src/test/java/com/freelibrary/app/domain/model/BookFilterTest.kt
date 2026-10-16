package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFilterTest {
    @Test
    fun `no choices means the catalog language and nothing active`() {
        val filter = BookFilter()

        assertEquals(CatalogDefaults.LANGUAGE, filter.browseLanguage)
        assertEquals(0, filter.activeCount)
        assertFalse(filter.isActive)
    }

    @Test
    fun `a chosen language is the one Explore lists`() {
        assertEquals("fr", BookFilter(language = "fr").browseLanguage)
    }

    @Test
    fun `every separate choice counts once`() {
        val filter = BookFilter(language = "fr", formats = setOf("epub", "pdf"), genres = setOf("Category: Fiction"))

        assertEquals(4, filter.activeCount)
        assertTrue(filter.isActive)
    }
}
