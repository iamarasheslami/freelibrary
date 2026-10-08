package com.freelibrary.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.freelibrary.app.data.local.entity.ShelfState
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

    /**
     * The [limit] most recently added books in [language]: newest release date first and, for
     * books released on the same day, the highest Gutenberg number first (compared as numbers).
     * A book without a release date sorts last. Same card shape as [observeBooksOnShelf].
     */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title,
               (SELECT a.name FROM book_authors AS ba
                INNER JOIN authors AS a ON a.id = ba.authorId
                WHERE ba.bookId = b.id ORDER BY ba.id LIMIT 1) AS authorName,
               b.coverUrl AS coverUrl
        FROM books AS b
        WHERE b.primaryLanguage = :language
        ORDER BY b.issuedDate DESC, CAST(b.externalId AS INTEGER) DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyAdded(
        language: String,
        limit: Int,
    ): Flow<List<BookCardRow>>

    /**
     * The reader's own books in a shelf [state] (for example Currently Reading or Want to Read),
     * most recently updated first. Not filtered by language: these are books the reader chose.
     * Same card shape as [observeBooksOnShelf].
     */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title,
               (SELECT a.name FROM book_authors AS ba
                INNER JOIN authors AS a ON a.id = ba.authorId
                WHERE ba.bookId = b.id ORDER BY ba.id LIMIT 1) AS authorName,
               b.coverUrl AS coverUrl
        FROM book_shelf_state AS s
        INNER JOIN books AS b ON b.id = s.bookId
        WHERE s.state = :state
        ORDER BY s.updatedAt DESC, b.id DESC
        LIMIT :limit
        """,
    )
    fun observeBooksInState(
        state: ShelfState,
        limit: Int,
    ): Flow<List<BookCardRow>>

    /**
     * Paged version of [observeBooksOnShelf] for the shelf's "View more" page: the same books in
     * the same order, all of them. Room adds the LIMIT and OFFSET itself.
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
        """,
    )
    fun shelfBooksPagingSource(
        shelfName: String,
        language: String,
    ): PagingSource<Int, BookCardRow>

    /** Paged version of [observeRecentlyAdded]: every book of the language, newest first. */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title,
               (SELECT a.name FROM book_authors AS ba
                INNER JOIN authors AS a ON a.id = ba.authorId
                WHERE ba.bookId = b.id ORDER BY ba.id LIMIT 1) AS authorName,
               b.coverUrl AS coverUrl
        FROM books AS b
        WHERE b.primaryLanguage = :language
        ORDER BY b.issuedDate DESC, CAST(b.externalId AS INTEGER) DESC
        """,
    )
    fun recentlyAddedPagingSource(language: String): PagingSource<Int, BookCardRow>

    /** Paged version of [observeBooksInState]: all the reader's books in [state], newest update first. */
    @Query(
        """
        SELECT b.id AS bookId, b.externalId AS externalId, b.title AS title,
               (SELECT a.name FROM book_authors AS ba
                INNER JOIN authors AS a ON a.id = ba.authorId
                WHERE ba.bookId = b.id ORDER BY ba.id LIMIT 1) AS authorName,
               b.coverUrl AS coverUrl
        FROM book_shelf_state AS s
        INNER JOIN books AS b ON b.id = s.bookId
        WHERE s.state = :state
        ORDER BY s.updatedAt DESC, b.id DESC
        """,
    )
    fun booksInStatePagingSource(state: ShelfState): PagingSource<Int, BookCardRow>

    /**
     * Every shelf with at least [minBooks] books in [language], with that count: the shelf sizes
     * the Explore page is built from. A shelf with no books in [language] does not appear.
     */
    @Query(
        """
        SELECT s.name AS name, COUNT(*) AS bookCount
        FROM bookshelves AS s
        INNER JOIN book_bookshelves AS bb ON bb.bookshelfId = s.id
        INNER JOIN books AS b ON b.id = bb.bookId
        WHERE b.primaryLanguage = :language
        GROUP BY s.id
        HAVING COUNT(*) >= :minBooks
        """,
    )
    suspend fun shelfBookCounts(
        language: String,
        minBooks: Int,
    ): List<ShelfCountRow>
}
