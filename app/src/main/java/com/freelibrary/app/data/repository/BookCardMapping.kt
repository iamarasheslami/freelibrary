package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookCardRow
import com.freelibrary.app.domain.model.BookCard

internal fun BookCardRow.toBookCard(): BookCard =
    BookCard(
        bookId = bookId,
        externalId = externalId,
        title = title,
        authorName = authorName,
        coverUrl = coverUrl,
    )
