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

class WithdrawalTest {
    private fun book(id: String) =
        BookExport(
            externalId = id,
            title = "Book $id",
            issuedDate = null,
            language = "en",
            locc = null,
            creators = listOf(CreatorExport("Austen, Jane", 1775, 1817)),
            subjects = emptyList(),
            bookshelves = emptyList(),
            formats = listOf(FormatExport("https://www.gutenberg.org/ebooks/$id.epub.images", "epub")),
            lastModified = "2026-10-02",
        )

    private fun export(
        ids: List<String>,
        withdrawn: List<String> = emptyList(),
    ): Path {
        val dir = Files.createTempDirectory("withdrawal")
        val entries = ids.map { ManifestEntry(it, "2026-10-02") }
        writeManifest(dir, Manifest("2026-10-27", "2026-10-02", entries, withdrawn))
        ids.forEach { writeBookExport(dir.resolve("books"), book(it)) }
        return dir
    }

    @Test
    fun `a withdrawn book leaves the list and its file, and joins the withdrawn list in order`() {
        val dir = export(listOf("1342", "158", "84"), withdrawn = listOf("2"))

        val result = withdrawBooks(dir, listOf("158", "1342"), today = "2026-11-05")

        assertEquals(listOf("158", "1342"), result.withdrawn)
        val manifest = readManifest(dir)!!
        assertEquals(listOf("84"), manifest.books.map { it.externalId })
        assertEquals(listOf("1342", "158", "2"), manifest.withdrawn)
        assertEquals("2026-11-05", manifest.generatedAt)
        assertFalse(Files.exists(dir.resolve("books/158.json")))
        assertFalse(Files.exists(dir.resolve("books/1342.json")))
        assertTrue(Files.exists(dir.resolve("books/84.json")))
    }

    @Test
    fun `the result of a withdrawal passes the check against the earlier manifest`() {
        val dir = export(listOf("1342", "158"))
        val before = readManifest(dir)!!

        withdrawBooks(dir, listOf("158"), today = "2026-11-05")

        val report = validateExport(dir, before)
        assertTrue(report.errors.toString(), report.ok)
    }

    @Test
    fun `an id that is unknown, unsafe or already withdrawn is reported and changes nothing`() {
        val dir = export(listOf("1342"), withdrawn = listOf("158"))
        val before = Files.readString(dir.resolve("manifest.json"))

        val result = withdrawBooks(dir, listOf("158", "9999", "../x"), today = "2026-11-05")

        assertEquals(emptyList<String>(), result.withdrawn)
        assertEquals(listOf("158"), result.alreadyWithdrawn)
        assertEquals(listOf("9999"), result.unknown)
        assertEquals(listOf("../x"), result.unsafe)
        assertEquals(before, Files.readString(dir.resolve("manifest.json")))
        assertTrue(Files.exists(dir.resolve("books/1342.json")))
    }
}
