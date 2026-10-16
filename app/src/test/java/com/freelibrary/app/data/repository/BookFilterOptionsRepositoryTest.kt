package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.FilterOption
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookFilterOptionsRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: BookFilterOptionsRepository
    private var sourceId: Long = 0

    private suspend fun addBook(
        externalId: String,
        language: String?,
        vararg formats: String,
    ): Long {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = "Book $externalId",
                    issuedDate = null,
                    primaryLanguage = language,
                    locc = null,
                    lastModified = "2026-10-16",
                ),
            )
        formats.forEach { type ->
            database.bookFormatDao().insert(
                BookFormat(bookId = bookId, formatType = type, downloadUrl = "https://example.org/$externalId.$type"),
            )
        }
        return bookId
    }

    private suspend fun shelve(
        shelfName: String,
        vararg bookIds: Long,
    ) {
        val shelfId = database.bookshelfDao().insert(Bookshelf(name = shelfName))
        bookIds.forEach { database.bookBookshelfDao().insert(BookBookshelf(bookId = it, bookshelfId = shelfId)) }
    }

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = BookFilterOptionsRepository(database.bookListDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `languages are listed with their book counts, most books first, and books without a language are skipped`() =
        runTest {
            addBook("1", "en")
            addBook("2", "en")
            addBook("3", "fr")
            addBook("4", null)
            addBook("5", "")

            assertEquals(listOf(FilterOption("en", 2), FilterOption("fr", 1)), repository.options().languages)
        }

    @Test
    fun `formats are listed with the number of books that have them`() =
        runTest {
            addBook("1", "en", "epub", "txt")
            addBook("2", "en", "epub")
            addBook("3", "en", "pdf")

            assertEquals(
                listOf(FilterOption("epub", 2), FilterOption("pdf", 1), FilterOption("txt", 1)),
                repository.options().formats,
            )
        }

    @Test
    fun `genres are only the Category shelves that have books`() =
        runTest {
            val a = addBook("1", "en")
            val b = addBook("2", "en")
            shelve("Category: Fiction", a, b)
            shelve("Category: Poetry", a)
            shelve("Category: Empty")
            shelve("Best Books Ever Listings", a, b)

            assertEquals(
                listOf(FilterOption("Category: Fiction", 2), FilterOption("Category: Poetry", 1)),
                repository.options().genres,
            )
        }

    @Test
    fun `an empty catalog has no options`() =
        runTest {
            val options = repository.options()

            assertTrue(options.languages.isEmpty() && options.formats.isEmpty() && options.genres.isEmpty())
        }
}
