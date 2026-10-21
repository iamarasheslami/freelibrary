package com.freelibrary.app.ui.reader

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPagingTest {
    /** Lines 10 tall, one after the other. */
    private fun starts(
        lines: Int,
        pageHeight: Int,
        lineHeight: Int = 10,
    ) = TextPaging.pageStarts(lines, { it * lineHeight }, { (it + 1) * lineHeight }, pageHeight)

    @Test
    fun `pages hold as many whole lines as fit`() {
        assertArrayEquals(intArrayOf(0, 3, 6, 9), starts(lines = 10, pageHeight = 35))
    }

    @Test
    fun `a line that is taller than the page gets a page of its own`() {
        assertArrayEquals(intArrayOf(0, 1, 2), starts(lines = 3, pageHeight = 5))
    }

    @Test
    fun `no lines make one empty page`() {
        assertArrayEquals(intArrayOf(0), starts(lines = 0, pageHeight = 100))
    }

    @Test
    fun `a line is found on its page`() {
        val pages = intArrayOf(0, 3, 6)
        assertEquals(0, TextPaging.pageOfLine(pages, 0))
        assertEquals(0, TextPaging.pageOfLine(pages, 2))
        assertEquals(1, TextPaging.pageOfLine(pages, 3))
        assertEquals(2, TextPaging.pageOfLine(pages, 99))
    }

    @Test
    fun `going back to a saved place never lands before it`() {
        val length = 12_345
        for (offset in listOf(0, 1, 7, 5_003, 12_344, 12_345)) {
            val back = TextPaging.offsetOf(TextPaging.permilleOf(offset, length), length)
            assertTrue("offset $offset came back as $back", back >= offset)
        }
    }

    @Test
    fun `the ends of a text are 0 and 1000`() {
        assertEquals(0, TextPaging.permilleOf(0, 500))
        assertEquals(1000, TextPaging.permilleOf(500, 500))
        assertEquals(0, TextPaging.permilleOf(5, 0))
        assertEquals(0, TextPaging.offsetOf(0, 500))
        assertEquals(500, TextPaging.offsetOf(1000, 500))
        assertEquals(500, TextPaging.offsetOf(5000, 500))
    }
}
