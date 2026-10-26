package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.download.BookFiles
import com.freelibrary.app.data.download.DownloadProgress
import com.freelibrary.app.data.download.Transfer
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.BookFileFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BookActionsTest {
    private fun List<BookActionState>.of(action: BookAction) = first { it.action == action }

    @Test
    fun `the same buttons are always there, in the same order`() {
        val everyAction =
            listOf(
                BookAction.READ,
                BookAction.DOWNLOAD,
                BookAction.SAVE,
                BookAction.FINISHED,
                BookAction.RESTART,
                BookAction.BOOKMARKS,
                BookAction.DELETE_DOWNLOAD,
            )

        assertEquals(everyAction, bookActionsFor(null, null).map { it.action })
        val files = BookFiles(listOf(BookFileFormat.EPUB), listOf(BookFileFormat.EPUB), null)
        assertEquals(everyAction, bookActionsFor(files, ShelfState.READING).map { it.action })
    }

    @Test
    fun `a book with nothing downloaded can be downloaded but not read or deleted`() {
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), emptyList(), null)
        val actions = bookActionsFor(files, null)

        assertEquals(false, actions.of(BookAction.READ).enabled)
        assertEquals(true, actions.of(BookAction.DOWNLOAD).enabled)
        assertEquals(false, actions.of(BookAction.DELETE_DOWNLOAD).enabled)
    }

    @Test
    fun `a book with every format downloaded can be read and deleted but not downloaded again`() {
        val both = listOf(BookFileFormat.EPUB, BookFileFormat.PDF)
        val actions = bookActionsFor(BookFiles(both, both, null), null)

        assertEquals(true, actions.of(BookAction.READ).enabled)
        assertEquals(false, actions.of(BookAction.DOWNLOAD).enabled)
        assertEquals(true, actions.of(BookAction.DELETE_DOWNLOAD).enabled)
    }

    @Test
    fun `a book with one of two formats downloaded can still be downloaded`() {
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), listOf(BookFileFormat.EPUB), null)

        assertEquals(true, bookActionsFor(files, null).of(BookAction.DOWNLOAD).enabled)
    }

    @Test
    fun `while a download runs Download becomes Cancel and Delete is off`() {
        val running = Transfer.Running(BookFileFormat.EPUB, DownloadProgress(10L, 100L))
        val files = BookFiles(listOf(BookFileFormat.EPUB, BookFileFormat.PDF), listOf(BookFileFormat.PDF), running)
        val actions = bookActionsFor(files, null)

        assertEquals(BookAction.CANCEL_DOWNLOAD, actions[1].action)
        assertEquals(true, actions[1].enabled)
        assertEquals(false, actions.of(BookAction.DELETE_DOWNLOAD).enabled)
    }

    @Test
    fun `Save is highlighted for a wanted book and off for a finished one`() {
        assertEquals(false, bookActionsFor(null, null).of(BookAction.SAVE).selected)
        assertEquals(true, bookActionsFor(null, ShelfState.WANT_TO_READ).of(BookAction.SAVE).selected)
        assertEquals(false, bookActionsFor(null, ShelfState.READING).of(BookAction.SAVE).selected)
        assertEquals(false, bookActionsFor(null, ShelfState.FINISHED).of(BookAction.SAVE).enabled)
    }

    @Test
    fun `Finished is highlighted only for a finished book`() {
        assertEquals(true, bookActionsFor(null, ShelfState.FINISHED).of(BookAction.FINISHED).selected)
        assertEquals(false, bookActionsFor(null, ShelfState.READING).of(BookAction.FINISHED).selected)
    }

    @Test
    fun `Start over works only while the book is being read`() {
        for (state in listOf(null, ShelfState.WANT_TO_READ, ShelfState.FINISHED)) {
            assertEquals(false, bookActionsFor(null, state).of(BookAction.RESTART).enabled)
        }
        assertEquals(true, bookActionsFor(null, ShelfState.READING).of(BookAction.RESTART).enabled)
    }
}
