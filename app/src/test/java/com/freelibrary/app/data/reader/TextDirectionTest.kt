package com.freelibrary.app.data.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDirectionTest {
    @Test
    fun `Arabic, Persian, Hebrew and Urdu run right to left`() {
        for (language in listOf("ar", "fa", "he", "ur", "ps")) {
            assertTrue(language, TextDirection.isRightToLeft(language))
        }
    }

    @Test
    fun `a region or a different case does not change the answer`() {
        assertTrue(TextDirection.isRightToLeft("ar-EG"))
        assertTrue(TextDirection.isRightToLeft("FA_ir"))
        assertTrue(TextDirection.isRightToLeft(" he "))
    }

    @Test
    fun `other languages and missing ones run left to right`() {
        assertFalse(TextDirection.isRightToLeft("en"))
        assertFalse(TextDirection.isRightToLeft("fr-CA"))
        assertFalse(TextDirection.isRightToLeft(""))
        assertFalse(TextDirection.isRightToLeft(null))
    }
}
