package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.ShelfState
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
class ReadingHistoryRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: ReadingHistoryRepository
    private var sourceId: Long = 0

    private suspend fun addBook(externalId: String): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = null,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-04",
            ),
        )

    private suspend fun shelve(
        bookId: Long,
        state: ShelfState,
    ) {
        database.bookShelfStateDao().upsert(BookShelfState(bookId, state, updatedAt = 1L))
        database.readingProgressDao().upsert(ReadingProgress(bookId, "epub:1:500", lastOpenedAt = 1L))
    }

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = ReadingHistoryRepository(database, database.bookShelfStateDao(), database.readingProgressDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `clearing keeps the Want to Read list and forgets reading and finished books`() =
        runTest {
            val wanted = addBook("1")
            val reading = addBook("2")
            val finished = addBook("3")
            shelve(wanted, ShelfState.WANT_TO_READ)
            shelve(reading, ShelfState.READING)
            shelve(finished, ShelfState.FINISHED)

            repository.clear()

            assertEquals(ShelfState.WANT_TO_READ, database.bookShelfStateDao().getForBook(wanted)?.state)
            assertNull(database.bookShelfStateDao().getForBook(reading))
            assertNull(database.bookShelfStateDao().getForBook(finished))
        }

    @Test
    fun `clearing deletes every saved reading position`() =
        runTest {
            val wanted = addBook("1")
            val reading = addBook("2")
            shelve(wanted, ShelfState.WANT_TO_READ)
            shelve(reading, ShelfState.READING)

            repository.clear()

            assertEquals(emptyList<ReadingProgress>(), database.readingProgressDao().getMostRecent(10))
        }

    @Test
    fun `clearing leaves bookmarks alone`() =
        runTest {
            val reading = addBook("1")
            shelve(reading, ShelfState.READING)
            database.bookmarkDao().insert(Bookmark(bookId = reading, position = "epub:1:500", note = "Mine", createdAt = 1L))

            repository.clear()

            assertEquals(1, database.bookmarkDao().getForBook(reading).size)
        }

    @Test
    fun `clearing an empty history does nothing and does not fail`() =
        runTest {
            repository.clear()

            assertEquals(emptyList<ReadingProgress>(), database.readingProgressDao().getMostRecent(10))
        }
}
