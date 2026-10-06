package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.ReadingProgress
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
class ReadingStateRepositoryTest {
    private class FakeTime(var now: Long = 1_000L) : TimeProvider {
        override fun nowMillis(): Long = now
    }

    private lateinit var database: FreeLibraryDatabase
    private lateinit var time: FakeTime
    private lateinit var repository: ReadingStateRepository
    private var sourceId: Long = 0

    private suspend fun addBook(
        externalId: String,
        title: String = "Book $externalId",
    ): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = title,
                issuedDate = null,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-04",
            ),
        )

    private suspend fun stateOf(bookId: Long) = database.bookShelfStateDao().getForBook(bookId)

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            time = FakeTime()
            repository =
                ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), time)
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `opening a book on no shelf makes it Currently Reading`() =
        runTest {
            val book = addBook("1")

            repository.onBookOpened(book)

            assertEquals(ShelfState.READING, stateOf(book)?.state)
        }

    @Test
    fun `opening a Want to Read book moves it to Currently Reading`() =
        runTest {
            val book = addBook("1")
            repository.saveForLater(book)

            repository.onBookOpened(book)

            assertEquals(ShelfState.READING, stateOf(book)?.state)
        }

    @Test
    fun `opening an Already Read book leaves it Already Read`() =
        runTest {
            val book = addBook("1")
            time.now = 2_000
            repository.markFinished(book)
            time.now = 5_000

            repository.onBookOpened(book)

            val state = stateOf(book)
            assertEquals(ShelfState.FINISHED, state?.state)
            assertEquals(2_000L, state?.updatedAt)
            assertEquals(2_000L, state?.finishedAt)
        }

    @Test
    fun `reopening a Currently Reading book moves it to the front of the list`() =
        runTest {
            val first = addBook("1", "First")
            val second = addBook("2", "Second")
            time.now = 1_000
            repository.onBookOpened(first)
            time.now = 2_000
            repository.onBookOpened(second)
            assertEquals(listOf("Second", "First"), repository.observeShelf(ShelfState.READING, 10).first().map { it.title })

            time.now = 3_000
            repository.onBookOpened(first)

            assertEquals(listOf("First", "Second"), repository.observeShelf(ShelfState.READING, 10).first().map { it.title })
        }

    @Test
    fun `save for later puts a new book on Want to Read`() =
        runTest {
            val book = addBook("1")

            repository.saveForLater(book)

            assertEquals(ShelfState.WANT_TO_READ, stateOf(book)?.state)
        }

    @Test
    fun `save for later moves a book that was only opened to Want to Read`() =
        runTest {
            val book = addBook("1")
            repository.onBookOpened(book)

            repository.saveForLater(book)

            assertEquals(ShelfState.WANT_TO_READ, stateOf(book)?.state)
        }

    @Test
    fun `save for later leaves an Already Read book alone`() =
        runTest {
            val book = addBook("1")
            repository.markFinished(book)

            repository.saveForLater(book)

            assertEquals(ShelfState.FINISHED, stateOf(book)?.state)
        }

    @Test
    fun `saving a Want to Read book again changes nothing`() =
        runTest {
            val book = addBook("1")
            time.now = 1_000
            repository.saveForLater(book)
            time.now = 9_000

            repository.saveForLater(book)

            assertEquals(1_000L, stateOf(book)?.updatedAt)
        }

    @Test
    fun `finishing records the time and finishing again keeps the first time`() =
        runTest {
            val book = addBook("1")
            time.now = 2_000
            repository.markFinished(book)
            time.now = 7_000

            repository.markFinished(book)

            assertEquals(2_000L, stateOf(book)?.finishedAt)
        }

    @Test
    fun `moving a book out of Already Read clears the finish time`() =
        runTest {
            val book = addBook("1")
            repository.markFinished(book)

            repository.setState(book, ShelfState.WANT_TO_READ)

            val state = stateOf(book)
            assertEquals(ShelfState.WANT_TO_READ, state?.state)
            assertNull(state?.finishedAt)
        }

    @Test
    fun `removing a book from its shelf keeps its saved reading position`() =
        runTest {
            val book = addBook("1")
            database.readingProgressDao().upsert(ReadingProgress(bookId = book, lastPosition = "page-42", lastOpenedAt = 500))
            repository.onBookOpened(book)

            repository.removeFromShelf(book)

            assertNull(stateOf(book))
            assertEquals("page-42", database.readingProgressDao().getForBook(book)?.lastPosition)
        }

    @Test
    fun `the state of a book and the size of a shelf can be observed`() =
        runTest {
            val opened = addBook("1")
            val other = addBook("2")
            val untouched = addBook("3")
            repository.onBookOpened(opened)
            repository.onBookOpened(other)

            assertEquals(ShelfState.READING, repository.observeStateOf(opened).first())
            assertNull(repository.observeStateOf(untouched).first())
            assertEquals(2, repository.observeCount(ShelfState.READING).first())
        }
}
