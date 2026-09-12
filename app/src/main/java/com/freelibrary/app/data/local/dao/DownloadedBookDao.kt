package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.DownloadedBook

@Dao
interface DownloadedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadedBook: DownloadedBook): Long

    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId")
    suspend fun getFormatsForBook(bookId: Long): List<DownloadedBook>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_books WHERE bookId = :bookId)")
    suspend fun isDownloaded(bookId: Long): Boolean

    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    suspend fun getAllDownloads(): List<DownloadedBook>

    @Query("DELETE FROM downloaded_books WHERE bookId = :bookId AND format = :format")
    suspend fun delete(
        bookId: Long,
        format: String,
    )

    @Query("DELETE FROM downloaded_books WHERE bookId = :bookId")
    suspend fun deleteAllFormatsForBook(bookId: Long)
}
