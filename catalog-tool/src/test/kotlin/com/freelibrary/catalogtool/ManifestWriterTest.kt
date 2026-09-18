package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.CreatorExport
import com.freelibrary.shared.FormatExport
import com.freelibrary.shared.Manifest
import com.freelibrary.shared.ManifestEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}
