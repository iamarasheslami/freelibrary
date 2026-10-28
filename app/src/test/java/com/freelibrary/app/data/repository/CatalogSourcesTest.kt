package com.freelibrary.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogSourcesTest {
    @Test
    fun `Project Gutenberg keeps its files at the root of the data repository`() {
        assertEquals("manifest.json", CatalogSource.GUTENBERG.manifestPath)
        assertEquals("books/1342.json", CatalogSource.GUTENBERG.bookPath("1342"))
    }

    @Test
    fun `another source keeps its files in its own folder`() {
        val source = CatalogSource(name = "OAPEN Library", attribution = "x", folder = "sources/oapen/")

        assertEquals("sources/oapen/manifest.json", source.manifestPath)
        assertEquals("sources/oapen/books/60401.json", source.bookPath("60401"))
    }

    @Test
    fun `a folder must be empty or a relative path ending in a slash`() {
        for (folder in listOf("sources/oapen", "/sources/oapen/", "../oapen/")) {
            assertThrows(IllegalArgumentException::class.java) { CatalogSource(name = "x", attribution = "x", folder = folder) }
        }
    }

    @Test
    fun `a book id that could change the address is refused`() {
        for (id in listOf("", "../secret", "1/2", "1.5", "https://example.org/x", "a b", "1342\n", "x".repeat(41))) {
            assertEquals("\"$id\"", false, CatalogSource.isValidExternalId(id))
            assertThrows(IllegalArgumentException::class.java) { CatalogSource.GUTENBERG.bookPath(id) }
        }
        for (id in listOf("1342", "60401", "a-b_C9")) {
            assertEquals(true, CatalogSource.isValidExternalId(id))
        }
    }

    @Test
    fun `the app starts with Project Gutenberg as its only source`() {
        assertEquals(listOf(CatalogSource.GUTENBERG), CatalogSources().all)
    }

    @Test
    fun `two sources cannot share a name or a folder`() {
        val first = CatalogSource.GUTENBERG
        assertThrows(IllegalArgumentException::class.java) { CatalogSources(listOf(first, first.copy(folder = "sources/a/"))) }
        assertThrows(IllegalArgumentException::class.java) { CatalogSources(listOf(first, first.copy(name = "Other"))) }
        assertThrows(IllegalArgumentException::class.java) { CatalogSources(emptyList()) }
    }
}
