package com.freelibrary.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrueBlackTest {
    @Test
    fun `it applies when dark mode is showing and the reader chose it`() {
        assertTrue(shouldUseTrueBlack(darkIsShowing = true, trueBlack = true))
    }

    @Test
    fun `it never applies in light mode, even when chosen`() {
        assertFalse(shouldUseTrueBlack(darkIsShowing = false, trueBlack = true))
    }

    @Test
    fun `it does not apply when the reader did not choose it`() {
        assertFalse(shouldUseTrueBlack(darkIsShowing = true, trueBlack = false))
        assertFalse(shouldUseTrueBlack(darkIsShowing = false, trueBlack = false))
    }
}
