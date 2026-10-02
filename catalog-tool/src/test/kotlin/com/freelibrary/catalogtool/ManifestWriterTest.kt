package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.CreatorExport
import com.freelibrary.shared.FormatExport
import com.freelibrary.shared.Manifest
import com.freelibrary.shared.ManifestEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ManifestWriterTest {
    private fun sampleBookExport() =
        BookExport(
            externalId = "1342",
            title = "Pride and Prejudice",
            issuedDate = "1998-06-01",
            language = "en",
            locc = "PR",
            creators = listOf(CreatorExport(name = "Jane Austen", birthYear = 1775, deathYear = 1817)),
            subjects = listOf("Fiction"),
            bookshelves = listOf("Best Books Ever Listings"),
            formats = listOf(FormatExport(url = "https://example.org/1342.txt", formatType = "txt")),
            lastModified = "2026-09-16",
        )

    @Test
    fun `writeBookExport creates a readable JSON file named after the external id`() {
        val tempDir = Files.createTempDirectory("book-export-test")

        writeBookExport(tempDir, sampleBookExport())

        val filePath = tempDir.resolve("1342.json")
        assertTrue(Files.exists(filePath))

        val decoded = Json.decodeFromString<BookExport>(Files.readString(filePath))
        assertEquals("Pride and Prejudice", decoded.title)
        assertEquals("Jane Austen", decoded.creators.first().name)
    }

    @Test
    fun `writeManifest creates a JSON file listing every book entry`() {
        val tempDir = Files.createTempDirectory("manifest-test")
        val manifest =
            Manifest(
                generatedAt = "2026-09-16",
                baselineVersion = "2026-09-16",
                books =
                    listOf(
                        ManifestEntry(externalId = "1342", lastModified = "2026-09-16"),
                        ManifestEntry(externalId = "158", lastModified = "2026-09-16"),
                    ),
            )

        writeManifest(tempDir, manifest)

        val filePath = tempDir.resolve("manifest.json")
        assertTrue(Files.exists(filePath))

        val decoded = Json.decodeFromString<Manifest>(Files.readString(filePath))
        assertEquals(2, decoded.books.size)
        assertEquals("1342", decoded.books.first().externalId)
    }

    @Test
    fun `toExport correctly maps a ParsedBook, including format type mapping`() {
        val parsedBook =
            ParsedBook(
                externalId = "1342",
                title = "Pride and Prejudice",
                issuedDate = "1998-06-01",
                language = "en",
                locc = "PR",
                creators = listOf(ParsedCreator(name = "Jane Austen", birthYear = 1775, deathYear = 1817)),
                subjects = listOf("Fiction"),
                bookshelves = listOf("Best Books Ever Listings"),
                formats = listOf(ParsedFormat(url = "https://example.org/1342.txt", mimeType = "text/plain; charset=utf-8")),
            )

        val export = parsedBook.toExport(lastModified = "2026-09-16")

        assertEquals("txt", export.formats.first().formatType)
        assertEquals("2026-09-16", export.lastModified)
    }

    @Test
    fun `toExport copies the summary and cover URL from the parsed book`() {
        val parsedBook =
            ParsedBook(
                externalId = "1342",
                title = "Pride and Prejudice",
                issuedDate = "1998-06-01",
                language = "en",
                locc = "PR",
                creators = emptyList(),
                subjects = emptyList(),
                bookshelves = emptyList(),
                formats = listOf(ParsedFormat(url = "https://example.org/1342.txt", mimeType = "text/plain; charset=utf-8")),
                summary = "A made-up summary.",
                coverUrl = "https://example.org/pg1342.cover.medium.jpg",
            )

        val export = parsedBook.toExport(lastModified = "2026-10-02")

        assertEquals("A made-up summary.", export.summary)
        assertEquals("https://example.org/pg1342.cover.medium.jpg", export.coverUrl)
    }

    @Test
    fun `writeBookExport keeps summary and cover URL through a write and read, including non-ASCII text`() {
        val tempDir = Files.createTempDirectory("book-export-summary-test")
        val book =
            sampleBookExport().copy(
                summary = "Un roman « fictif » — avec des accents: é, ü, ñ.",
                coverUrl = "https://example.org/pg1342.cover.medium.jpg",
            )

        writeBookExport(tempDir, book)

        val decoded = Json.decodeFromString<BookExport>(Files.readString(tempDir.resolve("1342.json")))
        assertEquals(book.summary, decoded.summary)
        assertEquals(book.coverUrl, decoded.coverUrl)
    }

    @Test
    fun `a book file written before summary and cover existed still decodes with both null`() {
        val oldStyleJson =
            """
            {"externalId":"1342","title":"Pride and Prejudice","issuedDate":"1998-06-01","language":"en",
            "locc":"PR","creators":[],"subjects":[],"bookshelves":[],"formats":[],"lastModified":"2026-09-19"}
            """.trimIndent()

        val decoded = Json.decodeFromString<BookExport>(oldStyleJson)

        assertEquals("Pride and Prejudice", decoded.title)
        assertNull(decoded.summary)
        assertNull(decoded.coverUrl)
    }
}
