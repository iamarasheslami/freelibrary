package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookAuthorDao
import com.freelibrary.app.data.local.dao.BookDao
import com.freelibrary.app.data.local.dao.BookSummaryDao
import com.freelibrary.app.domain.model.BookAuthorCredit
import com.freelibrary.app.domain.model.BookDetails
import javax.inject.Inject
import javax.inject.Singleton

/** What a book's own page shows, read from the catalog. */
@Singleton
class BookDetailsRepository
    @Inject
    constructor(
        private val bookDao: BookDao,
        private val bookAuthorDao: BookAuthorDao,
        private val bookSummaryDao: BookSummaryDao,
    ) {
        /**
         * One book's page content, or null when no such book exists (for example one removed by a
         * catalog sync). A person credited in two roles appears once, and a blank summary counts
         * as no summary.
         */
        suspend fun load(bookId: Long): BookDetails? {
            val book = bookDao.getById(bookId) ?: return null
            val authors =
                bookAuthorDao
                    .getAuthorsForBook(bookId)
                    .distinctBy { it.id }
                    .map { BookAuthorCredit(it.name, it.birthYear, it.deathYear) }
            return BookDetails(
                bookId = book.id,
                externalId = book.externalId,
                title = book.title,
                coverUrl = book.coverUrl,
                language = book.primaryLanguage,
                authors = authors,
                summary = bookSummaryDao.getSummaryForBook(bookId)?.takeIf { it.isNotBlank() },
            )
        }
    }
