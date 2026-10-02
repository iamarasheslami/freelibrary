package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import kotlinx.coroutines.flow.Flow

@Dao
interface BookShelfStateDao {
    /**
     * Sets a book's shelf state, replacing any previous state: a book is on
     * exactly one shelf at a time. Safe to replace because nothing references
     * book_shelf_state, so no user data can cascade away.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookShelfState: BookShelfState)

    @Query("SELECT * FROM book_shelf_state WHERE bookId = :bookId")
    suspend fun getForBook(bookId: Long): BookShelfState?

    /** Live version for screens that must react when the reader changes a book's shelf. */
    @Query("SELECT * FROM book_shelf_state WHERE bookId = :bookId")
    fun observeForBook(bookId: Long): Flow<BookShelfState?>

    /** Backs the home-screen sliders: books in one state, most recently updated first. */
    @Query("SELECT * FROM book_shelf_state WHERE state = :state ORDER BY updatedAt DESC LIMIT :limit")
    fun observeByState(
        state: ShelfState,
        limit: Int,
    ): Flow<List<BookShelfState>>

    /** Lets a slider decide whether to show "View more", and feeds future stats. */
    @Query("SELECT COUNT(*) FROM book_shelf_state WHERE state = :state")
    fun observeCount(state: ShelfState): Flow<Int>

    @Query("DELETE FROM book_shelf_state WHERE bookId = :bookId")
    suspend fun delete(bookId: Long)

    /** Backs a "clear my reading history" action. */
    @Query("DELETE FROM book_shelf_state")
    suspend fun deleteAll()
}
