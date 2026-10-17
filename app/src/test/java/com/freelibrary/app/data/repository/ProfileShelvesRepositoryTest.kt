package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ProfileSection
import kotlinx.coroutines.flow.first
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
class ProfileShelvesRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: ProfileShelvesRepository
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
                ProfileShelvesRepository(
                    ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 100L }),
                )
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addBook(
        externalId: String,
        state: ShelfState,
    ) {
        val id =
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
        database.bookShelfStateDao().upsert(BookShelfState(id, state, updatedAt = id))
    }

    private suspend fun sections(): Map<String, ProfileSection> = repository.sections.first().associateBy { it.id }

    @Test
    fun `the three shelves are there even when they are empty`() =
        runTest {
            val sections = repository.sections.first()

            assertEquals(listOf("continue-reading", "want-to-read", "finished"), sections.map { it.id })
            sections.forEach {
                assertTrue(it.books.isEmpty())
                assertFalse(it.hasMore)
            }
        }

    @Test
    fun `each book appears on its own shelf`() =
        runTest {
            addBook("1", ShelfState.READING)
            addBook("2", ShelfState.WANT_TO_READ)
            addBook("3", ShelfState.FINISHED)

            val sections = sections()

            assertEquals(listOf("Book 1"), sections.getValue("continue-reading").books.map { it.title })
            assertEquals(listOf("Book 2"), sections.getValue("want-to-read").books.map { it.title })
            assertEquals(listOf("Book 3"), sections.getValue("finished").books.map { it.title })
        }

    @Test
    fun `a long shelf shows ten books and offers View more`() =
        runTest {
            (1..12).forEach { addBook("$it", ShelfState.FINISHED) }

            val finished = sections().getValue("finished")

            assertEquals(CatalogDefaults.BOOKS_PER_SLIDER, finished.books.size)
            assertTrue(finished.hasMore)
        }
}
