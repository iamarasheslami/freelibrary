package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Highlight
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
class HighlightDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var highlightDao: HighlightDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            highlightDao = database.highlightDao()

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
                        lastModified = "2026-09-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    private fun highlight(
        start: String,
        end: String,
        color: String?,
        createdAt: Long,
    ) = Highlight(
        bookId = bookId,
        startPosition = start,
        endPosition = end,
        color = color,
        createdAt = createdAt,
    )

    @Test
    fun `insert and retrieve a highlight with a color`() =
        runTest {
            highlightDao.insert(highlight(start = "para-10", end = "para-11", color = "yellow", createdAt = 1000L))

            val highlights = highlightDao.getForBook(bookId)

            assertEquals("yellow", highlights.first().color)
        }

    @Test
    fun `a highlight can be created without a color`() =
        runTest {
            highlightDao.insert(highlight(start = "para-10", end = "para-11", color = null, createdAt = 1000L))

            val highlights = highlightDao.getForBook(bookId)

            assertNull(highlights.first().color)
        }

    @Test
    fun `only the position range is stored, not the highlighted text itself`() =
        runTest {
            highlightDao.insert(highlight(start = "para-10", end = "para-11", color = null, createdAt = 1000L))

            val highlight = highlightDao.getForBook(bookId).first()

            assertEquals("para-10", highlight.startPosition)
            assertEquals("para-11", highlight.endPosition)
        }

    @Test
    fun `delete removes a single highlight by id`() =
        runTest {
            val id = highlightDao.insert(highlight(start = "para-10", end = "para-11", color = null, createdAt = 1000L))
            highlightDao.insert(highlight(start = "para-50", end = "para-51", color = null, createdAt = 2000L))

            highlightDao.delete(id)

            assertEquals(1, highlightDao.getForBook(bookId).size)
        }

    @Test
    fun `deleteAll wipes every highlight, for a full local privacy reset`() =
        runTest {
            highlightDao.insert(highlight(start = "para-10", end = "para-11", color = null, createdAt = 1000L))
            highlightDao.insert(highlight(start = "para-50", end = "para-51", color = null, createdAt = 2000L))

            highlightDao.deleteAll()

            assertTrue(highlightDao.getForBook(bookId).isEmpty())
        }
}
