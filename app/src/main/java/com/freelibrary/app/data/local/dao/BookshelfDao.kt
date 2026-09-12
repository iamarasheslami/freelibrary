package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Bookshelf

@Dao
interface BookshelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookshelf: Bookshelf): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookshelves: List<Bookshelf>)

    @Query("SELECT * FROM bookshelves WHERE id = :id")
    suspend fun getById(id: Long): Bookshelf?

    @Query("SELECT * FROM bookshelves WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Bookshelf?

    @Query("SELECT * FROM bookshelves ORDER BY name ASC")
    suspend fun getAll(): List<Bookshelf>
}
