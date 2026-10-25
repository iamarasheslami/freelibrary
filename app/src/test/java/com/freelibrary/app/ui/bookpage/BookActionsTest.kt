package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.download.BookFiles
import com.freelibrary.app.data.download.DownloadProgress
import com.freelibrary.app.data.download.Transfer
import com.freelibrary.app.domain.model.BookFileFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BookActionsTest {
    private val controls = ShelfControls(primary = ShelfAction.SAVE, secondary = ShelfAction.MARK_FINISHED)

    @Test
    fun `a book with nothing downloaded offers download, the shelf buttons and bookmarks`() {
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), emptyList(), null)

        assertEquals(
            listOf(
                BookAction.Download,
                BookAction.Shelf(ShelfAction.SAVE),
                BookAction.Shelf(ShelfAction.MARK_FINISHED),
                BookAction.Bookmarks,
            ),
            bookActionsFor(files, controls),
        )
    }

    @Test
    fun `a downloaded book offers read first and delete last`() {
        val files = BookFiles(listOf(BookFileFormat.EPUB), listOf(BookFileFormat.EPUB), null)

        assertEquals(
            listOf(
                BookAction.Read,
                BookAction.Shelf(ShelfAction.SAVE),
                BookAction.Shelf(ShelfAction.MARK_FINISHED),
                BookAction.Bookmarks,
                BookAction.DeleteDownload,
            ),
            bookActionsFor(files, controls),
        )
    }

    @Test
    fun `download stays while another format is still missing`() {
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), listOf(BookFileFormat.EPUB), null)

        assertEquals(listOf(BookAction.Read, BookAction.Download), bookActionsFor(files, null).take(2))
    }

    @Test
    fun `a running download offers cancel instead of download, and no delete`() {
        val transfer = Transfer.Running(BookFileFormat.PDF, DownloadProgress(10L, 100L))
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), listOf(BookFileFormat.EPUB), transfer)

        val actions = bookActionsFor(files, null)

        assertEquals(listOf(BookAction.Read, BookAction.CancelDownload, BookAction.Bookmarks), actions)
    }

    @Test
    fun `a book with no openable file still has bookmarks and its shelf buttons`() {
        val files = BookFiles(emptyList(), emptyList(), null)

        assertEquals(
            listOf(BookAction.Shelf(ShelfAction.SAVE), BookAction.Shelf(ShelfAction.MARK_FINISHED), BookAction.Bookmarks),
            bookActionsFor(files, controls),
        )
    }

    @Test
    fun `before anything is loaded only bookmarks is offered`() {
        assertEquals(listOf(BookAction.Bookmarks), bookActionsFor(null, null))
    }
}
