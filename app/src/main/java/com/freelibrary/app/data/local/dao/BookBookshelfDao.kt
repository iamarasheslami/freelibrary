package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.Bookshelf

@Dao
interface BookBookshelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookBookshelf: BookBookshelf): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookBookshelves: List<BookBookshelf>)

    @Query(
        """
        SELECT bookshelves.* FROM bookshelves
        INNER JOIN book_bookshelves ON bookshelves.id = book_bookshelves.bookshelfId
        WHERE book_bookshelves.bookId = :bookId
        """,
    )
    suspend fun getBookshelvesForBook(bookId: Long): List<Bookshelf>

    @Query(
        """
        SELECT books.* FROM books
        INNER JOIN book_bookshelves ON books.id = book_bookshelves.bookId
        WHERE book_bookshelves.bookshelfId = :bookshelfId
        """,
    )
    suspend fun getBooksForBookshelf(bookshelfId: Long): List<Book>

    /**
     * Clears every bookshelf association for a book, so the sync client can
     * rebuild them fresh when a book's curated shelves change. Safe to do
     * unconditionally since this table holds catalog associations, not user
     * data - unlike deleting the book row itself.
     */
    @Query("DELETE FROM book_bookshelves WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
