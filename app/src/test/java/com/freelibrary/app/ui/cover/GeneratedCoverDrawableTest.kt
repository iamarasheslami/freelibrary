package com.freelibrary.app.ui.cover

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeneratedCoverDrawableTest {
    private data class Recorded(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val color: Int,
    )

    /** Records the rectangles a drawable paints, in order, with the color each was painted in. */
    private class RecordingCanvas : Canvas() {
        val rects = mutableListOf<Recorded>()

        override fun drawRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            paint: Paint,
        ) {
            rects += Recorded(left, top, right, bottom, paint.color)
        }
    }

    private val style = CoverStyle.forKey("1342")

    @Test
    fun `the background fills the whole cover first`() {
        val rects = draw(cover(), 240, 360)

        assertEquals(Recorded(0f, 0f, 240f, 360f, style.backgroundColor), rects.first())
    }

    @Test
    fun `a cover big enough for text draws the gold rule`() {
        val rects = draw(cover(), 240, 360)

        assertTrue(rects.any { it.color == CoverStyle.RULE_COLOR })
    }

    @Test
    fun `a tiny cover is only its background`() {
        val rects = draw(cover(), 20, 30)

        assertEquals(listOf(Recorded(0f, 0f, 20f, 30f, style.backgroundColor)), rects)
    }

    @Test
    fun `empty bounds draw nothing`() {
        assertTrue(draw(cover(), 0, 0).isEmpty())
    }

    @Test
    fun `any title and author draw without failing`() {
        val titles = listOf("", "   ", "x".repeat(5000), "كتاب الأدب والشعر", "📚📚📚", ";", "A;B;C:", "\n\n", "Wörterbuch der Straße")
        val authors = listOf(null, "", "   ", "Jane Austen", "y".repeat(2000), "مؤلف")
        val sizes = listOf(240 to 360, 120 to 180, 48 to 72, 10 to 10)
        for (title in titles) {
            for (author in authors) {
                for ((width, height) in sizes) {
                    val rects = draw(cover(title = title, author = author), width, height)

                    assertEquals(style.backgroundColor, rects.first().color)
                }
            }
        }
    }

    @Test
    fun `changing the size rebuilds the cover`() {
        val cover = cover()

        assertEquals(1, draw(cover, 20, 30).size)
        assertTrue(draw(cover, 240, 360).any { it.color == CoverStyle.RULE_COLOR })
        assertEquals(1, draw(cover, 20, 30).size)
    }

    @Test
    fun `the cover can be drawn with a reduced alpha`() {
        val cover = cover()
        cover.alpha = 128

        assertEquals(128, cover.alpha)
        assertEquals(style.backgroundColor, draw(cover, 240, 360).first().color)
    }

    private fun cover(
        title: String = "Pride and Prejudice",
        author: String? = "Jane Austen",
    ) = GeneratedCoverDrawable(style, title, author, Typeface.DEFAULT_BOLD, Typeface.DEFAULT)

    private fun draw(
        cover: GeneratedCoverDrawable,
        width: Int,
        height: Int,
    ): List<Recorded> {
        cover.setBounds(0, 0, width, height)
        val canvas = RecordingCanvas()
        cover.draw(canvas)
        return canvas.rects
    }
}
