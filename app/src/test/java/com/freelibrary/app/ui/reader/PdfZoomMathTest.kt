package com.freelibrary.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfZoomMathTest {
    @Test
    fun `the zoom stays between the whole page and the most it allows`() {
        assertEquals(1f, PdfZoomMath.clampZoom(0.4f), 0f)
        assertEquals(2.2f, PdfZoomMath.clampZoom(2.2f), 0f)
        assertEquals(PdfZoomMath.MAX_ZOOM, PdfZoomMath.clampZoom(9f), 0f)
    }

    @Test
    fun `a page smaller than the view is centred`() {
        assertEquals(100f, PdfZoomMath.clampTranslation(0f, 300f, 500f) * 1f - 0f, 0f)
        assertEquals(0f, PdfZoomMath.clampTranslation(40f, 500f, 500f), 0f)
    }

    @Test
    fun `a page larger than the view cannot be moved past its edges`() {
        assertEquals(0f, PdfZoomMath.clampTranslation(50f, 1000f, 400f), 0f)
        assertEquals(-600f, PdfZoomMath.clampTranslation(-900f, 1000f, 400f), 0f)
        assertEquals(-200f, PdfZoomMath.clampTranslation(-200f, 1000f, 400f), 0f)
    }

    @Test
    fun `zooming keeps the point under the fingers where it was`() {
        val translation = PdfZoomMath.translationAfterZoom(translation = 0f, focus = 100f, oldZoom = 1f, newZoom = 2f)

        assertEquals(-100f, translation, 0f)
        // The point of the page that was at 100 is now at (100 - translation) / zoom of the page's own scale: still 100.
        assertEquals(100f, (100f - translation) / 2f, 0f)
    }

    @Test
    fun `the drawing is sharpened in whole levels as the zoom grows`() {
        assertEquals(1, PdfZoomMath.renderLevel(1f))
        assertEquals(2, PdfZoomMath.renderLevel(1.2f))
        assertEquals(2, PdfZoomMath.renderLevel(2f))
        assertEquals(3, PdfZoomMath.renderLevel(2.5f))
        assertEquals(3, PdfZoomMath.renderLevel(3f))
        assertEquals(3, PdfZoomMath.renderLevel(9f))
    }

    @Test
    fun `a zoomed page is at an edge only when it touches that side of the view`() {
        assertEquals(false, PdfZoomMath.edgeOf(0f, 1000f, 400f))
        assertEquals(false, PdfZoomMath.edgeOf(-0.5f, 1000f, 400f))
        assertEquals(true, PdfZoomMath.edgeOf(-600f, 1000f, 400f))
        assertEquals(true, PdfZoomMath.edgeOf(-599.5f, 1000f, 400f))
        assertNull(PdfZoomMath.edgeOf(-300f, 1000f, 400f))
        assertNull(PdfZoomMath.edgeOf(-3f, 1000f, 400f))
    }

    @Test
    fun `a page that fits the view has no edge to push against`() {
        assertNull(PdfZoomMath.edgeOf(0f, 400f, 400f))
        assertNull(PdfZoomMath.edgeOf(50f, 300f, 400f))
    }

    @Test
    fun `pushing right against the left edge goes back and pushing left against the right edge goes on`() {
        assertEquals(false, PdfZoomMath.turnAtEdge(startEdge = false, dx = 120f, threshold = 100f))
        assertEquals(true, PdfZoomMath.turnAtEdge(startEdge = true, dx = -120f, threshold = 100f))
    }

    @Test
    fun `a swipe away from the edge, a short push or a swipe from the middle turns nothing`() {
        assertNull(PdfZoomMath.turnAtEdge(startEdge = false, dx = -300f, threshold = 100f))
        assertNull(PdfZoomMath.turnAtEdge(startEdge = true, dx = 300f, threshold = 100f))
        assertNull(PdfZoomMath.turnAtEdge(startEdge = false, dx = 99f, threshold = 100f))
        assertNull(PdfZoomMath.turnAtEdge(startEdge = null, dx = 500f, threshold = 100f))
        assertNull(PdfZoomMath.turnAtEdge(startEdge = null, dx = -500f, threshold = 100f))
    }
}
