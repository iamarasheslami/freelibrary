package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
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
class DownloadedBookDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var downloadedBookDao: DownloadedBookDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            downloadedBookDao = database.downloadedBookDao()

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

    private fun epubDownload(downloadedAt: Long) =
        DownloadedBook(
            bookId = bookId,
            localFilePath = "/books/1342.epub",
            format = "epub",
            downloadedAt = downloadedAt,
        )

    private fun txtDownload(downloadedAt: Long) =
        DownloadedBook(
            bookId = bookId,
            localFilePath = "/books/1342.txt",
            format = "txt",
            downloadedAt = downloadedAt,
        )

    @Test
    fun `a book can have both epub and txt downloaded at the same time`() =
        runTest {
            downloadedBookDao.insert(epubDownload(downloadedAt = 1000L))
            downloadedBookDao.insert(txtDownload(downloadedAt = 2000L))

            val formats = downloadedBookDao.getFormatsForBook(bookId)

            assertEquals(2, formats.size)
        }

    @Test
    fun `isDownloaded is true when at least one format exists`() =
        runTest {
            downloadedBookDao.insert(epubDownload(downloadedAt = 1000L))

            assertTrue(downloadedBookDao.isDownloaded(bookId))
        }

    @Test
    fun `isDownloaded is false when no format has been downloaded`() =
        runTest {
            assertFalse(downloadedBookDao.isDownloaded(bookId))
        }

    @Test
    fun `delete removes only the specified format, leaving other formats intact`() =
        runTest {
            downloadedBookDao.insert(epubDownload(downloadedAt = 1000L))
            downloadedBookDao.insert(txtDownload(downloadedAt = 2000L))

            downloadedBookDao.delete(bookId, "epub")

            val remaining = downloadedBookDao.getFormatsForBook(bookId)
            assertEquals(1, remaining.size)
            assertEquals("txt", remaining.first().format)
        }

    @Test
    fun `deleteAllFormatsForBook removes every downloaded format for that book`() =
        runTest {
            downloadedBookDao.insert(epubDownload(downloadedAt = 1000L))
            downloadedBookDao.insert(txtDownload(downloadedAt = 2000L))

            downloadedBookDao.deleteAllFormatsForBook(bookId)

            assertFalse(downloadedBookDao.isDownloaded(bookId))
        }

    @Test
    fun `getAllDownloads orders by most recently downloaded first`() =
        runTest {
            downloadedBookDao.insert(txtDownload(downloadedAt = 1000L))
            downloadedBookDao.insert(epubDownload(downloadedAt = 2000L))

            val all = downloadedBookDao.getAllDownloads()

            assertEquals("epub", all.first().format)
        }
}
