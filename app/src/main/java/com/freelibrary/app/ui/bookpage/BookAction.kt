package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.download.BookFiles

/** One icon button under a book's cover and title. */
sealed interface BookAction {
    /** Opens the book; with several downloaded formats the reader picks one from a list. */
    data object Read : BookAction

    /** Downloads the book; the reader picks the format from a list of everything offered. */
    data object Download : BookAction

    data object CancelDownload : BookAction

    data object DeleteDownload : BookAction

    data object Bookmarks : BookAction

    data class Shelf(
        val action: ShelfAction,
    ) : BookAction
}

/**
 * The buttons a book's page shows, in order: Read, then Download (or Cancel while a download is
 * waiting or running), then the shelf buttons, Bookmarks, and Delete download last. [files] is null
 * until the book's files are known, and [controls] until the book is loaded.
 */
fun bookActionsFor(
    files: BookFiles?,
    controls: ShelfControls?,
): List<BookAction> =
    buildList {
        if (files != null) {
            if (files.downloaded.isNotEmpty()) add(BookAction.Read)
            if (files.busy) {
                add(BookAction.CancelDownload)
            } else if (files.toDownload.isNotEmpty()) {
                add(BookAction.Download)
            }
        }
        controls?.primary?.let { add(BookAction.Shelf(it)) }
        controls?.secondary?.let { add(BookAction.Shelf(it)) }
        add(BookAction.Bookmarks)
        if (files != null && !files.busy && files.downloaded.isNotEmpty()) add(BookAction.DeleteDownload)
    }
