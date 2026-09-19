package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.Source
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
class ReadingProgressDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var readingProgressDao: ReadingProgressDao
    private var bookOneId: Long = 0
    private var bookTwoId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            readingProgressDao = database.readingProgressDao()

            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookOneId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1",
                        title = "Book One",
                        issuedDate = null,
                        primaryLanguage = null,
                        locc = null,
                        lastModified = "2026-09-16",
                    ),
                )
            bookTwoId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "2",
                        title = "Book Two",
                        issuedDate = null,
                        primaryLanguage = null,
                        locc = null,
                        lastModified = "2026-09-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert saves a new reading position`() =
        runTest {
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "para-42", lastOpenedAt = 1000L))

            val saved = readingProgressDao.getForBook(bookOneId)

            assertEquals("para-42", saved?.lastPosition)
        }

    @Test
    fun `upsert overwrites the previous position for the same book rather than duplicating`() =
        runTest {
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "para-10", lastOpenedAt = 1000L))
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "para-50", lastOpenedAt = 2000L))

            val saved = readingProgressDao.getForBook(bookOneId)

            assertEquals("para-50", saved?.lastPosition)
        }

    @Test
    fun `getForBook returns null for a book that has never been opened`() =
        runTest {
            val saved = readingProgressDao.getForBook(bookOneId)

            assertNull(saved)
        }

    @Test
    fun `getMostRecent orders by lastOpenedAt descending`() =
        runTest {
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "p1", lastOpenedAt = 1000L))
            readingProgressDao.upsert(ReadingProgress(bookId = bookTwoId, lastPosition = "p2", lastOpenedAt = 2000L))

            val recent = readingProgressDao.getMostRecent(limit = 5)

            assertEquals(bookTwoId, recent.first().bookId)
        }

    @Test
    fun `delete removes progress for a single book only`() =
        runTest {
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "p1", lastOpenedAt = 1000L))
            readingProgressDao.upsert(ReadingProgress(bookId = bookTwoId, lastPosition = "p2", lastOpenedAt = 2000L))

            readingProgressDao.delete(bookOneId)

            assertNull(readingProgressDao.getForBook(bookOneId))
            assertEquals("p2", readingProgressDao.getForBook(bookTwoId)?.lastPosition)
        }

    @Test
    fun `deleteAll wipes every reading position, for a full local privacy reset`() =
        runTest {
            readingProgressDao.upsert(ReadingProgress(bookId = bookOneId, lastPosition = "p1", lastOpenedAt = 1000L))
            readingProgressDao.upsert(ReadingProgress(bookId = bookTwoId, lastPosition = "p2", lastOpenedAt = 2000L))

            readingProgressDao.deleteAll()

            assertTrue(readingProgressDao.getMostRecent(limit = 10).isEmpty())
        }
}
