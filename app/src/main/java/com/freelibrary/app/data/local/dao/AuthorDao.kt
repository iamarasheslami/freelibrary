package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Author

@Dao
interface AuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(author: Author): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(authors: List<Author>)

    @Query("SELECT * FROM authors WHERE id = :id")
    suspend fun getById(id: Long): Author?

    @Query("SELECT * FROM authors WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Author?
}
