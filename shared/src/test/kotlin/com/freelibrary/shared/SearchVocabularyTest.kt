package com.freelibrary.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVocabularyTest {
    private val vocabulary =
        SearchVocabulary(
            mapOf(
                "dickens" to 50,
                "dickinson" to 10,
                "dikes" to 3,
                "twain" to 40,
                "heights" to 30,
                "holmes" to 25,
                "home" to 300,
                "homes" to 20,
                "artagnan" to 4,
                "homme" to 50,
                "pride" to 100,
                "bride" to 60,
            ),
        )

    @Test
    fun `edit distance counts a swap of neighbouring letters as one edit`() {
        assertEquals(1, boundedEditDistance("hieghts", "heights", 2))
    }

    @Test
    fun `edit distance counts a missing or wrong letter as one edit`() {
        assertEquals(1, boundedEditDistance("prejudise", "prejudice", 2))
        assertEquals(1, boundedEditDistance("dikens", "dickens", 2))
    }

    @Test
    fun `edit distance gives up beyond the allowed number of edits`() {
        assertEquals(2, boundedEditDistance("abc", "xyz", 1))
        assertEquals(2, boundedEditDistance("a", "abcd", 1))
    }

    @Test
    fun `a misspelled word is corrected, preferring the word used by more books`() {
        assertEquals("dickens", vocabulary.correctionsFor("dikens").first())
    }

    @Test
    fun `a real word still offers its neighbours, so homes can reach holmes`() {
        val corrections = vocabulary.correctionsFor("homes")

        assertTrue("holmes" in corrections)
        assertTrue("home" in corrections)
        assertFalse("homes" in corrections)
    }

    @Test
    fun `words of three letters or fewer are never corrected`() {
        assertTrue(vocabulary.correctionsFor("pri").isEmpty())
    }

    @Test
    fun `a wrong first letter is only fixed by the widened pass`() {
        assertTrue(vocabulary.correctionsFor("bride").isEmpty())
        assertEquals(listOf("pride"), vocabulary.correctionsFor("bride", widen = true))
    }

    @Test
    fun `an elision typed without its apostrophe is offered the word after the article`() {
        assertEquals(listOf("artagnan"), vocabulary.correctionsFor("dartagnan"))
        assertEquals(listOf("homme"), vocabulary.correctionsFor("lhomme"))
    }

    @Test
    fun `the elision shortcut is skipped when the typed word is itself known`() {
        val known = SearchVocabulary(mapOf("dante" to 10, "ante" to 5))

        assertTrue(known.correctionsFor("dante").isEmpty())
    }

    @Test
    fun `the limit caps the number of corrections`() {
        assertEquals(1, vocabulary.correctionsFor("homes", limit = 1).size)
    }

    @Test
    fun `fromTexts counts each word once per text`() {
        val built = SearchVocabulary.fromTexts(sequenceOf("pride and pride", "pride of place"))

        assertTrue("pride" in built)
        assertEquals(4, built.size)
    }

    @Test
    fun `fromFoldedTexts builds the vocabulary from already folded text`() {
        val built = SearchVocabulary.fromFoldedTexts(sequenceOf("pride and pride", "pride of place", ""))

        assertEquals(4, built.size)
        assertTrue("place" in built)
    }
}
