package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.ReadingDay
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDayDao {
    /** Notes a day of reading; a day that is already there stays as it is. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(readingDay: ReadingDay)

    /** Every day the reader read, oldest first. There is at most one a day, so this stays small. */
    @Query("SELECT day FROM reading_days ORDER BY day")
    fun observeDays(): Flow<List<Long>>

    /** Backs the Settings action that clears the reading history. */
    @Query("DELETE FROM reading_days")
    suspend fun deleteAll()
}
