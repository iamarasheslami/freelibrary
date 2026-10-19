package com.freelibrary.app.data.repository

import android.database.sqlite.SQLiteException
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.dao.ReadingProgressDao
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.domain.model.BookFileFormat
import com.freelibrary.app.domain.model.ReaderPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** What the reader needs from storage: the downloaded file of a book and where the reader got to in it. */
@Singleton
class ReaderRepository
    @Inject
    constructor(
        private val downloadedBookDao: DownloadedBookDao,
        private val readingProgressDao: ReadingProgressDao,
        private val timeProvider: TimeProvider,
    ) {
        /** The downloaded EPUB of [bookId], or null when none was downloaded or its file has gone. */
        suspend fun epubFile(bookId: Long): File? =
            withContext(Dispatchers.IO) {
                downloadedBookDao
                    .getFormatsForBook(bookId)
                    .firstOrNull { it.format.equals(BookFileFormat.EPUB.catalogName, ignoreCase = true) }
                    ?.let { File(it.localFilePath) }
                    ?.takeIf { it.isFile }
            }

        /** Where the reader left an EPUB, or null when nothing readable is saved for it. */
        suspend fun savedEpubPosition(bookId: Long): ReaderPosition.Epub? =
            readingProgressDao
                .getForBook(bookId)
                ?.lastPosition
                ?.let { ReaderPosition.decode(it) } as? ReaderPosition.Epub

        /**
         * Saves where the reader is in [bookId]. Returns false when it could not be saved, for
         * example because a catalog sync removed the book; reading goes on either way.
         */
        suspend fun savePosition(
            bookId: Long,
            position: ReaderPosition,
        ): Boolean =
            try {
                readingProgressDao.upsert(ReadingProgress(bookId, position.encode(), timeProvider.nowMillis()))
                true
            } catch (e: SQLiteException) {
                false
            }
    }
