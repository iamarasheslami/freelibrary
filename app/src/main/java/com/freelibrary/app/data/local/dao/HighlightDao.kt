package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Highlight

@Dao
interface HighlightDao {
    @Insert
    suspend fun insert(highlight: Highlight): Long

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt ASC")
    suspend fun getForBook(bookId: Long): List<Highlight>

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun delete(highlightId: Long)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Long)

    @Query("DELETE FROM highlights")
    suspend fun deleteAll()
}
