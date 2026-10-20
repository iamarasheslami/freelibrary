package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.ReaderPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: BookmarkRepository
    private var now = 1_000L
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runBlocking {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = BookmarkRepository(database.bookmarkDao(), TimeProvider { now })
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "2701",
                        title = "Moby Dick",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a bookmark is saved with a trimmed note and the time`() =
        runBlocking<Unit> {
            assertTrue(repository.add(bookId, ReaderPosition.Epub(2, 300), "  Key idea  "))

            val saved = repository.observe(bookId).first().single()

            assertEquals(SavedBookmark(saved.id, bookId, "Key idea", ReaderPosition.Epub(2, 300), 1_000L), saved)
        }

    @Test
    fun `a blank note is saved as no note and a long one is cut`() =
        runBlocking<Unit> {
            repository.add(bookId, ReaderPosition.Pdf(1), "   \n ")
            repository.add(bookId, ReaderPosition.Pdf(2), "x".repeat(500))

            val saved = repository.observe(bookId).first()

            assertNull(saved[0].note)
            assertEquals(BookmarkRepository.MAX_NOTE_LENGTH, saved[1].note?.length)
        }

    @Test
    fun `bookmarks come back in the order of the book`() =
        runBlocking<Unit> {
            repository.add(bookId, ReaderPosition.Text(500), null)
            repository.add(bookId, ReaderPosition.Pdf(7), null)
            repository.add(bookId, ReaderPosition.Epub(3, 100), null)
            repository.add(bookId, ReaderPosition.Epub(1, 900), null)
            repository.add(bookId, ReaderPosition.Epub(1, 100), null)

            val positions = repository.observe(bookId).first().map { it.position }

            assertEquals(
                listOf(
                    ReaderPosition.Epub(1, 100),
                    ReaderPosition.Epub(1, 900),
                    ReaderPosition.Epub(3, 100),
                    ReaderPosition.Pdf(7),
                    ReaderPosition.Text(500),
                ),
                positions,
            )
        }

    @Test
    fun `a bookmark whose place is not understood is left out`() =
        runBlocking<Unit> {
            database.bookmarkDao().insert(Bookmark(bookId = bookId, position = "weird:1", note = null, createdAt = 1L))
            repository.add(bookId, ReaderPosition.Pdf(4), null)

            assertEquals(listOf(ReaderPosition.Pdf(4)), repository.observe(bookId).first().map { it.position })
        }

    @Test
    fun `a bookmark for a book that is not in the catalog is not saved`() =
        runBlocking<Unit> {
            assertFalse(repository.add(bookId + 99, ReaderPosition.Pdf(1), null))
        }

    @Test
    fun `a deleted bookmark can be put back as it was`() =
        runBlocking<Unit> {
            repository.add(bookId, ReaderPosition.Epub(2, 300), "Remember")
            val original = repository.observe(bookId).first().single()

            assertTrue(repository.delete(original.id))
            assertTrue(repository.observe(bookId).first().isEmpty())

            assertTrue(repository.restore(original))
            assertEquals(listOf(original), repository.observe(bookId).first())
        }
}
