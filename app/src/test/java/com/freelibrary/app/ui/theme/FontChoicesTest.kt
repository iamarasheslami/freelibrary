package com.freelibrary.app.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontChoicesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `the reader font starts as Literata, a book keeps its own font and the text colour follows the theme`() {
        assertEquals(AppFont.LITERATA, FontChoices.textFont(context))
        assertNull(FontChoices.epubFont(context))
        assertEquals(TextColour.DEFAULT, FontChoices.textColour(context))
    }

    @Test
    fun `every choice is remembered on its own`() {
        FontChoices.setTextFont(context, AppFont.ATKINSON)
        FontChoices.setEpubFont(context, AppFont.OPEN_DYSLEXIC)
        FontChoices.setTextColour(context, TextColour.BROWN)

        assertEquals(AppFont.ATKINSON, FontChoices.textFont(context))
        assertEquals(AppFont.OPEN_DYSLEXIC, FontChoices.epubFont(context))
        assertEquals(TextColour.BROWN, FontChoices.textColour(context))
    }

    @Test
    fun `an EPUB can go back to the book's own font`() {
        FontChoices.setEpubFont(context, AppFont.EB_GARAMOND)
        FontChoices.setEpubFont(context, null)

        assertNull(FontChoices.epubFont(context))
    }

    @Test
    fun `an unknown stored value falls back to the default`() {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putString("text_font", "").putString("epub_font", "gone").putString("text_colour", "gone")
            .commit()

        assertEquals(AppFont.LITERATA, FontChoices.textFont(context))
        assertNull(FontChoices.epubFont(context))
        assertEquals(TextColour.DEFAULT, FontChoices.textColour(context))
    }

    @Test
    fun `reset forgets every choice`() {
        FontChoices.setTextFont(context, AppFont.ATKINSON)
        FontChoices.setEpubFont(context, AppFont.OPEN_DYSLEXIC)
        FontChoices.setTextColour(context, TextColour.RED)

        FontChoices.reset(context)

        assertEquals(AppFont.LITERATA, FontChoices.textFont(context))
        assertNull(FontChoices.epubFont(context))
        assertEquals(TextColour.DEFAULT, FontChoices.textColour(context))
    }
}
