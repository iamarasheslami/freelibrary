package com.freelibrary.app.ui.booklist

import org.junit.Assert.assertEquals
import org.junit.Test

class BookGridTest {
    @Test
    fun `as many cards fit as the width allows`() {
        assertEquals(2, gridColumnCount(availableWidthPx = 1000, cardWidthPx = 300, minGapPx = 40))
        assertEquals(3, gridColumnCount(availableWidthPx = 1020, cardWidthPx = 300, minGapPx = 40))
    }

    @Test
    fun `there is always at least one column`() {
        assertEquals(1, gridColumnCount(availableWidthPx = 100, cardWidthPx = 300, minGapPx = 40))
        assertEquals(1, gridColumnCount(availableWidthPx = 0, cardWidthPx = 300, minGapPx = 40))
    }

    @Test
    fun `a card with no size gives one column instead of a division by zero`() {
        assertEquals(1, gridColumnCount(availableWidthPx = 1000, cardWidthPx = 0, minGapPx = 0))
    }

    @Test
    fun `a card is centred in its column`() {
        assertEquals(100, cellSideInset(availableWidthPx = 1000, columns = 2, cardWidthPx = 300))
        assertEquals(20, cellSideInset(availableWidthPx = 1020, columns = 3, cardWidthPx = 300))
    }

    @Test
    fun `a column narrower than the card gets no negative inset`() {
        assertEquals(0, cellSideInset(availableWidthPx = 250, columns = 1, cardWidthPx = 300))
    }

    @Test
    fun `no columns means no inset`() {
        assertEquals(0, cellSideInset(availableWidthPx = 1000, columns = 0, cardWidthPx = 300))
    }

    @Test
    fun `the gap between neighbouring cards is never smaller than the minimum`() {
        val card = 300
        val minGap = 40
        (300..3000 step 7).forEach { width ->
            val columns = gridColumnCount(width, card, minGap)
            if (columns > 1) {
                val between = 2 * cellSideInset(width, columns, card)
                assertEquals("width $width", true, between >= minGap - 1)
            }
        }
    }
}
