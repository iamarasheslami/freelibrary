package com.freelibrary.app.ui.insets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellInsetsTest {
    @Test
    fun `below edge to edge nothing extra is applied and the bar pads for the navigation bar`() {
        val result = shellInsets(false, 10, 20, 48, 300, bottomBarShown = true)
        assertEquals(ShellInsets(0, 0, 0, 48, true), result)
    }

    @Test
    fun `below edge to edge the screens pad for the navigation bar when no bottom bar shows`() {
        assertFalse(shellInsets(false, 0, 0, 48, 0, bottomBarShown = false).consumeBottomSystemBar)
    }

    @Test
    fun `edge to edge keeps the content clear of the side bars`() {
        val result = shellInsets(true, 24, 48, 0, 0, bottomBarShown = true)
        assertEquals(24, result.left)
        assertEquals(48, result.right)
    }

    @Test
    fun `edge to edge without a keyboard the bar pads for the navigation bar`() {
        val result = shellInsets(true, 0, 0, 48, 0, bottomBarShown = true)
        assertEquals(0, result.contentBottom)
        assertEquals(48, result.barBottom)
        assertTrue(result.consumeBottomSystemBar)
    }

    @Test
    fun `edge to edge with a keyboard the content is lifted by all of it and nothing pads twice`() {
        val result = shellInsets(true, 0, 0, 48, 800, bottomBarShown = false)
        assertEquals(800, result.contentBottom)
        assertEquals(0, result.barBottom)
        assertTrue(result.consumeBottomSystemBar)
    }

    @Test
    fun `edge to edge without a bottom bar or keyboard the screens pad for the navigation bar`() {
        assertFalse(shellInsets(true, 0, 0, 48, 0, bottomBarShown = false).consumeBottomSystemBar)
    }
}
