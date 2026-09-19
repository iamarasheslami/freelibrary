package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.freelibrary.app.data.local.entity.Book

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    /**
     * Updates an existing book in place, matched by its primary key (id).
     * Deliberately separate from insert: a sync-driven correction to an
     * existing book must never delete-and-reinsert, since reading_progress,
     * bookmarks, highlights, and downloaded_books all reference books.id
     * with ON DELETE CASCADE - a delete-and-reinsert would silently wipe a
     * user's reading data just because the catalog source corrected a typo.
     */
    @Update
    suspend fun update(book: Book)

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    @Query("SELECT * FROM books WHERE sourceId = :sourceId AND externalId = :externalId")
    suspend fun getBySourceAndExternalId(
        sourceId: Long,
        externalId: String,
    ): Book?

    @Query("SELECT externalId, lastModified FROM books WHERE sourceId = :sourceId")
    suspend fun getAllExternalIdsAndLastModified(sourceId: Long): List<ExternalIdAndLastModified>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}

data class ExternalIdAndLastModified(
    val externalId: String,
    val lastModified: String,
)
