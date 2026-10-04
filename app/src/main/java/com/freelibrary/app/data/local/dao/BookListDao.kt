package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries behind the book sliders and lists of the home screen. */
@Dao
interface BookListDao {
    /**
     * The first [limit] books of the curated shelf named [shelfName] in [language], lowest
     * Gutenberg number first (the numeric sort needs the CAST: as text, "100" would come before
     * "9"). One row per book: the author is a subquery for the first credited person, so a book
     * with several credits is not repeated. Re-emits when the catalog changes, for example after
     * a sync.
     */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title,
               (SELECT a.name FROM book_authors AS ba
                INNER JOIN authors AS a ON a.id = ba.authorId
                WHERE ba.bookId = b.id ORDER BY ba.id LIMIT 1) AS authorName,
               b.coverUrl AS coverUrl
        FROM books AS b
        INNER JOIN book_bookshelves AS bb ON bb.bookId = b.id
        INNER JOIN bookshelves AS s ON s.id = bb.bookshelfId
        WHERE s.name = :shelfName AND b.primaryLanguage = :language
        ORDER BY CAST(b.externalId AS INTEGER), b.id
        LIMIT :limit
        """,
    )
    fun observeBooksOnShelf(
        shelfName: String,
        language: String,
        limit: Int,
    ): Flow<List<BookCardRow>>

    /** How many books the shelf holds in [language]; tells a slider whether to offer "View more". */
    @Query(
        """
        SELECT COUNT(*) FROM books AS b
        INNER JOIN book_bookshelves AS bb ON bb.bookId = b.id
        INNER JOIN bookshelves AS s ON s.id = bb.bookshelfId
        WHERE s.name = :shelfName AND b.primaryLanguage = :language
        """,
    )
    fun observeShelfBookCount(
        shelfName: String,
        language: String,
    ): Flow<Int>
}
