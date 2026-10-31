package com.freelibrary.app.data.download

import com.freelibrary.app.domain.model.BookFileFormat

/** A book file on the phone: which [bookId] and [format] it is, the book's [title] and the [sizeBytes] it takes. */
data class StoredDownload(
    val bookId: Long,
    val title: String,
    val format: BookFileFormat,
    val sizeBytes: Long,
)
