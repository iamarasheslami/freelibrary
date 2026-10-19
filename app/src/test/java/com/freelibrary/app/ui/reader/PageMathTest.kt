package com.freelibrary.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageMathTest {
    @Test
    fun `the number of pages is the content width over the page width, rounded`() {
        assertEquals(3, PageMath.pageCount(3000, 1000))
        assertEquals(3, PageMath.pageCount(2999, 1000))
        assertEquals(3, PageMath.pageCount(3001, 1000))
        assertEquals(1, PageMath.pageCount(1000, 1000))
    }

    @Test
    fun `a chapter that is not laid out yet counts as one page`() {
        assertEquals(1, PageMath.pageCount(0, 1000))
        assertEquals(1, PageMath.pageCount(500, 1000))
        assertEquals(1, PageMath.pageCount(3000, 0))
    }

    @Test
    fun `the page showing is the nearest one to where the view is scrolled`() {
        assertEquals(0, PageMath.pageAt(0, 1000, 3))
        assertEquals(1, PageMath.pageAt(1000, 1000, 3))
        assertEquals(1, PageMath.pageAt(1499, 1000, 3))
        assertEquals(2, PageMath.pageAt(1501, 1000, 3))
        assertEquals(2, PageMath.pageAt(9000, 1000, 3))
        assertEquals(0, PageMath.pageAt(-50, 1000, 3))
    }

    @Test
    fun `a page is shown by scrolling to a multiple of the page width`() {
        assertEquals(2000, PageMath.scrollXOf(2, 1000, 3))
        assertEquals(2000, PageMath.scrollXOf(9, 1000, 3))
        assertEquals(0, PageMath.scrollXOf(-1, 1000, 3))
    }

    @Test
    fun `how far through the chapter a page is runs from nothing to everything`() {
        assertEquals(0, PageMath.permilleOf(0, 5))
        assertEquals(500, PageMath.permilleOf(2, 5))
        assertEquals(1000, PageMath.permilleOf(4, 5))
        assertEquals(500, PageMath.permilleOf(1, 3))
        assertEquals(0, PageMath.permilleOf(0, 1))
        assertEquals(0, PageMath.permilleOf(3, 1))
    }

    @Test
    fun `a place in the chapter falls on the nearest page`() {
        assertEquals(0, PageMath.pageOf(0, 5))
        assertEquals(1, PageMath.pageOf(250, 5))
        assertEquals(2, PageMath.pageOf(500, 5))
        assertEquals(4, PageMath.pageOf(1000, 5))
        assertEquals(4, PageMath.pageOf(5000, 5))
        assertEquals(0, PageMath.pageOf(-5, 5))
        assertEquals(0, PageMath.pageOf(800, 1))
    }

    @Test
    fun `a page read from a place and written back is the same page`() {
        for (count in 2..200) {
            for (page in 0 until count) {
                assertEquals("page $page of $count", page, PageMath.pageOf(PageMath.permilleOf(page, count), count))
            }
        }
    }

    @Test
    fun `a swipe turns forward when it moves left and back when it moves right`() {
        assertEquals(true, PageMath.swipeForward(-100f, 50f))
        assertEquals(true, PageMath.swipeForward(-50f, 50f))
        assertEquals(false, PageMath.swipeForward(100f, 50f))
        assertNull(PageMath.swipeForward(30f, 50f))
        assertNull(PageMath.swipeForward(-30f, 50f))
    }

    @Test
    fun `a tap at an edge turns the page and a tap in the middle does not`() {
        assertEquals(false, PageMath.tapForward(100f, 1000))
        assertEquals(true, PageMath.tapForward(900f, 1000))
        assertNull(PageMath.tapForward(500f, 1000))
        assertNull(PageMath.tapForward(250f, 1000))
        assertNull(PageMath.tapForward(750f, 1000))
        assertEquals(true, PageMath.tapForward(751f, 1000))
    }
}
