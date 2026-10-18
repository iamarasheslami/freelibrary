package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReaderPositionTest {
    @Test
    fun `each kind of position is written with its kind first`() {
        assertEquals("epub:12:250", ReaderPosition.Epub(12, 250).encode())
        assertEquals("pdf:7", ReaderPosition.Pdf(7).encode())
        assertEquals("text:48210", ReaderPosition.Text(48210).encode())
    }

    @Test
    fun `a written position reads back as the same position`() {
        val positions =
            listOf(
                ReaderPosition.Epub(0, 0),
                ReaderPosition.Epub(3, 1000),
                ReaderPosition.Pdf(0),
                ReaderPosition.Pdf(999),
                ReaderPosition.Text(0),
                ReaderPosition.Text(Int.MAX_VALUE),
            )

        for (position in positions) {
            assertEquals(position, ReaderPosition.decode(position.encode()))
        }
    }

    @Test
    fun `text that is not a position is not decoded`() {
        val bad =
            listOf(
                "",
                "epub",
                "epub:1",
                "epub:1:2:3",
                "epub:1:1001",
                "epub:-1:5",
                "epub:1:-5",
                "epub:a:b",
                "pdf",
                "pdf:",
                "pdf:-1",
                "pdf:1:2",
                "pdf:1.5",
                "text:-3",
                "text:abc",
                "chapter:1",
                "12",
                "EPUB:1:2",
            )

        for (value in bad) {
            assertNull("'$value' should not decode", ReaderPosition.decode(value))
        }
    }

    @Test
    fun `a position that cannot exist cannot be made`() {
        assertThrows(IllegalArgumentException::class.java) { ReaderPosition.Epub(-1, 0) }
        assertThrows(IllegalArgumentException::class.java) { ReaderPosition.Epub(0, 1001) }
        assertThrows(IllegalArgumentException::class.java) { ReaderPosition.Pdf(-1) }
        assertThrows(IllegalArgumentException::class.java) { ReaderPosition.Text(-1) }
    }
}
