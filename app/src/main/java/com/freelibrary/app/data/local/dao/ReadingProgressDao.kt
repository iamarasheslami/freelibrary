package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.ReadingProgress

@Dao
interface ReadingProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(readingProgress: ReadingProgress)

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getForBook(bookId: Long): ReadingProgress?

    @Query("SELECT * FROM reading_progress ORDER BY lastOpenedAt DESC LIMIT :limit")
    suspend fun getMostRecent(limit: Int): List<ReadingProgress>

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun delete(bookId: Long)

    @Query("DELETE FROM reading_progress")
    suspend fun deleteAll()
}
