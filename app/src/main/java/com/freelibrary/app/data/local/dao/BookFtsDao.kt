package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFts

@Dao
interface BookFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookFts: BookFts)

    @Query("DELETE FROM books_fts WHERE rowid = :bookId")
    suspend fun deleteForBook(bookId: Long)

    @Query(
        """
        SELECT books.* FROM books
        INNER JOIN books_fts ON books.id = books_fts.rowid
        WHERE books_fts MATCH :query
        """,
    )
    suspend fun search(query: String): List<Book>

    /**
     * The books matching [matchQuery] (at most [limit] books, in index order),
     * one row per credited person in credit order. The limit sits in a
     * subquery so it counts books, not joined author rows.
     */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title, a.name AS authorName
        FROM (SELECT rowid AS id FROM books_fts WHERE books_fts MATCH :matchQuery LIMIT :limit) AS m
        INNER JOIN books AS b ON b.id = m.id
        LEFT JOIN book_authors AS ba ON ba.bookId = b.id
        LEFT JOIN authors AS a ON a.id = ba.authorId
        ORDER BY b.id, ba.id
        """,
    )
    suspend fun findCandidates(
        matchQuery: String,
        limit: Int,
    ): List<SearchRow>

    /** Every indexed (folded) title, for building the spelling-correction vocabulary. */
    @Query("SELECT title FROM books_fts")
    suspend fun allIndexedTitles(): List<String>

    /** Every indexed (folded) author-names string, for the same purpose. */
    @Query("SELECT authorNames FROM books_fts")
    suspend fun allIndexedAuthorNames(): List<String>
}
