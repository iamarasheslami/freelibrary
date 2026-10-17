package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.Highlight
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.ProfileStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileStatsRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: ProfileStatsRepository
    private var sourceId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository =
                ProfileStatsRepository(database.bookShelfStateDao(), database.bookmarkDao(), database.highlightDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addBook(externalId: String): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = null,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-17",
            ),
        )

    @Test
    fun `a reader with nothing saved has all zeros`() =
        runTest {
            assertEquals(ProfileStats(0, 0, 0, 0, 0), repository.stats.first())
        }

    @Test
    fun `each shelf bookmark and highlight is counted`() =
        runTest {
            val a = addBook("1")
            val b = addBook("2")
            val c = addBook("3")
            val d = addBook("4")
            database.bookShelfStateDao().upsert(BookShelfState(a, ShelfState.READING, updatedAt = 1L))
            database.bookShelfStateDao().upsert(BookShelfState(b, ShelfState.WANT_TO_READ, updatedAt = 2L))
            database.bookShelfStateDao().upsert(BookShelfState(c, ShelfState.FINISHED, updatedAt = 3L, finishedAt = 3L))
            database.bookShelfStateDao().upsert(BookShelfState(d, ShelfState.FINISHED, updatedAt = 4L, finishedAt = 4L))
            database.bookmarkDao().insert(Bookmark(bookId = a, position = "0.1", note = null, createdAt = 5L))
            database.highlightDao().insert(Highlight(bookId = a, startPosition = "0.2", endPosition = "0.3", color = null, createdAt = 6L))
            database.highlightDao().insert(Highlight(bookId = b, startPosition = "0.4", endPosition = "0.5", color = null, createdAt = 7L))

            assertEquals(ProfileStats(1, 1, 2, 1, 2), repository.stats.first())
        }

    @Test
    fun `moving a book to another shelf changes the counts`() =
        runTest {
            val a = addBook("1")
            database.bookShelfStateDao().upsert(BookShelfState(a, ShelfState.READING, updatedAt = 1L))
            database.bookShelfStateDao().upsert(BookShelfState(a, ShelfState.FINISHED, updatedAt = 2L, finishedAt = 2L))

            assertEquals(ProfileStats(0, 0, 1, 0, 0), repository.stats.first())
        }
}
