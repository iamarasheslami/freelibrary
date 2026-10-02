package com.freelibrary.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {
    @Test
    fun `fold lowercases, trims and collapses spaces`() {
        assertEquals("pride and prejudice", SearchText.fold("  Pride   AND Prejudice "))
    }

    @Test
    fun `fold removes accents`() {
        assertEquals("bronte", SearchText.fold("Brontë"))
        assertEquals("les miserables", SearchText.fold("Les Misérables"))
        assertEquals("vaino linna", SearchText.fold("Väinö Linna"))
    }

    @Test
    fun `fold maps letters that do not decompose`() {
        assertEquals("strasse", SearchText.fold("Straße"))
        assertEquals("aero", SearchText.fold("Ærø"))
        assertEquals("oeuvres", SearchText.fold("Œuvres"))
        assertEquals("lodz", SearchText.fold("Łódź"))
    }

    @Test
    fun `fold joins words at apostrophes in possessives and contractions`() {
        assertEquals("kings", SearchText.fold("King's"))
        assertEquals("dont", SearchText.fold("Don’t"))
        assertEquals("obrien", SearchText.fold("O'Brien"))
        assertEquals("hells angels", SearchText.fold("Hell's Angels"))
    }

    @Test
    fun `fold splits French and Italian elisions into two words`() {
        assertEquals("l homme qui rit", SearchText.fold("L'Homme qui rit"))
        assertEquals("dell arte", SearchText.fold("dell'arte"))
        assertEquals("d artagnan", SearchText.fold("D'Artagnan"))
    }

    @Test
    fun `fold turns other punctuation into spaces`() {
        assertEquals("dumas alexandre", SearchText.fold("Dumas, Alexandre"))
        assertEquals("jean jacques", SearchText.fold("Jean-Jacques"))
        assertEquals("hello", SearchText.fold("Hello!!!"))
    }

    @Test
    fun `fold keeps digits`() {
        assertEquals("1984", SearchText.fold("1984"))
        assertEquals("catch 22", SearchText.fold("Catch-22"))
    }

    @Test
    fun `fold keeps non-Latin letters`() {
        assertEquals("война и мир", SearchText.fold("Война и Мир"))
        assertEquals("红楼梦", SearchText.fold("红楼梦"))
    }

    @Test
    fun `fold keeps marks that are part of a non-Latin letter`() {
        assertEquals("война", SearchText.fold("Война"))
        assertEquals("がっこう", SearchText.fold("がっこう"))
    }

    @Test
    fun `fold strips Greek accents`() {
        assertEquals("ιλιας", SearchText.fold("Ἰλιάς"))
    }

    @Test
    fun `fold keeps a word with vowel-sign marks as one word`() {
        assertEquals(1, SearchText.tokens("हिन्दी").size)
    }

    @Test
    fun `fold of text with no letters or digits is empty`() {
        assertEquals("", SearchText.fold("  !!! "))
    }

    @Test
    fun `fold is idempotent`() {
        val once = SearchText.fold("Les Misérables: D'Artagnan, Straße & Co.")
        assertEquals(once, SearchText.fold(once))
    }

    @Test
    fun `tokens returns the folded words in order`() {
        assertEquals(listOf("the", "art", "of", "war"), SearchText.tokens("The Art of War"))
    }

    @Test
    fun `tokens of blank text is empty`() {
        assertTrue(SearchText.tokens("   ").isEmpty())
    }
}
