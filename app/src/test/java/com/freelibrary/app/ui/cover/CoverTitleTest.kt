package com.freelibrary.app.ui.cover

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverTitleTest {
    @Test
    fun `a semicolon splits the main title from the subtitle`() {
        assertEquals(
            CoverTitle("Frankenstein", "Or, The Modern Prometheus"),
            CoverTitle.parse("Frankenstein; Or, The Modern Prometheus"),
        )
    }

    @Test
    fun `a colon followed by a space splits too`() {
        assertEquals(CoverTitle("Moby Dick", "The Whale"), CoverTitle.parse("Moby Dick: The Whale"))
    }

    @Test
    fun `a title without a separator stays whole`() {
        assertEquals(CoverTitle("Pride and Prejudice", null), CoverTitle.parse("Pride and Prejudice"))
    }

    @Test
    fun `a colon inside a number does not split`() {
        assertEquals(CoverTitle("The 12:30 Train", null), CoverTitle.parse("The 12:30 Train"))
    }

    @Test
    fun `only the first separator splits`() {
        assertEquals(CoverTitle("A", "B; C"), CoverTitle.parse("A; B; C"))
    }

    @Test
    fun `line breaks and repeated spaces become single spaces`() {
        assertEquals(CoverTitle("Pride and Prejudice", null), CoverTitle.parse("Pride\r\nand   Prejudice"))
    }

    @Test
    fun `a leading separator leaves only the subtitle as the title`() {
        assertEquals(CoverTitle("Or, The Whale", null), CoverTitle.parse("; Or, The Whale"))
    }

    @Test
    fun `a trailing separator leaves no blank subtitle`() {
        assertEquals(CoverTitle("Hamlet", null), CoverTitle.parse("Hamlet;"))
        assertEquals(CoverTitle("Hamlet", null), CoverTitle.parse("Hamlet:"))
    }

    @Test
    fun `a blank title gives an empty main title`() {
        assertEquals(CoverTitle("", null), CoverTitle.parse(""))
        assertEquals(CoverTitle("", null), CoverTitle.parse("   "))
    }
}
