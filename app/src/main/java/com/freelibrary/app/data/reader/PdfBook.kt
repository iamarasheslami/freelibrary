package com.freelibrary.app.data.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Why a file could not be opened as a PDF. */
enum class PdfProblem {
    /** Not a PDF at all, or a file that is not there. */
    NOT_A_PDF,

    /** A PDF locked with a password, which the reader cannot open. */
    PASSWORD_PROTECTED,

    /** A PDF that is damaged or that the phone cannot read. */
    DAMAGED,

    /** A PDF with no pages. */
    EMPTY,
}

sealed interface PdfOpenResult {
    data class Opened(
        val book: PdfBook,
    ) : PdfOpenResult

    data class Failed(
        val problem: PdfProblem,
    ) : PdfOpenResult
}

/**
 * A PDF file opened for reading with the phone's own PDF renderer; nothing is sent anywhere and no
 * other app or service is needed. Pages are drawn one at a time (the renderer allows no more) and
 * only on request, so a large PDF costs little memory. The book must be closed when the reader is done.
 */
class PdfBook private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {
    val pageCount: Int = renderer.pageCount

    private val lock = Any()
    private var closed = false

    /**
     * Draws page [index] (from 0) as wide as [viewWidth] pixels times the zoom [level] (1 to look at
     * the page, 2 or 3 to look at it zoomed in). Returns null if the page could not be drawn. When
     * there is not enough memory for the sharper picture the page is drawn at level 1 instead. Call it
     * off the main thread.
     */
    fun render(
        index: Int,
        viewWidth: Int,
        level: Int,
    ): Bitmap? =
        synchronized(lock) {
            if (closed || index !in 0 until pageCount) return null
            draw(index, viewWidth, level) ?: if (level > 1) draw(index, viewWidth, 1) else null
        }

    private fun draw(
        index: Int,
        viewWidth: Int,
        level: Int,
    ): Bitmap? =
        try {
            val page = renderer.openPage(index)
            try {
                val (width, height) = renderSize(page.width, page.height, viewWidth, level)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: IllegalStateException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            renderer.close()
            descriptor.close()
        }
    }

    companion object {
        /** The most pixels one drawn page may have: about 32 MB, which an old phone can still hold. */
        const val MAX_RENDER_PIXELS = 8_000_000L

        private const val SIGNATURE_SEARCH_BYTES = 1024

        /**
         * The size in pixels to draw a page of [pageWidth] by [pageHeight] at: as wide as the view,
         * times [level], and shrunk (keeping its shape) if that would pass [maxPixels]. A page taller
         * than the view is scrolled, so only the width counts.
         */
        fun renderSize(
            pageWidth: Int,
            pageHeight: Int,
            viewWidth: Int,
            level: Int,
            maxPixels: Long = MAX_RENDER_PIXELS,
        ): Pair<Int, Int> {
            if (pageWidth <= 0 || pageHeight <= 0 || viewWidth <= 0) return 1 to 1
            val fit = viewWidth.toDouble() / pageWidth * level.coerceAtLeast(1)
            var width = pageWidth * fit
            var height = pageHeight * fit
            val pixels = width * height
            if (pixels > maxPixels) {
                val shrink = sqrt(maxPixels / pixels)
                width *= shrink
                height *= shrink
            }
            return width.roundToInt().coerceAtLeast(1) to height.roundToInt().coerceAtLeast(1)
        }

        /** Opens [file]. Reads files from disk, so call it off the main thread. */
        fun open(file: File): PdfOpenResult {
            if (!looksLikePdf(file)) return PdfOpenResult.Failed(PdfProblem.NOT_A_PDF)
            var descriptor: ParcelFileDescriptor? = null
            return try {
                val opened = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor = opened
                val renderer = PdfRenderer(opened)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    opened.close()
                    PdfOpenResult.Failed(PdfProblem.EMPTY)
                } else {
                    PdfOpenResult.Opened(PdfBook(opened, renderer))
                }
            } catch (e: SecurityException) {
                closeQuietly(descriptor)
                PdfOpenResult.Failed(PdfProblem.PASSWORD_PROTECTED)
            } catch (e: IOException) {
                closeQuietly(descriptor)
                PdfOpenResult.Failed(PdfProblem.DAMAGED)
            } catch (e: IllegalArgumentException) {
                closeQuietly(descriptor)
                PdfOpenResult.Failed(PdfProblem.DAMAGED)
            }
        }

        /** A PDF starts with "%PDF-", somewhere in its first kilobyte. */
        private fun looksLikePdf(file: File): Boolean =
            try {
                file.inputStream().use { input ->
                    val buffer = ByteArray(SIGNATURE_SEARCH_BYTES)
                    val count = input.read(buffer)
                    count > 0 && String(buffer, 0, count, Charsets.ISO_8859_1).contains("%PDF-")
                }
            } catch (e: IOException) {
                false
            }

        private fun closeQuietly(descriptor: ParcelFileDescriptor?) {
            try {
                descriptor?.close()
            } catch (e: IOException) {
                // Nothing more can be done with it.
            }
        }
    }
}
