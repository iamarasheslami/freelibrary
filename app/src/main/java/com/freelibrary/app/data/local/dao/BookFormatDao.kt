package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.BookFormat

@Dao
interface BookFormatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookFormat: BookFormat): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookFormats: List<BookFormat>)

    @Query("SELECT * FROM book_formats WHERE bookId = :bookId")
    suspend fun getFormatsForBook(bookId: Long): List<BookFormat>

    @Query("SELECT EXISTS(SELECT 1 FROM book_formats WHERE bookId = :bookId AND formatType = :formatType)")
    suspend fun hasFormat(
        bookId: Long,
        formatType: String,
    ): Boolean
}
