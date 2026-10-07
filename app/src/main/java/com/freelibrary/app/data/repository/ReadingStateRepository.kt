package com.freelibrary.app.data.repository

import androidx.room.withTransaction
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.BookCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the reader's shelf rules live. A book is on at most one shelf at a time:
 * - opening a book moves it to Currently Reading (and to the front of that list), unless it is
 *   already Already Read;
 * - Save moves a book that is on no shelf, or only being looked at (Currently Reading), to Want
 *   to Read, and does nothing for a book that is already wanted or already read;
 * - the reader can set any state manually, and remove a book from its shelf.
 * Removing a book never touches its saved reading position, and downloading or deleting a
 * downloaded file never changes the shelf. Each rule runs as one transaction.
 */
@Singleton
class ReadingStateRepository
    @Inject
    constructor(
        private val database: FreeLibraryDatabase,
        private val bookShelfStateDao: BookShelfStateDao,
        private val bookListDao: BookListDao,
        private val timeProvider: TimeProvider,
    ) {
        /** The reader's books in [state] for a slider, most recently updated first. */
        fun observeShelf(
            state: ShelfState,
            limit: Int,
        ): Flow<List<BookCard>> = bookListDao.observeBooksInState(state, limit).map { rows -> rows.map { it.toBookCard() } }

        /** How many books are in [state]; tells a slider whether to offer "View more". */
        fun observeCount(state: ShelfState): Flow<Int> = bookShelfStateDao.observeCount(state)

        /** The shelf a book is on, or null; for a book page's own buttons. */
        fun observeStateOf(bookId: Long): Flow<ShelfState?> = bookShelfStateDao.observeForBook(bookId).map { it?.state }

        /** Call when the reader opens a book. */
        suspend fun onBookOpened(bookId: Long) {
            database.withTransaction {
                when (bookShelfStateDao.getForBook(bookId)?.state) {
                    ShelfState.FINISHED -> Unit
                    null, ShelfState.WANT_TO_READ, ShelfState.READING -> write(bookId, ShelfState.READING)
                }
            }
        }

        /** The Save button. */
        suspend fun saveForLater(bookId: Long) {
            database.withTransaction {
                when (bookShelfStateDao.getForBook(bookId)?.state) {
                    null, ShelfState.READING -> write(bookId, ShelfState.WANT_TO_READ)
                    ShelfState.WANT_TO_READ, ShelfState.FINISHED -> Unit
                }
            }
        }

        /** The reader finished the book; keeps the first finish time if it already was finished. */
        suspend fun markFinished(bookId: Long) = setState(bookId, ShelfState.FINISHED)

        /** The reader's manual change to any state; setting the state a book already has does nothing. */
        suspend fun setState(
            bookId: Long,
            state: ShelfState,
        ) {
            database.withTransaction {
                if (bookShelfStateDao.getForBook(bookId)?.state != state) write(bookId, state)
            }
        }

        /** Removes a book from its shelf. Its saved reading position is kept. */
        suspend fun removeFromShelf(bookId: Long) {
            bookShelfStateDao.delete(bookId)
        }

        private suspend fun write(
            bookId: Long,
            state: ShelfState,
        ) {
            val now = timeProvider.nowMillis()
            bookShelfStateDao.upsert(
                BookShelfState(
                    bookId = bookId,
                    state = state,
                    updatedAt = now,
                    finishedAt = if (state == ShelfState.FINISHED) now else null,
                ),
            )
        }
    }
