package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookSummary
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookSummaryDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookSummaryDao: BookSummaryDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookSummaryDao = database.bookSummaryDao()

            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1",
                        title = "Book One",
                        issuedDate = null,
                        primaryLanguage = null,
                        locc = null,
                        lastModified = "2026-10-02",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert saves a summary that can be read back, including non-ASCII text`() =
        runTest {
            val text = "Un roman « fictif » — avec des accents: é, ü, ñ."

            bookSummaryDao.upsert(BookSummary(bookId = bookId, summary = text))

            assertEquals(text, bookSummaryDao.getSummaryForBook(bookId))
        }

    @Test
    fun `getSummaryForBook returns null when the book has no summary`() =
        runTest {
            assertNull(bookSummaryDao.getSummaryForBook(bookId))
        }

    @Test
    fun `upsert replaces an existing summary for the same book`() =
        runTest {
            bookSummaryDao.upsert(BookSummary(bookId = bookId, summary = "Old text."))

            bookSummaryDao.upsert(BookSummary(bookId = bookId, summary = "Corrected text."))

            assertEquals("Corrected text.", bookSummaryDao.getSummaryForBook(bookId))
        }

    @Test
    fun `deleteForBook removes the summary`() =
        runTest {
            bookSummaryDao.upsert(BookSummary(bookId = bookId, summary = "Some text."))

            bookSummaryDao.deleteForBook(bookId)

            assertNull(bookSummaryDao.getSummaryForBook(bookId))
        }

    @Test
    fun `deleting the book also deletes its summary through the foreign key cascade`() =
        runTest {
            bookSummaryDao.upsert(BookSummary(bookId = bookId, summary = "Some text."))

            database.openHelper.writableDatabase.execSQL("DELETE FROM books WHERE id = $bookId")

            assertNull(bookSummaryDao.getSummaryForBook(bookId))
        }
}
