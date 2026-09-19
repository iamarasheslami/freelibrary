package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.BookAuthor

@Dao
interface BookAuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookAuthor: BookAuthor): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookAuthors: List<BookAuthor>)

    @Query(
        """
        SELECT authors.* FROM authors
        INNER JOIN book_authors ON authors.id = book_authors.authorId
        WHERE book_authors.bookId = :bookId
        """,
    )
    suspend fun getAuthorsForBook(bookId: Long): List<Author>

    /**
     * Clears every author association for a book, so the sync client can
     * rebuild them fresh when a book's credited people change. Safe to do
     * unconditionally since this table holds catalog associations, not user
     * data - unlike deleting the book row itself.
     */
    @Query("DELETE FROM book_authors WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
