package com.freelibrary.app.ui.booklist

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HorizontalGapDecorationTest {
    private fun offsets(
        position: Int,
        rightToLeft: Boolean,
    ): Rect = Rect().also { applyGap(it, position, gapPx = 8, rightToLeft = rightToLeft) }

    @Test
    fun `the first item has no gap`() {
        assertEquals(Rect(0, 0, 0, 0), offsets(position = 0, rightToLeft = false))
        assertEquals(Rect(0, 0, 0, 0), offsets(position = 0, rightToLeft = true))
    }

    @Test
    fun `later items get the gap on their left in left-to-right layouts`() {
        assertEquals(Rect(8, 0, 0, 0), offsets(position = 3, rightToLeft = false))
    }

    @Test
    fun `later items get the gap on their right in right-to-left layouts`() {
        assertEquals(Rect(0, 0, 8, 0), offsets(position = 3, rightToLeft = true))
    }

    @Test
    fun `an item with an unknown position gets no gap`() {
        assertEquals(Rect(0, 0, 0, 0), offsets(position = -1, rightToLeft = false))
    }
}
