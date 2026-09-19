package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.Source
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
class BookFormatDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookFormatDao: BookFormatDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookFormatDao = database.bookFormatDao()

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

    @Test
    fun `getFormatsForBook returns every format for that book`() =
        runTest {
            bookFormatDao.insertAll(
                listOf(
                    BookFormat(bookId = bookId, formatType = "epub", downloadUrl = "https://example.org/1342.epub"),
                    BookFormat(bookId = bookId, formatType = "txt", downloadUrl = "https://example.org/1342.txt"),
                ),
            )

            val formats = bookFormatDao.getFormatsForBook(bookId)

            assertEquals(2, formats.size)
        }

    @Test
    fun `hasFormat returns true when the format exists`() =
        runTest {
            bookFormatDao.insert(BookFormat(bookId = bookId, formatType = "epub", downloadUrl = "https://example.org/1342.epub"))

            assertTrue(bookFormatDao.hasFormat(bookId, "epub"))
        }

    @Test
    fun `hasFormat returns false when the format does not exist`() =
        runTest {
            assertFalse(bookFormatDao.hasFormat(bookId, "pdf"))
        }

    @Test
    fun `getFormatsForBook returns empty list for a book with no formats`() =
        runTest {
            val formats = bookFormatDao.getFormatsForBook(bookId)

            assertTrue(formats.isEmpty())
        }
}
