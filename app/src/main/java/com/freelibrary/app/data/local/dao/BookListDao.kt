package com.freelibrary.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.freelibrary.app.data.local.entity.ShelfState
import kotlinx.coroutines.flow.Flow

/**
 * Queries behind the book sliders and lists of the home screen.
 *
 * The shelf queries start from the shelf (CROSS JOIN fixes the order the tables are visited in)
 * and walk to its books. Left to itself, SQLite starts from the 78,000 books and checks every one
 * against the shelf, which cost about 55 ms per query however small the shelf was.
 */
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
        FROM bookshelves AS s
        CROSS JOIN book_bookshelves AS bb ON bb.bookshelfId = s.id
        CROSS JOIN books AS b ON b.id = bb.bookId
        WHERE s.name = :shelfName AND b.primaryLanguage = :language
        $BOOK_FILTER_SQL
        ORDER BY CAST(b.externalId AS INTEGER), b.id
        LIMIT :limit
        """,
    )
    fun observeBooksOnShelf(
        shelfName: String,
        language: String,
        limit: Int,
        formats: List<String>,
        anyFormat: Boolean,
        genres: List<String>,
        anyGenre: Boolean,
    ): Flow<List<BookCardRow>>

    /** How many books the shelf holds in [language]; tells a slider whether to offer "View more". */
    @Query(
        """
        SELECT COUNT(*) FROM bookshelves AS s
        CROSS JOIN book_bookshelves AS bb ON bb.bookshelfId = s.id
        CROSS JOIN books AS b ON b.id = bb.bookId
        WHERE s.name = :shelfName AND b.primaryLanguage = :language
        $BOOK_FILTER_SQL
        """,
    )
    fun observeShelfBookCount(
        shelfName: String,
        language: String,
        formats: List<String>,
        anyFormat: Boolean,
        genres: List<String>,
        anyGenre: Boolean,
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
        FROM bookshelves AS s
        CROSS JOIN book_bookshelves AS bb ON bb.bookshelfId = s.id
        CROSS JOIN books AS b ON b.id = bb.bookId
        WHERE s.name = :shelfName AND b.primaryLanguage = :language
        $BOOK_FILTER_SQL
        ORDER BY CAST(b.externalId AS INTEGER), b.id
        """,
    )
    fun shelfBooksPagingSource(
        shelfName: String,
        language: String,
        formats: List<String>,
        anyFormat: Boolean,
        genres: List<String>,
        anyGenre: Boolean,
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
        $BOOK_FILTER_SQL
        GROUP BY s.id
        HAVING COUNT(*) >= :minBooks
        """,
    )
    suspend fun shelfBookCounts(
        language: String,
        minBooks: Int,
        formats: List<String>,
        anyFormat: Boolean,
        genres: List<String>,
        anyGenre: Boolean,
    ): List<ShelfCountRow>

    /** Every language that at least one book has, with its book count, most books first. */
    @Query(
        """
        SELECT primaryLanguage AS value, COUNT(*) AS bookCount
        FROM books
        WHERE primaryLanguage IS NOT NULL AND primaryLanguage != ''
        GROUP BY primaryLanguage
        ORDER BY bookCount DESC, value
        """,
    )
    suspend fun languageOptions(): List<FilterOptionRow>

    /** Every file format that at least one book has, with its book count, most books first. */
    @Query(
        """
        SELECT formatType AS value, COUNT(*) AS bookCount
        FROM book_formats
        GROUP BY formatType
        ORDER BY bookCount DESC, value
        """,
    )
    suspend fun formatOptions(): List<FilterOptionRow>

    /** Every genre shelf ("Category: ...") that has books, with its book count, most books first. */
    @Query(
        """
        SELECT s.name AS value, COUNT(*) AS bookCount
        FROM bookshelves AS s
        INNER JOIN book_bookshelves AS bb ON bb.bookshelfId = s.id
        WHERE s.name LIKE 'Category: %'
        GROUP BY s.id
        ORDER BY bookCount DESC, value
        """,
    )
    suspend fun genreOptions(): List<FilterOptionRow>
}
