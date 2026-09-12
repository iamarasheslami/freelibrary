package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookBookshelfDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookBookshelfDao: BookBookshelfDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookBookshelfDao = database.bookBookshelfDao()

            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1342",
                        title = "Pride and Prejudice",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getBookshelvesForBook returns every linked bookshelf via the join`() =
        runTest {
            val shelfId = database.bookshelfDao().insert(Bookshelf(name = "Best Books Ever Listings"))
            bookBookshelfDao.insert(BookBookshelf(bookId = bookId, bookshelfId = shelfId))

            val shelves = bookBookshelfDao.getBookshelvesForBook(bookId)

            assertEquals(1, shelves.size)
            assertEquals("Best Books Ever Listings", shelves.first().name)
        }

    @Test
    fun `getBooksForBookshelf returns every book linked to that bookshelf`() =
        runTest {
            val shelfId = database.bookshelfDao().insert(Bookshelf(name = "Best Books Ever Listings"))
            bookBookshelfDao.insert(BookBookshelf(bookId = bookId, bookshelfId = shelfId))

            val books = bookBookshelfDao.getBooksForBookshelf(shelfId)

            assertEquals(1, books.size)
            assertEquals("Pride and Prejudice", books.first().title)
        }

    @Test
    fun `getBookshelvesForBook returns an empty list for a book on no curated shelf`() =
        runTest {
            val shelves = bookBookshelfDao.getBookshelvesForBook(bookId)

            assertTrue(shelves.isEmpty())
        }
}
