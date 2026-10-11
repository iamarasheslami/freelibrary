package com.freelibrary.app.ui.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverGeometryTest {
    private val reference = CoverGeometry(240f, 360f)

    @Test
    fun `the reference cover uses the design numbers`() {
        assertEquals(28f, reference.textLeft, DELTA)
        assertEquals(192f, reference.textWidth, DELTA)
        assertEquals(288f, reference.horizonY, DELTA)
        assertEquals(332f, reference.authorBottom, DELTA)
    }

    @Test
    fun `every position scales linearly with the cover width`() {
        val double = CoverGeometry(480f, 720f)

        assertEquals(reference.textLeft * 2, double.textLeft, DELTA)
        assertEquals(reference.textWidth * 2, double.textWidth, DELTA)
        assertEquals(reference.ruleY * 2, double.ruleY, DELTA)
        assertEquals(reference.titleTop * 2, double.titleTop, DELTA)
        assertEquals(reference.authorSize * 2, double.authorSize, DELTA)
        assertEquals(reference.titleSizes.first() * 2, double.titleSizes.first(), DELTA)
    }

    @Test
    fun `the text column fits inside the cover`() {
        assertTrue(reference.textLeft >= 0f)
        assertTrue(reference.textLeft + reference.textWidth <= reference.width)
    }

    @Test
    fun `the decorations end above the author line`() {
        val authorTop = reference.authorBottom - reference.authorSize

        assertTrue(reference.horizonY < authorTop)
        assertTrue(reference.dotsBottom + reference.dotRadius < reference.horizonY)
    }

    @Test
    fun `the title starts below the gold rule`() {
        assertTrue(reference.titleTop > reference.ruleY + reference.ruleThickness)
    }

    @Test
    fun `title sizes shrink from the largest to the smallest`() {
        val sizes = reference.titleSizes

        assertEquals(30f, sizes.first(), DELTA)
        assertEquals(16f, sizes.last(), DELTA)
        assertEquals(sizes.sortedDescending(), sizes)
        assertEquals(sizes.size, sizes.toSet().size)
    }

    @Test
    fun `a tiny cover cannot draw text`() {
        assertFalse(CoverGeometry(20f, 30f).canDrawText)
        assertTrue(reference.canDrawText)
    }

    @Test
    fun `a cover of zero size still has finite geometry`() {
        val empty = CoverGeometry(0f, 0f)

        assertFalse(empty.canDrawText)
        assertTrue(empty.textWidth.isFinite() && empty.titleTop.isFinite() && empty.horizonY.isFinite())
    }

    @Test
    fun `small covers show only the title`() {
        assertTrue(CoverGeometry(150f, 225f).showsDetails)
        assertFalse(CoverGeometry(120f, 180f).showsDetails)
        assertFalse(CoverGeometry(0f, 0f).showsDetails)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
