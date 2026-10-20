package com.freelibrary.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import com.freelibrary.app.domain.model.ReaderPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderArgumentsTest {
    @Test
    fun `a place in the arguments is read`() {
        assertEquals(ReaderPosition.Pdf(3), SavedStateHandle(mapOf(ARG_START_POSITION to "pdf:3")).startPosition())
        assertEquals(ReaderPosition.Epub(2, 500), SavedStateHandle(mapOf(ARG_START_POSITION to "epub:2:500")).startPosition())
        assertEquals(ReaderPosition.Text(42), SavedStateHandle(mapOf(ARG_START_POSITION to "text:42")).startPosition())
    }

    @Test
    fun `no place, or text that is not a place, gives none`() {
        assertNull(SavedStateHandle().startPosition())
        assertNull(SavedStateHandle(mapOf(ARG_START_POSITION to null)).startPosition())
        assertNull(SavedStateHandle(mapOf(ARG_START_POSITION to "")).startPosition())
        assertNull(SavedStateHandle(mapOf(ARG_START_POSITION to "page 3")).startPosition())
        assertNull(SavedStateHandle(mapOf(ARG_START_POSITION to "epub:2")).startPosition())
    }
}
