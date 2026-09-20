package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Source

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: Source): Long

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: Long): Source?

    @Query("SELECT * FROM sources")
    suspend fun getAll(): List<Source>

    @Query("SELECT * FROM sources WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Source?
}
