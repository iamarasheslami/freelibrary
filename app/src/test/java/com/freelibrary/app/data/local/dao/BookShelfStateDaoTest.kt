package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookShelfStateDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var shelfDao: BookShelfStateDao
    private var sourceId: Long = 0
    private var bookOneId: Long = 0
    private var bookTwoId: Long = 0
    private var bookThreeId: Long = 0

    private suspend fun insertBook(externalId: String): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = null,
                primaryLanguage = null,
                locc = null,
                lastModified = "2026-10-02",
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
            shelfDao = database.bookShelfStateDao()

            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookOneId = insertBook("1")
            bookTwoId = insertBook("2")
            bookThreeId = insertBook("3")
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `every shelf state survives a save and read`() =
        runTest {
            ShelfState.values().forEach { state ->
                shelfDao.upsert(BookShelfState(bookId = bookOneId, state = state, updatedAt = 100))

                assertEquals(state, shelfDao.getForBook(bookOneId)?.state)
            }
        }

    @Test
    fun `getForBook returns null for a book that is on no shelf`() =
        runTest {
            assertNull(shelfDao.getForBook(bookOneId))
        }

    @Test
    fun `upsert moves a book to a new state instead of adding a second row`() =
        runTest {
            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.WANT_TO_READ, updatedAt = 100))

            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.READING, updatedAt = 200))

            assertEquals(ShelfState.READING, shelfDao.getForBook(bookOneId)?.state)
            assertEquals(0, shelfDao.observeCount(ShelfState.WANT_TO_READ).first())
            assertEquals(1, shelfDao.observeCount(ShelfState.READING).first())
        }

    @Test
    fun `finishedAt is stored for a finished book`() =
        runTest {
            shelfDao.upsert(
                BookShelfState(bookId = bookOneId, state = ShelfState.FINISHED, updatedAt = 300, finishedAt = 300),
            )

            assertEquals(300L, shelfDao.getForBook(bookOneId)?.finishedAt)
        }

    @Test
    fun `observeByState returns only that state, newest first`() =
        runTest {
            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.READING, updatedAt = 100))
            shelfDao.upsert(BookShelfState(bookId = bookTwoId, state = ShelfState.READING, updatedAt = 300))
            shelfDao.upsert(BookShelfState(bookId = bookThreeId, state = ShelfState.WANT_TO_READ, updatedAt = 200))

            val reading = shelfDao.observeByState(ShelfState.READING, limit = 10).first()

            assertEquals(listOf(bookTwoId, bookOneId), reading.map { it.bookId })
        }

    @Test
    fun `observeByState respects the limit`() =
        runTest {
            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.WANT_TO_READ, updatedAt = 100))
            shelfDao.upsert(BookShelfState(bookId = bookTwoId, state = ShelfState.WANT_TO_READ, updatedAt = 200))
            shelfDao.upsert(BookShelfState(bookId = bookThreeId, state = ShelfState.WANT_TO_READ, updatedAt = 300))

            val shown = shelfDao.observeByState(ShelfState.WANT_TO_READ, limit = 2).first()

            assertEquals(listOf(bookThreeId, bookTwoId), shown.map { it.bookId })
            assertEquals(3, shelfDao.observeCount(ShelfState.WANT_TO_READ).first())
        }

    @Test
    fun `delete removes one book from its shelf and deleteAll clears every shelf`() =
        runTest {
            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.READING, updatedAt = 100))
            shelfDao.upsert(BookShelfState(bookId = bookTwoId, state = ShelfState.FINISHED, updatedAt = 200))

            shelfDao.delete(bookOneId)

            assertNull(shelfDao.getForBook(bookOneId))
            assertEquals(ShelfState.FINISHED, shelfDao.getForBook(bookTwoId)?.state)

            shelfDao.deleteAll()

            assertNull(shelfDao.getForBook(bookTwoId))
        }

    @Test
    fun `deleting the book also removes its shelf state through the foreign key cascade`() =
        runTest {
            shelfDao.upsert(BookShelfState(bookId = bookOneId, state = ShelfState.READING, updatedAt = 100))

            database.openHelper.writableDatabase.execSQL("DELETE FROM books WHERE id = $bookOneId")

            assertNull(shelfDao.getForBook(bookOneId))
        }
}
