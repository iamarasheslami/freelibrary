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

    @Test
    fun `the app size scales the text size of the page`() {
        val normal = style(paged = false, rightToLeft = false).copy(textPercent = 100, appScale = 1f).css()
        val small = style(paged = false, rightToLeft = false).copy(textPercent = 100, appScale = 0.85f).css()

        assertTrue(normal.contains("font-size: 100% !important"))
        assertTrue(small.contains("font-size: 85% !important"))
    }

    @Test
    fun `the app size is applied after the reader's own minimum and maximum`() {
        val css = style(paged = true, rightToLeft = false).copy(textPercent = 70, appScale = 0.85f).css()

        assertTrue(css.contains("font-size: 60% !important"))
    }

    private val lora =
        ReaderFont(
            listOf(
                WebFont("lora.ttf", "100 900", italic = false),
                WebFont("lora_italic.ttf", "100 900", italic = true),
            ),
        )

    @Test
    fun `a book keeps its own font when no font is chosen`() {
        assertFalse(style(paged = true, rightToLeft = false).css().contains("font-family"))
        assertFalse(style(paged = false, rightToLeft = false).css().contains("@font-face"))
    }

    @Test
    fun `a chosen font is loaded from the server and replaces the book's, except for code`() {
        for (paged in listOf(true, false)) {
            val css = style(paged = paged, rightToLeft = false).copy(font = lora).css()

            assertTrue(css.contains("src: url('${EpubPageServer.FONT_URL}lora.ttf') format('truetype')"))
            assertTrue(css.contains("font-style: italic; font-weight: 100 900; src: url('${EpubPageServer.FONT_URL}lora_italic.ttf')"))
            assertTrue(css.contains("body, body * { font-family: '${ReaderFont.FAMILY}', serif !important; }"))
            assertTrue(css.contains("pre, pre *, code, kbd, samp, tt { font-family: monospace !important; }"))
        }
    }

    @Test
    fun `an opentype file is declared as opentype`() {
        val font = ReaderFont(listOf(WebFont("a.otf", "400", italic = false)))

        val css = style(paged = true, rightToLeft = false).copy(font = font).css()

        assertTrue(css.contains("format('opentype')"))
    }

    @Test
    fun `changing the font changes the style sheet, so a cached sheet is never reused`() {
        val plain = style(paged = true, rightToLeft = false)

        assertTrue(plain.css().hashCode() != plain.copy(font = lora).css().hashCode())
    }
}
