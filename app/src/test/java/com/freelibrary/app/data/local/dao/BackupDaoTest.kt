package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Highlight
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var backupDao: BackupDao
    private var gutenbergId: Long = 0

    private suspend fun addBook(
        sourceId: Long,
        externalId: String,
    ): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = null,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-16",
            ),
        )

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            backupDao = database.backupDao()
            gutenbergId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `shelf states name their book by source and external id`() =
        runTest {
            val one = addBook(gutenbergId, "1342")
            database.bookShelfStateDao().upsert(BookShelfState(one, ShelfState.FINISHED, updatedAt = 10L, finishedAt = 9L))

            assertEquals(
                listOf(ShelfStateRow("Project Gutenberg", "1342", "FINISHED", 10L, 9L)),
                backupDao.shelfStates(),
            )
        }

    @Test
    fun `reading positions, bookmarks, highlights and downloads are read with their book`() =
        runTest {
            val one = addBook(gutenbergId, "1")
            database.readingProgressDao().upsert(ReadingProgress(one, "0.5", lastOpenedAt = 20L))
            database.bookmarkDao().insert(Bookmark(bookId = one, position = "0.1", note = null, createdAt = 21L))
            database.highlightDao().insert(
                Highlight(bookId = one, startPosition = "0.2", endPosition = "0.3", color = "yellow", createdAt = 22L),
            )
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = one, localFilePath = "/books/1.epub", format = "epub", downloadedAt = 23L),
            )

            assertEquals(listOf(ReadingProgressRow("Project Gutenberg", "1", "0.5", 20L)), backupDao.readingProgress())
            assertEquals(listOf(BookmarkRow("Project Gutenberg", "1", "0.1", null, 21L)), backupDao.bookmarks())
            assertEquals(listOf(HighlightRow("Project Gutenberg", "1", "0.2", "0.3", "yellow", 22L)), backupDao.highlights())
            assertEquals(listOf(DownloadRow("Project Gutenberg", "1", "/books/1.epub", "epub", 23L)), backupDao.downloads())
        }

    @Test
    fun `rows come out oldest first`() =
        runTest {
            val one = addBook(gutenbergId, "1")
            val two = addBook(gutenbergId, "2")
            database.bookShelfStateDao().upsert(BookShelfState(two, ShelfState.READING, updatedAt = 30L))
            database.bookShelfStateDao().upsert(BookShelfState(one, ShelfState.READING, updatedAt = 10L))

            assertEquals(listOf("1", "2"), backupDao.shelfStates().map { it.externalId })
        }

    @Test
    fun `a book is found by source and external id, and the same id under another source is a different book`() =
        runTest {
            val one = addBook(gutenbergId, "1")
            val otherSource = database.sourceDao().insert(Source(name = "Other", attribution = "o"))
            val otherOne = addBook(otherSource, "1")

            assertEquals(one, backupDao.bookId("Project Gutenberg", "1"))
            assertEquals(otherOne, backupDao.bookId("Other", "1"))
            assertNotEquals(backupDao.bookId("Project Gutenberg", "1"), backupDao.bookId("Other", "1"))
        }

    @Test
    fun `a book this phone does not have is not found`() =
        runTest {
            addBook(gutenbergId, "1")

            assertNull(backupDao.bookId("Project Gutenberg", "999"))
            assertNull(backupDao.bookId("Unknown source", "1"))
        }

    @Test
    fun `an empty database has nothing to back up`() =
        runTest {
            assertEquals(emptyList<ShelfStateRow>(), backupDao.shelfStates())
            assertEquals(emptyList<DownloadRow>(), backupDao.downloads())
        }
}
