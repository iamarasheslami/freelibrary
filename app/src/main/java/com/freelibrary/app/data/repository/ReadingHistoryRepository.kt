package com.freelibrary.app.data.repository

import androidx.room.withTransaction
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.dao.ReadingProgressDao
import com.freelibrary.app.data.local.entity.ShelfState
import javax.inject.Inject
import javax.inject.Singleton

/** The reader's reading history, which the reader can wipe from Settings. */
interface ReadingHistory {
    /**
     * Forgets what the reader has read: books leave the Currently Reading and Already Read shelves
     * and every saved reading position is deleted. The Want to Read list, bookmarks, downloaded
     * books and settings are not touched.
     */
    suspend fun clear()
}

/** Clears the history in one transaction, so it is either all gone or all still there. */
@Singleton
class ReadingHistoryRepository
    @Inject
    constructor(
        private val database: FreeLibraryDatabase,
        private val bookShelfStateDao: BookShelfStateDao,
        private val readingProgressDao: ReadingProgressDao,
    ) : ReadingHistory {
        override suspend fun clear() {
            database.withTransaction {
                bookShelfStateDao.deleteAllExcept(ShelfState.WANT_TO_READ)
                readingProgressDao.deleteAll()
            }
        }
    }
