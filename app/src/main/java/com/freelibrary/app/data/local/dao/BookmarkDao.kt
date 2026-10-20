package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt ASC")
    suspend fun getForBook(bookId: Long): List<Bookmark>

    /** Live list of a book's bookmarks, oldest first. */
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt ASC, id ASC")
    fun observeForBook(bookId: Long): Flow<List<Bookmark>>

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun delete(bookmarkId: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Long)

    /** Live total of the reader's bookmarks, for the Profile page. */
    @Query("SELECT COUNT(*) FROM bookmarks")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
