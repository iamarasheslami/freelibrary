package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.WithdrawnBook

@Dao
interface WithdrawnBookDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun mark(withdrawn: WithdrawnBook)

    @Query("DELETE FROM withdrawn_books WHERE bookId = :bookId")
    suspend fun unmark(bookId: Long)

    /** True when the reader has anything of this book that a deletion would take with it. */
    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM reading_progress WHERE bookId = :bookId)
            OR EXISTS(SELECT 1 FROM bookmarks WHERE bookId = :bookId)
            OR EXISTS(SELECT 1 FROM highlights WHERE bookId = :bookId)
            OR EXISTS(SELECT 1 FROM downloaded_books WHERE bookId = :bookId)
            OR EXISTS(SELECT 1 FROM book_shelf_state WHERE bookId = :bookId)
        """,
    )
    suspend fun hasReaderData(bookId: Long): Boolean

    /** The external ids of this source's books that are currently marked withdrawn. */
    @Query(
        """
        SELECT b.externalId FROM withdrawn_books AS w
        INNER JOIN books AS b ON b.id = w.bookId
        WHERE b.sourceId = :sourceId
        """,
    )
    suspend fun markedExternalIds(sourceId: Long): List<String>

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: Long)
}
