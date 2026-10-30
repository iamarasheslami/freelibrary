package com.freelibrary.app.ui.reader

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderChromeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun bars() = listOf(View(context), View(context))

    @Test
    fun `a reader starts with the bars showing`() {
        val bars = bars()

        val chrome = ReaderChrome(bars, alwaysShown = false)

        assertTrue(chrome.shown)
        bars.forEach { assertEquals(View.VISIBLE, it.visibility) }
    }

    @Test
    fun `a tap hides the bars, keeping their space, and the next tap shows them`() {
        val bars = bars()
        val chrome = ReaderChrome(bars, alwaysShown = false)

        chrome.setShown(false, animate = false)
        assertFalse(chrome.shown)
        bars.forEach { assertEquals(View.INVISIBLE, it.visibility) }

        chrome.setShown(true, animate = false)
        assertTrue(chrome.shown)
        bars.forEach { assertEquals(View.VISIBLE, it.visibility) }
    }

    @Test
    fun `the bars stay with a screen reader on`() {
        val bars = bars()
        val chrome = ReaderChrome(bars, alwaysShown = true)

        chrome.setShown(false, animate = false)

        assertTrue(chrome.shown)
        bars.forEach { assertEquals(View.VISIBLE, it.visibility) }
    }
}
