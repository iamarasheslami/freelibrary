package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.CreatorExport
import com.freelibrary.shared.FormatExport
import com.freelibrary.shared.Manifest
import com.freelibrary.shared.ManifestEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ExportValidatorTest {
    private fun book(
        id: String = "1342",
        date: String = "2026-10-02",
        title: String = "Pride and Prejudice",
        url: String = "https://www.gutenberg.org/ebooks/1342.epub.images",
    ) = BookExport(
        externalId = id,
        title = title,
        issuedDate = null,
        language = "en",
        locc = null,
        creators = listOf(CreatorExport("Austen, Jane", 1775, 1817)),
        subjects = emptyList(),
        bookshelves = emptyList(),
        formats = listOf(FormatExport(url, "epub")),
        lastModified = date,
    )

    private fun manifest(vararg entries: Pair<String, String>) =
        Manifest("2026-10-27", "2026-10-02", entries.map { ManifestEntry(it.first, it.second) })

    private fun export(
        manifest: Manifest,
        vararg books: BookExport,
    ): Path {
        val dir = Files.createTempDirectory("export-validator")
        writeManifest(dir, manifest)
        books.forEach { writeBookExport(dir.resolve("books"), it) }
        return dir
    }

    @Test
    fun `a consistent export is accepted`() {
        val dir = export(manifest("1342" to "2026-10-02"), book())

        val report = validateExport(dir)

        assertTrue(report.errors.toString(), report.ok)
        assertEquals(1, report.bookCount)
    }

    @Test
    fun `a missing manifest is an error`() {
        val report = validateExport(Files.createTempDirectory("export-validator"))

        assertFalse(report.ok)
    }

    @Test
    fun `a manifest that is not valid JSON is an error`() {
        val dir = Files.createTempDirectory("export-validator")
        Files.writeString(dir.resolve("manifest.json"), "{ not json")

        assertFalse(validateExport(dir).ok)
    }

    @Test
    fun `an empty manifest is an error`() {
        assertFalse(validateExport(export(manifest())).ok)
    }

    @Test
    fun `a listed book without a file is an error`() {
        val dir = export(manifest("1342" to "2026-10-02", "158" to "2026-10-02"), book())

        val report = validateExport(dir)

        assertEquals(1, report.errorCount)
        assertTrue(report.errors.single().contains("158"))
    }

    @Test
    fun `a file that is not in the manifest is an error`() {
        val dir = export(manifest("1342" to "2026-10-02"), book(), book(id = "158"))

        assertTrue(validateExport(dir).errors.any { it.contains("158.json") })
    }

    @Test
    fun `a file that cannot be decoded is an error`() {
        val dir = export(manifest("1342" to "2026-10-02"), book())
        Files.writeString(dir.resolve("books/1342.json"), "{\"externalId\":\"1342\"}")

        assertFalse(validateExport(dir).ok)
    }

    @Test
    fun `a file that holds another book is an error`() {
        val dir = export(manifest("1342" to "2026-10-02"), book(id = "158"))
        Files.move(dir.resolve("books/158.json"), dir.resolve("books/1342.json"))

        assertTrue(validateExport(dir).errors.any { it.contains("holds book 158") })
    }

    @Test
    fun `a date that differs between the manifest and the file is an error`() {
        val dir = export(manifest("1342" to "2026-10-27"), book(date = "2026-10-02"))

        assertFalse(validateExport(dir).ok)
    }

    @Test
    fun `an unsafe id or a duplicate id is an error`() {
        val unsafe = export(manifest("../x" to "2026-10-02", "1342" to "2026-10-02"), book())
        val twice = export(manifest("1342" to "2026-10-02", "1342" to "2026-10-02"), book())

        assertTrue(validateExport(unsafe).errors.any { it.contains("Unsafe book id") })
        assertTrue(validateExport(twice).errors.any { it.contains("twice") })
    }

    @Test
    fun `a blank title is an error but a plain http address is only a warning`() {
        val blank = export(manifest("1342" to "2026-10-02"), book(title = " "))
        val http = export(manifest("1342" to "2026-10-02"), book(url = "http://example.org/1342.epub"))

        assertFalse(validateExport(blank).ok)
        val report = validateExport(http)
        assertTrue(report.ok)
        assertEquals(1, report.warningCount)
    }

    @Test
    fun `a book that disappeared from the published manifest is an error`() {
        val dir = export(manifest("1342" to "2026-10-02"), book())
        val published = manifest("1342" to "2026-10-02", "158" to "2026-10-02")

        assertTrue(validateExport(dir, published).errors.any { it.contains("158") && it.contains("gone") })
    }

    @Test
    fun `a date that went backwards is an error and a newer one is accepted`() {
        val dir = export(manifest("1342" to "2026-10-02"), book(date = "2026-10-02"))

        assertFalse(validateExport(dir, manifest("1342" to "2026-10-19")).ok)
        assertTrue(validateExport(dir, manifest("1342" to "2026-09-19")).ok)
    }
}
