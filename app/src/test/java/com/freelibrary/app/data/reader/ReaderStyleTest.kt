package com.freelibrary.app.data.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderStyleTest {
    private fun style(
        paged: Boolean,
        rightToLeft: Boolean,
    ) = ReaderStyle(textColor = 0x112233, backgroundColor = 0xFFFFFF, linkColor = 0xAA0000, paged = paged, rightToLeft = rightToLeft)

    @Test
    fun `text blocks are justified in both views, without overriding the book's own centring`() {
        for (paged in listOf(true, false)) {
            val css = style(paged = paged, rightToLeft = false).css()

            assertTrue(css.contains("p, li, dd, blockquote { text-align: justify; }"))
            assertFalse(css.contains("text-align: justify !important"))
        }
    }

    @Test
    fun `a left to right book gets no direction rule`() {
        assertFalse(style(paged = true, rightToLeft = false).css().contains("direction: rtl"))
        assertFalse(style(paged = false, rightToLeft = false).css().contains("direction: rtl"))
    }

    @Test
    fun `in page view a right to left book flips its content but not its columns`() {
        val css = style(paged = true, rightToLeft = true).css()

        assertTrue(css.contains("body > * { direction: rtl !important; }"))
        assertFalse(css.contains("body { direction: rtl"))
    }

    @Test
    fun `in scroll view a right to left book flips the whole body`() {
        assertTrue(style(paged = false, rightToLeft = true).css().contains("body { direction: rtl !important; }"))
    }
}
