package com.freelibrary.app.ui.filter

import com.freelibrary.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FilterLabelsTest {
    @Test
    fun `a language code becomes the language's name`() {
        assertEquals("English", FilterLabels.languageLabel("en", Locale.ENGLISH))
        assertEquals("French", FilterLabels.languageLabel("fr", Locale.ENGLISH))
    }

    @Test
    fun `the name starts with a capital even where the language writes it small`() {
        assertEquals("Anglais", FilterLabels.languageLabel("en", Locale.FRENCH))
    }

    @Test
    fun `an unknown language code is shown in capitals`() {
        assertEquals("XX", FilterLabels.languageLabel("xx", Locale.ENGLISH))
    }

    @Test
    fun `a genre loses its Category prefix`() {
        assertEquals("Fiction", FilterLabels.genreLabel("Category: Fiction"))
        assertEquals("Poetry", FilterLabels.genreLabel("Poetry"))
    }

    @Test
    fun `known formats have a name and unknown ones do not`() {
        assertEquals(R.string.format_epub, FilterLabels.formatLabelRes("epub"))
        assertEquals(R.string.format_pdf, FilterLabels.formatLabelRes("pdf"))
        assertEquals(R.string.format_txt, FilterLabels.formatLabelRes("txt"))
        assertNull(FilterLabels.formatLabelRes("mobi"))
    }

    @Test
    fun `the icon and its description change while a filter is on`() {
        assertNotEquals(FilterLabels.iconRes(false), FilterLabels.iconRes(true))
        assertEquals(R.string.filter_title, FilterLabels.descriptionRes(false))
        assertEquals(R.string.filter_title_active, FilterLabels.descriptionRes(true))
    }
}
