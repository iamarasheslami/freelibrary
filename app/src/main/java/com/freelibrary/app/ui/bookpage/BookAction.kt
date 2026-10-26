package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.download.BookFiles
import com.freelibrary.app.data.local.entity.ShelfState

/** One icon button under a book's cover and title. */
enum class BookAction {
    /** Opens the book; with several downloaded formats the reader picks one from a list. */
    READ,

    /** Downloads the book; the reader picks the format from a list of everything offered. */
    DOWNLOAD,

    /** Takes the place of [DOWNLOAD] while a download is waiting or running. */
    CANCEL_DOWNLOAD,

    /** A switch: puts the book on the Want to Read shelf, or takes it off. */
    SAVE,

    /** A switch: marks the book finished, or takes that back. */
    FINISHED,

    /** Back to the beginning: forgets the reading position and leaves Currently Reading. */
    RESTART,

    BOOKMARKS,

    DELETE_DOWNLOAD,
}

/**
 * A button as the page draws it: [enabled] false means it is shown dimmed and cannot be tapped, and
 * [selected] true means it is highlighted because its state is on.
 */
data class BookActionState(
    val action: BookAction,
    val enabled: Boolean,
    val selected: Boolean = false,
)

/**
 * The buttons a book's page always shows, in this order: Read, Download (Cancel while a download is
 * waiting or running), Save, Finished, Start over, Bookmarks and Delete download. They never come and
 * go; they are only enabled or not, highlighted or not. [files] is null until the book's files are
 * known, [shelfState] is null for a book on no shelf.
 */
fun bookActionsFor(
    files: BookFiles?,
    shelfState: ShelfState?,
): List<BookActionState> {
    val downloaded = files?.downloaded.orEmpty()
    val busy = files?.busy == true
    return listOf(
        BookActionState(BookAction.READ, enabled = downloaded.isNotEmpty()),
        if (busy) {
            BookActionState(BookAction.CANCEL_DOWNLOAD, enabled = true)
        } else {
            BookActionState(BookAction.DOWNLOAD, enabled = files?.toDownload?.isNotEmpty() == true)
        },
        BookActionState(
            BookAction.SAVE,
            enabled = shelfState != ShelfState.FINISHED,
            selected = shelfState == ShelfState.WANT_TO_READ,
        ),
        BookActionState(BookAction.FINISHED, enabled = true, selected = shelfState == ShelfState.FINISHED),
        BookActionState(BookAction.RESTART, enabled = shelfState == ShelfState.READING),
        BookActionState(BookAction.BOOKMARKS, enabled = true),
        BookActionState(BookAction.DELETE_DOWNLOAD, enabled = !busy && downloaded.isNotEmpty()),
    )
}
