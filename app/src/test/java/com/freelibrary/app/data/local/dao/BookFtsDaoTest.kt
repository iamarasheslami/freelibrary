package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFts
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
class BookFtsDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookFtsDao: BookFtsDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookFtsDao = database.bookFtsDao()

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
            bookFtsDao.upsert(BookFts(bookId = bookId, title = "Pride and Prejudice", authorNames = "Jane Austen"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `search finds a book by a matching title word`() =
        runTest {
            val results = bookFtsDao.search("Prejudice")

            assertEquals(1, results.size)
            assertEquals("Pride and Prejudice", results.first().title)
        }

    @Test
    fun `search finds a book by author name, not just title`() =
        runTest {
            val results = bookFtsDao.search("Austen")

            assertEquals(1, results.size)
        }

    @Test
    fun `search supports prefix matching`() =
        runTest {
            val results = bookFtsDao.search("Pri*")

            assertEquals(1, results.size)
        }

    @Test
    fun `search returns no results for a term that matches nothing`() =
        runTest {
            val results = bookFtsDao.search("Nonexistent")

            assertTrue(results.isEmpty())
        }

    @Test
    fun `deleteForBook removes the book from the search index`() =
        runTest {
            bookFtsDao.deleteForBook(bookId)

            val results = bookFtsDao.search("Prejudice")

            assertTrue(results.isEmpty())
        }
}
