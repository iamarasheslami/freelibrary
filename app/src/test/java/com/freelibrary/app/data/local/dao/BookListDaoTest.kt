package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookListDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookListDao: BookListDao
    private var sourceId: Long = 0

    private suspend fun addBook(
        externalId: String,
        title: String,
        language: String? = "en",
        coverUrl: String? = null,
        issuedDate: String? = null,
        authors: List<String> = emptyList(),
    ): Long {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = title,
                    issuedDate = issuedDate,
                    primaryLanguage = language,
                    locc = null,
                    lastModified = "2026-10-04",
                    coverUrl = coverUrl,
                ),
            )
        authors.forEach { name ->
            val authorId = database.authorDao().insert(Author(name = name, birthYear = null, deathYear = null))
            database.bookAuthorDao().insert(BookAuthor(bookId = bookId, authorId = authorId, role = "author"))
        }
        return bookId
    }

    private suspend fun shelve(
        shelfName: String,
        vararg bookIds: Long,
    ) {
        val shelfId =
            database.bookshelfDao().findByName(shelfName)?.id
                ?: database.bookshelfDao().insert(Bookshelf(name = shelfName))
        bookIds.forEach { database.bookBookshelfDao().insert(BookBookshelf(bookId = it, bookshelfId = shelfId)) }
    }

    private suspend fun booksOn(
        shelfName: String,
        language: String = "en",
        limit: Int = 10,
    ) = bookListDao.observeBooksOnShelf(shelfName, language, limit).first()

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookListDao = database.bookListDao()
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `only the shelf's books in the requested language are returned`() =
        runTest {
            val english = addBook("1", "English One")
            val french = addBook("2", "Un livre", language = "fr")
            val unknown = addBook("3", "No language", language = null)
            val elsewhere = addBook("4", "On another shelf")
            shelve("Harvard Classics", english, french, unknown)
            shelve("Poetry", elsewhere)

            assertEquals(listOf("English One"), booksOn("Harvard Classics").map { it.title })
        }

    @Test
    fun `books come lowest Gutenberg number first, compared as numbers`() =
        runTest {
            val big = addBook("100", "C")
            val small = addBook("9", "A")
            val middle = addBook("20", "B")
            shelve("Adventure", big, small, middle)

            assertEquals(listOf("9", "20", "100"), booksOn("Adventure").map { it.externalId })
        }

    @Test
    fun `the limit keeps the lowest numbers`() =
        runTest {
            val ids = (1..5).map { addBook("$it", "Book $it") }
            shelve("Adventure", *ids.toLongArray())

            assertEquals(listOf("1", "2", "3"), booksOn("Adventure", limit = 3).map { it.externalId })
        }

    @Test
    fun `a book with several credits appears once with its first author`() =
        runTest {
            val gambler = addBook("1", "The Gambler", authors = listOf("Dostoyevsky, Fyodor", "Hogarth, C. J."))
            val anonymous = addBook("2", "Anonymous Tales")
            shelve("Poetry", gambler, anonymous)

            val rows = booksOn("Poetry")

            assertEquals(2, rows.size)
            assertEquals("Dostoyevsky, Fyodor", rows.first { it.title == "The Gambler" }.authorName)
            assertNull(rows.first { it.title == "Anonymous Tales" }.authorName)
        }

    @Test
    fun `the cover URL is passed through`() =
        runTest {
            val book = addBook("1", "With Cover", coverUrl = "https://example.org/1.cover.medium.jpg")
            val plain = addBook("2", "Without Cover")
            shelve("Poetry", book, plain)

            val rows = booksOn("Poetry")

            assertEquals("https://example.org/1.cover.medium.jpg", rows.first { it.externalId == "1" }.coverUrl)
            assertNull(rows.first { it.externalId == "2" }.coverUrl)
        }

    @Test
    fun `the shelf count follows the same shelf and language filter`() =
        runTest {
            val one = addBook("1", "One")
            val two = addBook("2", "Two")
            val french = addBook("3", "Trois", language = "fr")
            val other = addBook("4", "Four")
            shelve("Poetry", one, two, french)
            shelve("Adventure", other)

            assertEquals(2, bookListDao.observeShelfBookCount("Poetry", "en").first())
            assertEquals(1, bookListDao.observeShelfBookCount("Poetry", "fr").first())
        }

    @Test
    fun `an unknown shelf is empty`() =
        runTest {
            addBook("1", "One")

            assertTrue(booksOn("No Such Shelf").isEmpty())
            assertEquals(0, bookListDao.observeShelfBookCount("No Such Shelf", "en").first())
        }

    private suspend fun recentlyAdded(
        language: String = "en",
        limit: Int = 10,
    ) = bookListDao.observeRecentlyAdded(language, limit).first()

    @Test
    fun `recently added lists the newest release date first`() =
        runTest {
            addBook("1", "Old", issuedDate = "2001-05-01")
            addBook("2", "Newest", issuedDate = "2026-09-30")
            addBook("3", "Middle", issuedDate = "2015-01-15")

            assertEquals(listOf("Newest", "Middle", "Old"), recentlyAdded().map { it.title })
        }

    @Test
    fun `books added on the same day come highest Gutenberg number first, compared as numbers`() =
        runTest {
            addBook("9", "Nine", issuedDate = "2026-09-30")
            addBook("100", "Hundred", issuedDate = "2026-09-30")
            addBook("20", "Twenty", issuedDate = "2026-09-30")

            assertEquals(listOf("100", "20", "9"), recentlyAdded().map { it.externalId })
        }

    @Test
    fun `recently added keeps to the requested language`() =
        runTest {
            addBook("1", "English", issuedDate = "2026-09-30")
            addBook("2", "Francais", language = "fr", issuedDate = "2026-10-01")
            addBook("3", "Unknown", language = null, issuedDate = "2026-10-01")

            assertEquals(listOf("English"), recentlyAdded().map { it.title })
        }

    @Test
    fun `the recently added limit keeps the newest books`() =
        runTest {
            (1..5).forEach { addBook("$it", "Book $it", issuedDate = "2026-09-0$it") }

            assertEquals(listOf("Book 5", "Book 4"), recentlyAdded(limit = 2).map { it.title })
        }

    @Test
    fun `books without a release date come last`() =
        runTest {
            addBook("1", "Undated")
            addBook("2", "Dated", issuedDate = "1999-01-01")

            assertEquals(listOf("Dated", "Undated"), recentlyAdded().map { it.title })
        }

    @Test
    fun `recently added books carry their first author and cover`() =
        runTest {
            addBook(
                "1",
                "The Gambler",
                coverUrl = "https://example.org/1.cover.medium.jpg",
                issuedDate = "2026-09-30",
                authors = listOf("Dostoyevsky, Fyodor", "Hogarth, C. J."),
            )

            val row = recentlyAdded().single()

            assertEquals("Dostoyevsky, Fyodor", row.authorName)
            assertEquals("https://example.org/1.cover.medium.jpg", row.coverUrl)
        }

    private suspend fun setState(
        bookId: Long,
        state: ShelfState,
        updatedAt: Long,
    ) {
        database.bookShelfStateDao().upsert(BookShelfState(bookId = bookId, state = state, updatedAt = updatedAt))
    }

    private suspend fun inState(
        state: ShelfState,
        limit: Int = 10,
    ) = bookListDao.observeBooksInState(state, limit).first()

    @Test
    fun `only books in the requested shelf state are returned`() =
        runTest {
            setState(addBook("1", "Reading"), ShelfState.READING, 100)
            setState(addBook("2", "Wanted"), ShelfState.WANT_TO_READ, 100)
            setState(addBook("3", "Finished"), ShelfState.FINISHED, 100)
            addBook("4", "On no shelf")

            assertEquals(listOf("Reading"), inState(ShelfState.READING).map { it.title })
            assertEquals(listOf("Wanted"), inState(ShelfState.WANT_TO_READ).map { it.title })
        }

    @Test
    fun `books in a state come most recently updated first`() =
        runTest {
            setState(addBook("1", "Oldest"), ShelfState.READING, 100)
            setState(addBook("2", "Newest"), ShelfState.READING, 300)
            setState(addBook("3", "Middle"), ShelfState.READING, 200)

            assertEquals(listOf("Newest", "Middle", "Oldest"), inState(ShelfState.READING).map { it.title })
        }

    @Test
    fun `the state limit keeps the most recently updated books`() =
        runTest {
            (1..5).forEach { setState(addBook("$it", "Book $it"), ShelfState.WANT_TO_READ, it * 100L) }

            assertEquals(listOf("Book 5", "Book 4"), inState(ShelfState.WANT_TO_READ, limit = 2).map { it.title })
        }

    @Test
    fun `the reader's own books are not filtered by language`() =
        runTest {
            setState(addBook("1", "Un livre", language = "fr"), ShelfState.READING, 100)
            setState(addBook("2", "No language", language = null), ShelfState.READING, 200)

            assertEquals(listOf("No language", "Un livre"), inState(ShelfState.READING).map { it.title })
        }

    @Test
    fun `books in a state carry their first author and cover`() =
        runTest {
            val book =
                addBook(
                    "1",
                    "The Gambler",
                    coverUrl = "https://example.org/1.cover.medium.jpg",
                    authors = listOf("Dostoyevsky, Fyodor", "Hogarth, C. J."),
                )
            setState(book, ShelfState.READING, 100)

            val row = inState(ShelfState.READING).single()

            assertEquals("Dostoyevsky, Fyodor", row.authorName)
            assertEquals("https://example.org/1.cover.medium.jpg", row.coverUrl)
        }
}
