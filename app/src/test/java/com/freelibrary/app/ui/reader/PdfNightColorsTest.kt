package com.freelibrary.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfNightColorsTest {
    private val paper = 0x141716
    private val ink = 0xE6E3DC

    /** What the matrix does to one colour channel (0 red, 1 green, 2 blue) holding [value] from 0 to 255. */
    private fun mapped(
        matrix: FloatArray,
        channel: Int,
        value: Float,
    ): Float = matrix[channel * 5 + channel] * value + matrix[channel * 5 + 4]

    @Test
    fun `white becomes the paper colour`() {
        val matrix = PdfNightColors.matrix(paper, ink)
        assertEquals(0x14.toFloat(), mapped(matrix, 0, 255f), 0.01f)
        assertEquals(0x17.toFloat(), mapped(matrix, 1, 255f), 0.01f)
        assertEquals(0x16.toFloat(), mapped(matrix, 2, 255f), 0.01f)
    }

    @Test
    fun `black becomes the ink colour`() {
        val matrix = PdfNightColors.matrix(paper, ink)
        assertEquals(0xE6.toFloat(), mapped(matrix, 0, 0f), 0.01f)
        assertEquals(0xE3.toFloat(), mapped(matrix, 1, 0f), 0.01f)
        assertEquals(0xDC.toFloat(), mapped(matrix, 2, 0f), 0.01f)
    }

    @Test
    fun `a mid grey lands between paper and ink`() {
        val matrix = PdfNightColors.matrix(paper, ink)
        val red = mapped(matrix, 0, 127.5f)
        assertTrue(red > 0x14 && red < 0xE6)
    }

    @Test
    fun `the alpha channel is left alone and nothing leaks between channels`() {
        val matrix = PdfNightColors.matrix(paper, ink)
        assertEquals(1f, matrix[18], 0f)
        assertEquals(0f, matrix[19], 0f)
        assertEquals(0f, matrix[1], 0f)
        assertEquals(0f, matrix[5], 0f)
    }

    @Test
    fun `dark colours are told from light ones`() {
        assertTrue(PdfNightColors.isDark(0x141716))
        assertTrue(PdfNightColors.isDark(0x000000))
        assertFalse(PdfNightColors.isDark(0xFAF8F4))
        assertFalse(PdfNightColors.isDark(0xFFFFFF))
    }
}
