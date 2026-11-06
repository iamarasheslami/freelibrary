package com.freelibrary.app.data.repository

import android.database.sqlite.SQLiteException
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.dao.ReadingDayDao
import com.freelibrary.app.data.local.dao.ReadingProgressDao
import com.freelibrary.app.data.local.entity.ReadingDay
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.domain.model.BookFileFormat
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.domain.model.localDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** How long a book must be open before moving to another place counts as reading it. */
private const val MOVE_COUNTS_AFTER_MILLIS = 5_000L

/** How long a book can stay open on one page before that page counts as read. */
private const val STAY_COUNTS_AFTER_MILLIS = 30_000L

/** Where a book was opened and when; it decides whether the reader has really read, not just opened the file. */
private class ReadingSession(
    val startPosition: String,
    val openedAt: Long,
)

/** What the reader needs from storage: the downloaded file of a book and where the reader got to in it. */
@Singleton
class ReaderRepository
    @Inject
    constructor(
        private val downloadedBookDao: DownloadedBookDao,
        private val readingProgressDao: ReadingProgressDao,
        private val readingDayDao: ReadingDayDao,
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

        private val sessions = ConcurrentHashMap<Long, ReadingSession>()

        /** The reader opened [bookId] at [position]. Opening alone is not reading: see [savePosition]. */
        fun readingStarted(
            bookId: Long,
            position: ReaderPosition,
        ) {
            sessions[bookId] = ReadingSession(position.encode(), timeProvider.nowMillis())
        }

        /**
         * Saves where the reader is in [bookId]. Today also becomes a day of reading, but only once the reader has read: they
         * moved to another place at least five seconds after opening, or they have stayed with the book for thirty seconds.
         * Opening a book and closing it again does not count. Returns false when it could not be saved, for example because a
         * catalog sync removed the book; reading goes on either way.
         */
        suspend fun savePosition(
            bookId: Long,
            position: ReaderPosition,
        ): Boolean =
            try {
                val now = timeProvider.nowMillis()
                readingProgressDao.upsert(ReadingProgress(bookId, position.encode(), now))
                if (hasRead(bookId, position, now)) readingDayDao.add(ReadingDay(localDay(now, TimeZone.getDefault())))
                true
            } catch (e: SQLiteException) {
                false
            }

        private fun hasRead(
            bookId: Long,
            position: ReaderPosition,
            now: Long,
        ): Boolean {
            val session = sessions[bookId] ?: return false
            val open = now - session.openedAt
            val moved = position.encode() != session.startPosition
            return (moved && open >= MOVE_COUNTS_AFTER_MILLIS) || open >= STAY_COUNTS_AFTER_MILLIS
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
