package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.Bookshelf
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
        authors: List<String> = emptyList(),
    ): Long {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = title,
                    issuedDate = null,
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
}
