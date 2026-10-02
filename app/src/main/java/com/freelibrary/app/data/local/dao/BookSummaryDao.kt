package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.BookSummary

@Dao
interface BookSummaryDao {
    /**
     * Inserts or replaces a book's summary. Replace-on-conflict is safe here:
     * nothing references book_summaries, so a sync-driven correction to a
     * summary cannot cascade into any user data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookSummary: BookSummary)

    /** Returns the summary text, or null if the book has none. */
    @Query("SELECT summary FROM book_summaries WHERE bookId = :bookId")
    suspend fun getSummaryForBook(bookId: Long): String?

    /** Used when a sync finds the catalog no longer has a summary for a book. */
    @Query("DELETE FROM book_summaries WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
