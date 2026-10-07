package com.freelibrary.app.data.repository

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookListRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: BookListRepository
    private var sourceId: Long = 0
    private val shelfName = "Adventure"

    private suspend fun addBook(
        externalId: String,
        title: String = "Book $externalId",
        language: String = "en",
        issuedDate: String? = null,
    ): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = title,
                issuedDate = issuedDate,
                primaryLanguage = language,
                locc = null,
                lastModified = "2026-10-04",
            ),
        )

    private suspend fun shelve(vararg bookIds: Long) {
        val shelfId =
            database.bookshelfDao().findByName(shelfName)?.id
                ?: database.bookshelfDao().insert(Bookshelf(name = shelfName))
        bookIds.forEach { database.bookBookshelfDao().insert(BookBookshelf(bookId = it, bookshelfId = shelfId)) }
    }

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = BookListRepository(database.bookListDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a shelf slider holds the first ten English books and offers View more when there are more`() =
        runTest {
            shelve(*(1..12).map { addBook("$it") }.toLongArray(), addBook("99", language = "fr"))

            val slider = repository.observeShelfSlider(shelfName).first()

            assertEquals((1..10).map { "$it" }, slider.books.map { it.externalId })
            assertEquals(12, slider.totalBooks)
            assertTrue(slider.hasMore)
        }

    @Test
    fun `a shelf with ten books or fewer does not offer View more`() =
        runTest {
            shelve(*(1..10).map { addBook("$it") }.toLongArray())

            val slider = repository.observeShelfSlider(shelfName).first()

            assertEquals(10, slider.books.size)
            assertFalse(slider.hasMore)
        }

    @Test
    fun `the recently added slider holds the ten newest English books`() =
        runTest {
            (1..12).forEach { addBook("$it", issuedDate = "2026-09-%02d".format(it)) }
            addBook("99", language = "fr", issuedDate = "2026-10-01")

            val books = repository.observeRecentlyAdded().first()

            assertEquals((12 downTo 3).map { "Book $it" }, books.map { it.title })
        }

    @Test
    fun `the shelf pages hold every English book of the shelf in order`() =
        runTest {
            shelve(*(1..35).map { addBook("$it") }.toLongArray(), addBook("99", language = "fr"))

            val books = repository.shelfPages(shelfName).asSnapshot()

            assertEquals((1..35).map { "$it" }, books.map { it.externalId })
        }

    @Test
    fun `the recently added pages list the newest books first`() =
        runTest {
            (1..5).forEach { addBook("$it", issuedDate = "2026-09-0$it") }

            val books = repository.recentlyAddedPages().asSnapshot()

            assertEquals((5 downTo 1).map { "Book $it" }, books.map { it.title })
        }

    @Test
    fun `the reader shelf pages list the reader's books`() =
        runTest {
            val one = addBook("1")
            val two = addBook("2")
            addBook("3")
            database.bookShelfStateDao().upsert(BookShelfState(bookId = one, state = ShelfState.WANT_TO_READ, updatedAt = 100))
            database.bookShelfStateDao().upsert(BookShelfState(bookId = two, state = ShelfState.WANT_TO_READ, updatedAt = 200))

            val books = repository.readerShelfPages(ShelfState.WANT_TO_READ).asSnapshot()

            assertEquals(listOf("Book 2", "Book 1"), books.map { it.title })
        }
}
