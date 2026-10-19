package com.freelibrary.app.ui.reader

import org.junit.Assert.assertEquals
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
}
