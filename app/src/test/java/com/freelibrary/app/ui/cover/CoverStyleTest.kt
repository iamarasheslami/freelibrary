package com.freelibrary.app.ui.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverStyleTest {
    @Test
    fun `a key always gets the same style`() {
        assertEquals(CoverStyle.forKey("1342"), CoverStyle.forKey("1342"))
    }

    @Test
    fun `an empty key still gets a valid style`() {
        val style = CoverStyle.forKey("")

        assertTrue(style.backgroundColor in CoverStyle.BACKGROUNDS.toList())
    }

    @Test
    fun `keys spread evenly over every color and every motif`() {
        val styles = (1..KEY_COUNT).map { CoverStyle.forKey(it.toString()) }

        assertEvenlySpread(styles.groupingBy { it.backgroundColor }.eachCount(), CoverStyle.BACKGROUNDS.size)
        assertEvenlySpread(styles.groupingBy { it.motif }.eachCount(), CoverMotif.entries.size)
    }

    @Test
    fun `neighbouring ids rarely share a color`() {
        val repeats =
            (1 until KEY_COUNT).count {
                CoverStyle.forKey("$it").backgroundColor == CoverStyle.forKey("${it + 1}").backgroundColor
            }

        // Chance alone repeats about one color in twelve. Allow up to twice that.
        assertTrue("$repeats of ${KEY_COUNT - 1} neighbours repeated a color", repeats < (KEY_COUNT - 1) / 6)
    }

    private fun assertEvenlySpread(
        counts: Map<*, Int>,
        categories: Int,
    ) {
        assertEquals("every category is used", categories, counts.size)
        val expected = KEY_COUNT.toDouble() / categories
        counts.forEach { (category, count) ->
            assertTrue(
                "$category appeared $count times, expected about ${expected.toInt()}",
                count in (expected * 0.7).toInt()..(expected * 1.3).toInt(),
            )
        }
    }

    private companion object {
        const val KEY_COUNT = 3000
    }
}
