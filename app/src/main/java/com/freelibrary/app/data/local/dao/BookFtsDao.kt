package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFts

@Dao
interface BookFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookFts: BookFts)

    @Query("DELETE FROM books_fts WHERE rowid = :bookId")
    suspend fun deleteForBook(bookId: Long)

    @Query(
        """
        SELECT books.* FROM books
        INNER JOIN books_fts ON books.id = books_fts.rowid
        WHERE books_fts MATCH :query
        """,
    )
    suspend fun search(query: String): List<Book>
}
