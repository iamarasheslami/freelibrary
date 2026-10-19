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
        suspend fun epubFile(bookId: Long): File? = downloadedFile(bookId, BookFileFormat.EPUB)

        /** The downloaded PDF of [bookId], or null when none was downloaded or its file has gone. */
        suspend fun pdfFile(bookId: Long): File? = downloadedFile(bookId, BookFileFormat.PDF)

        /** The downloaded plain text of [bookId], or null when none was downloaded or its file has gone. */
        suspend fun textFile(bookId: Long): File? = downloadedFile(bookId, BookFileFormat.TEXT)

        /** Where the reader left an EPUB, or null when nothing readable is saved for it. */
        suspend fun savedEpubPosition(bookId: Long): ReaderPosition.Epub? = savedPosition(bookId) as? ReaderPosition.Epub

        /** The page the reader left a PDF at, or null when nothing readable is saved for it. */
        suspend fun savedPdfPosition(bookId: Long): ReaderPosition.Pdf? = savedPosition(bookId) as? ReaderPosition.Pdf

        /** The place the reader left a plain text book at, or null when nothing readable is saved for it. */
        suspend fun savedTextPosition(bookId: Long): ReaderPosition.Text? = savedPosition(bookId) as? ReaderPosition.Text

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

        private suspend fun downloadedFile(
            bookId: Long,
            format: BookFileFormat,
        ): File? =
            withContext(Dispatchers.IO) {
                downloadedBookDao
                    .getFormatsForBook(bookId)
                    .firstOrNull { it.format.equals(format.catalogName, ignoreCase = true) }
                    ?.let { File(it.localFilePath) }
                    ?.takeIf { it.isFile }
            }

        private suspend fun savedPosition(bookId: Long): ReaderPosition? =
            readingProgressDao
                .getForBook(bookId)
                ?.lastPosition
                ?.let { ReaderPosition.decode(it) }
    }
