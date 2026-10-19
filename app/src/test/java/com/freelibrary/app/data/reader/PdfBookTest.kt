package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class PdfBookTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `a file that is not a PDF does not open`() {
        val file = temporaryFolder.newFile("a.pdf")
        file.writeText("Just some text, not a PDF.")

        assertEquals(PdfOpenResult.Failed(PdfProblem.NOT_A_PDF), PdfBook.open(file))
    }

    @Test
    fun `an empty file or one that is not there does not open`() {
        assertEquals(PdfOpenResult.Failed(PdfProblem.NOT_A_PDF), PdfBook.open(temporaryFolder.newFile("empty.pdf")))
        assertEquals(PdfOpenResult.Failed(PdfProblem.NOT_A_PDF), PdfBook.open(File(temporaryFolder.root, "gone.pdf")))
    }

    @Test
    fun `a page is drawn as wide as the view, whatever the height of the view`() {
        assertEquals(720 to 960, PdfBook.renderSize(600, 800, 720, 1))
        assertEquals(960 to 720, PdfBook.renderSize(800, 600, 960, 1))
        // The same page on a view turned sideways: wider, so taller, and scrolled.
        assertEquals(1520 to 2027, PdfBook.renderSize(600, 800, 1520, 1))
    }

    @Test
    fun `a higher level draws the page that many times sharper`() {
        assertEquals(1440 to 1920, PdfBook.renderSize(600, 800, 720, 2))
        assertEquals(2160 to 2880, PdfBook.renderSize(600, 800, 720, 3))
    }

    @Test
    fun `a drawing that would be too big is shrunk and keeps its shape`() {
        val (width, height) = PdfBook.renderSize(600, 800, 720, 3, maxPixels = 1_000_000L)

        assertTrue(width.toLong() * height <= 1_010_000L)
        assertTrue(abs(width.toDouble() / height - 0.75) < 0.01)
    }

    @Test
    fun `a size that makes no sense gives a single pixel instead of a crash`() {
        assertEquals(1 to 1, PdfBook.renderSize(0, 800, 720, 1))
        assertEquals(1 to 1, PdfBook.renderSize(600, 800, 0, 1))
    }
}
