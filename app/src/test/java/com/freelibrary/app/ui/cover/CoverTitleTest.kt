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

    @Test
    fun `a subfield code after a colon is dropped and the subtitle follows it`() {
        assertEquals(
            CoverTitle("Medieval cities", "Their origins and the revival of trade"),
            CoverTitle.parse("Medieval cities : \$b Their origins and the revival of trade"),
        )
    }

    @Test
    fun `a subfield code with no colon in front of it splits the title`() {
        assertEquals(
            CoverTitle("Twilight", "A story of two villages"),
            CoverTitle.parse("Twilight \$b A story of two villages"),
        )
    }

    @Test
    fun `a subfield code at the end leaves no blank subtitle`() {
        assertEquals(CoverTitle("Hamlet", null), CoverTitle.parse("Hamlet \$b"))
    }

    @Test
    fun `a dollar sign inside real text is left alone`() {
        assertEquals(CoverTitle("Save \$5 now", null), CoverTitle.parse("Save \$5 now"))
        assertEquals(CoverTitle("Pay \$bill later", null), CoverTitle.parse("Pay \$bill later"))
    }
}
