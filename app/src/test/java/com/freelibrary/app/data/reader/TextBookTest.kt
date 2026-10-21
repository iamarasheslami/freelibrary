package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile

class TextBookTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun book(text: String): TextBook = TextBook.fromText(text)!!

    private fun longText(paragraphs: Int = 120) = (1..paragraphs).joinToString("\n\n") { "a".repeat(1_000) + it }

    @Test
    fun `running prose is joined into a paragraph and verse keeps its line ends`() {
        val prose =
            "This line is long enough to be one of a wrapped paragraph.\n" +
                "This one is long enough as well, so they run together,\nand so ends it."
        val verse = "Roses are red\nViolets are blue\nSugar is sweet"

        val plain = book("$prose\n\n$verse").plain(0)

        val expectedProse =
            "This line is long enough to be one of a wrapped paragraph. " +
                "This one is long enough as well, so they run together, and so ends it."
        assertEquals("$expectedProse\n\n$verse", plain)
    }

    @Test
    fun `indented lines keep their line ends`() {
        val plain = book("    indented and quoted <b> & more\n    second line is indented as well").plain(0)

        assertEquals("    indented and quoted <b> & more\n    second line is indented as well", plain)
    }

    @Test
    fun `blank runs between paragraphs become one blank line`() {
        assertEquals("One\n\nTwo", book("One\n\n\n\n  \nTwo").plain(0))
    }

    @Test
    fun `a byte order mark and windows line ends are removed`() {
        val book = book("\uFEFFFirst line\r\nsecond line\r\n\r\nNext")

        assertEquals("First line\nsecond line\n\nNext".length, book.length)
    }

    @Test
    fun `text with nothing to read gives no book`() {
        assertNull(TextBook.fromText(""))
        assertNull(TextBook.fromText("  \n\n \t "))
    }

    @Test
    fun `a long book is cut into parts that follow each other and end at paragraph breaks`() {
        val text = longText()
        val parts = book(text).parts

        assertTrue(parts.size > 1)
        assertEquals(0, parts.first().start)
        assertEquals(text.length, parts.last().end)
        parts.zipWithNext().forEach { (before, after) ->
            assertEquals(before.end, after.start)
            assertEquals("\n", text.substring(after.start - 1, after.start))
            assertTrue(before.end - before.start >= TextBook.PART_TARGET_CHARS)
        }
    }

    @Test
    fun `a short book is one part, and a tiny rest is joined to the part before it`() {
        assertEquals(1, book("One paragraph.\n\nAnother one.").parts.size)

        val almostTwoParts = "a".repeat(TextBook.PART_TARGET_CHARS) + "\n\n" + "b".repeat(500)
        assertEquals(1, book(almostTwoParts).parts.size)
    }

    @Test
    fun `a book with no paragraph breaks is still cut at line ends`() {
        val text = (1..2_000).joinToString("\n") { "x".repeat(99) + it }

        assertTrue(book(text).parts.size > 1)
    }

    @Test
    fun `a place and a part with how far through it are converted both ways`() {
        val book = book(longText())

        assertEquals(0 to 0, book.locate(0))
        assertEquals(book.parts.lastIndex to 1000, book.locate(book.length))
        assertEquals(0 to 0, book.locate(-5))
        for (offset in listOf(1, 999, 40_100, 55_555, 90_000, book.length - 1)) {
            val (part, permille) = book.locate(offset)
            val range = book.parts[part]
            val back = book.offsetOf(part, permille)

            assertTrue("offset $offset in part $part", offset >= range.start && offset <= range.end)
            assertTrue("offset $offset came back as $back", kotlin.math.abs(back - offset) <= (range.end - range.start) / 1000 + 1)
        }
    }

    @Test
    fun `a file opens, in UTF-8 or else in windows-1252`() {
        val utf8 = temporaryFolder.newFile("utf8.txt")
        utf8.writeText("caf\u00e9 au lait")
        val old = temporaryFolder.newFile("old.txt")
        old.writeBytes(byteArrayOf('c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 0xE9.toByte()))

        val fromUtf8 = (TextBook.open(utf8) as TextOpenResult.Opened).book
        val fromOld = (TextBook.open(old) as TextOpenResult.Opened).book

        assertTrue(fromUtf8.plain(0).contains("caf\u00e9 au lait"))
        assertTrue(fromOld.plain(0).contains("caf\u00e9"))
    }

    @Test
    fun `an empty, missing or huge file does not open`() {
        val empty = temporaryFolder.newFile("empty.txt")
        assertEquals(TextOpenResult.Failed(TextProblem.EMPTY), TextBook.open(empty))
        assertEquals(TextOpenResult.Failed(TextProblem.UNREADABLE), TextBook.open(java.io.File(temporaryFolder.root, "gone.txt")))

        val huge = temporaryFolder.newFile("huge.txt")
        RandomAccessFile(huge, "rw").use { it.setLength(TextBook.MAX_FILE_BYTES + 1) }
        assertEquals(TextOpenResult.Failed(TextProblem.TOO_LARGE), TextBook.open(huge))
        assertNotNull(TextBook.fromText("fine"))
    }
}
