package com.freelibrary.app.data.repository

import android.database.sqlite.SQLiteException
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.BookmarkDao
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.domain.model.ReaderPosition
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A bookmark of the reader's, with its place decoded. */
data class SavedBookmark(
    val id: Long,
    val bookId: Long,
    val note: String?,
    val position: ReaderPosition,
    val createdAt: Long,
)

/**
 * The reader's bookmarks. A bookmark is a place in one book ([ReaderPosition]) with an optional
 * note. They are listed in the order of the book, not of the time they were made. A bookmark whose
 * place this version cannot read is left out of the list, but stays stored.
 */
@Singleton
class BookmarkRepository
    @Inject
    constructor(
        private val bookmarkDao: BookmarkDao,
        private val timeProvider: TimeProvider,
    ) {
        /**
         * Saves a bookmark at [position] in [bookId]. A blank [note] is saved as no note and a long
         * one is cut to [MAX_NOTE_LENGTH]. Returns false when it could not be saved, for example
         * because a catalog sync removed the book.
         */
        suspend fun add(
            bookId: Long,
            position: ReaderPosition,
            note: String?,
        ): Boolean =
            withContext(NonCancellable) {
                try {
                    bookmarkDao.insert(
                        Bookmark(
                            bookId = bookId,
                            position = position.encode(),
                            note = cleanNote(note),
                            createdAt = timeProvider.nowMillis(),
                        ),
                    )
                    true
                } catch (e: SQLiteException) {
                    false
                }
            }

        /** The bookmarks of [bookId] in the order of the book, kept up to date. */
        fun observe(bookId: Long): Flow<List<SavedBookmark>> =
            bookmarkDao.observeForBook(bookId).map { rows ->
                rows
                    .mapNotNull { row ->
                        ReaderPosition.decode(row.position)?.let { SavedBookmark(row.id, row.bookId, row.note, it, row.createdAt) }
                    }.sortedWith(
                        compareBy<SavedBookmark>(
                            { it.position.kind() },
                            { it.position.major() },
                            { it.position.minor() },
                            { it.createdAt },
                        ),
                    )
            }

        /** Deletes the bookmark [id]. Returns false when that failed. */
        suspend fun delete(id: Long): Boolean =
            withContext(NonCancellable) {
                try {
                    bookmarkDao.delete(id)
                    true
                } catch (e: SQLiteException) {
                    false
                }
            }

        /** Puts back a bookmark that was deleted, as it was. Returns false when that failed. */
        suspend fun restore(bookmark: SavedBookmark): Boolean =
            withContext(NonCancellable) {
                try {
                    bookmarkDao.insert(
                        Bookmark(
                            id = bookmark.id,
                            bookId = bookmark.bookId,
                            position = bookmark.position.encode(),
                            note = bookmark.note,
                            createdAt = bookmark.createdAt,
                        ),
                    )
                    true
                } catch (e: SQLiteException) {
                    false
                }
            }

        private fun cleanNote(note: String?): String? =
            note
                ?.trim()
                ?.take(MAX_NOTE_LENGTH)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        private fun ReaderPosition.kind(): Int =
            when (this) {
                is ReaderPosition.Epub -> 0
                is ReaderPosition.Pdf -> 1
                is ReaderPosition.Text -> 2
            }

        private fun ReaderPosition.major(): Int =
            when (this) {
                is ReaderPosition.Epub -> chapter
                is ReaderPosition.Pdf -> page
                is ReaderPosition.Text -> offset
            }

        private fun ReaderPosition.minor(): Int =
            when (this) {
                is ReaderPosition.Epub -> permille
                is ReaderPosition.Pdf -> 0
                is ReaderPosition.Text -> 0
            }

        companion object {
            /** The longest note kept; the note box in the add dialog has the same limit. */
            const val MAX_NOTE_LENGTH = 200
        }
    }
