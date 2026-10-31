package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.DownloadedBook
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadedBook: DownloadedBook): Long

    /** The downloaded formats of one book, kept live so a page follows every download and delete. */
    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId")
    fun observeForBook(bookId: Long): Flow<List<DownloadedBook>>

    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId")
    suspend fun getFormatsForBook(bookId: Long): List<DownloadedBook>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_books WHERE bookId = :bookId)")
    suspend fun isDownloaded(bookId: Long): Boolean

    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    suspend fun getAllDownloads(): List<DownloadedBook>

    /** Every downloaded file with its book's title, newest first, kept live. */
    @Query(
        "SELECT d.bookId AS bookId, b.title AS title, d.format AS format, d.localFilePath AS localFilePath " +
            "FROM downloaded_books d JOIN books b ON b.id = d.bookId ORDER BY d.downloadedAt DESC, d.id DESC",
    )
    fun observeAllWithTitles(): Flow<List<DownloadedFileRow>>

    @Query("DELETE FROM downloaded_books WHERE bookId = :bookId AND format = :format")
    suspend fun delete(
        bookId: Long,
        format: String,
    )

    @Query("DELETE FROM downloaded_books WHERE bookId = :bookId")
    suspend fun deleteAllFormatsForBook(bookId: Long)
}
