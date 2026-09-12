package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Bookmark
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
class BookmarkDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookmarkDao = database.bookmarkDao()

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

    private fun bookmark(
        position: String,
        note: String?,
        createdAt: Long,
    ) = Bookmark(
        bookId = bookId,
        position = position,
        note = note,
        createdAt = createdAt,
    )

    @Test
    fun `insert and retrieve a bookmark with a note`() =
        runTest {
            bookmarkDao.insert(bookmark(position = "para-10", note = "Favorite line", createdAt = 1000L))

            val bookmarks = bookmarkDao.getForBook(bookId)

            assertEquals("Favorite line", bookmarks.first().note)
        }

    @Test
    fun `a bookmark can be created without a note`() =
        runTest {
            bookmarkDao.insert(bookmark(position = "para-10", note = null, createdAt = 1000L))

            val bookmarks = bookmarkDao.getForBook(bookId)

            assertNull(bookmarks.first().note)
        }

    @Test
    fun `a book can have more than one bookmark`() =
        runTest {
            bookmarkDao.insert(bookmark(position = "para-10", note = null, createdAt = 1000L))
            bookmarkDao.insert(bookmark(position = "para-50", note = null, createdAt = 2000L))

            val bookmarks = bookmarkDao.getForBook(bookId)

            assertEquals(2, bookmarks.size)
        }

    @Test
    fun `delete removes a single bookmark by id`() =
        runTest {
            val id = bookmarkDao.insert(bookmark(position = "para-10", note = null, createdAt = 1000L))
            bookmarkDao.insert(bookmark(position = "para-50", note = null, createdAt = 2000L))

            bookmarkDao.delete(id)

            assertEquals(1, bookmarkDao.getForBook(bookId).size)
        }

    @Test
    fun `deleteAll wipes every bookmark, for a full local privacy reset`() =
        runTest {
            bookmarkDao.insert(bookmark(position = "para-10", note = null, createdAt = 1000L))
            bookmarkDao.insert(bookmark(position = "para-50", note = null, createdAt = 2000L))

            bookmarkDao.deleteAll()

            assertTrue(bookmarkDao.getForBook(bookId).isEmpty())
        }
}
