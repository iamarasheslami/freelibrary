package com.freelibrary.app.ui.theme

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.R
import com.google.android.material.color.MaterialColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderThemeTest {
    private val application: Context = ApplicationProvider.getApplicationContext()
    private val view = View(ContextThemeWrapper(application, R.style.Theme_FreeLibrary))

    @Test
    fun `Sepia is a light warm page in any app theme`() {
        val page = ReaderTheme.SEPIA.page(view)

        assertEquals(0xFFF7F0DF.toInt(), page.background)
        assertEquals(0xFF3B2F22.toInt(), page.ink)
        assertFalse(page.dark)
    }

    @Test
    fun `Dim night is a soft dark page in any app theme`() {
        val page = ReaderTheme.DIM.page(view)

        assertEquals(0xFF1C1A17.toInt(), page.background)
        assertEquals(0xFFB9B2A3.toInt(), page.ink)
        assertTrue(page.dark)
    }

    @Test
    fun `the app page takes the app's own colours`() {
        val page = ReaderTheme.APP.page(view)

        assertEquals(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface), page.background)
        assertEquals(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface), page.ink)
    }

    @Test
    fun `an unknown id is the app page and every id is its own`() {
        assertEquals(ReaderTheme.APP, ReaderTheme.fromId(null))
        assertEquals(ReaderTheme.APP, ReaderTheme.fromId("gone"))
        assertEquals(ReaderTheme.entries.size, ReaderTheme.entries.map { it.id }.toSet().size)
        ReaderTheme.entries.forEach { assertEquals(it, ReaderTheme.fromId(it.id)) }
    }

    @Test
    fun `a text colour takes the shade of the page and not of the app theme`() {
        val sepia = ReaderTheme.SEPIA.page(view)
        val dim = ReaderTheme.DIM.page(view)

        assertEquals(0xFF9A1010.toInt(), TextColour.RED.resolve(application, sepia))
        assertEquals(0xFFFF9A9A.toInt(), TextColour.RED.resolve(application, dim))
    }

    @Test
    fun `the default text colour is the page's own ink`() {
        val dim = ReaderTheme.DIM.page(view)

        assertEquals(dim.ink, TextColour.DEFAULT.resolve(application, dim))
    }
}
