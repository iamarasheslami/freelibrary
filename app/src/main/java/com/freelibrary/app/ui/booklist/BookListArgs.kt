package com.freelibrary.app.ui.booklist

import com.freelibrary.app.domain.model.ViewMoreTarget

internal const val KIND_CONTINUE_READING = "continue-reading"
internal const val KIND_WANT_TO_READ = "want-to-read"
internal const val KIND_FINISHED = "finished"
internal const val KIND_RECENTLY_ADDED = "recently-added"
internal const val KIND_SHELF = "shelf"

/** A "View more" target in the plain-text form that navigation arguments can carry. */
data class BookListArgs(
    val kind: String,
    val bookshelfName: String?,
)

fun ViewMoreTarget.toArgs(): BookListArgs =
    when (this) {
        ViewMoreTarget.ContinueReading -> BookListArgs(KIND_CONTINUE_READING, null)
        ViewMoreTarget.WantToRead -> BookListArgs(KIND_WANT_TO_READ, null)
        ViewMoreTarget.Finished -> BookListArgs(KIND_FINISHED, null)
        ViewMoreTarget.RecentlyAdded -> BookListArgs(KIND_RECENTLY_ADDED, null)
        is ViewMoreTarget.CatalogShelf -> BookListArgs(KIND_SHELF, bookshelfName)
    }

/** The target for these arguments, or null when they name nothing known (a shelf needs its name). */
fun viewMoreTargetFrom(
    kind: String?,
    bookshelfName: String?,
): ViewMoreTarget? =
    when (kind) {
        KIND_CONTINUE_READING -> ViewMoreTarget.ContinueReading
        KIND_WANT_TO_READ -> ViewMoreTarget.WantToRead
        KIND_FINISHED -> ViewMoreTarget.Finished
        KIND_RECENTLY_ADDED -> ViewMoreTarget.RecentlyAdded
        KIND_SHELF -> bookshelfName?.takeIf { it.isNotBlank() }?.let { ViewMoreTarget.CatalogShelf(it) }
        else -> null
    }
