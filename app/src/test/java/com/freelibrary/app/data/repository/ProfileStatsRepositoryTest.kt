package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.Highlight
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.ProfileCounts
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
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = ProfileStatsRepository(database.bookmarkDao(), database.highlightDao())
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1",
                        title = "Book 1",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-17",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a reader with nothing saved has zero bookmarks and highlights`() =
        runTest {
            assertEquals(ProfileCounts(0, 0), repository.counts.first())
        }

    @Test
    fun `bookmarks and highlights are counted`() =
        runTest {
            database.bookmarkDao().insert(Bookmark(bookId = bookId, position = "0.1", note = null, createdAt = 1L))
            database.bookmarkDao().insert(Bookmark(bookId = bookId, position = "0.2", note = null, createdAt = 2L))
            database.highlightDao().insert(
                Highlight(bookId = bookId, startPosition = "0.3", endPosition = "0.4", color = null, createdAt = 3L),
            )

            assertEquals(ProfileCounts(2, 1), repository.counts.first())
        }
}
