package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.HomeShelves
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: HomeRepository
    private var sourceId: Long = 0
    private val harvardShelf = HomeShelves.all.first { it.id == "harvard-classics" }.bookshelfName

    private suspend fun addBook(externalId: String): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = "2026-09-%02d".format(externalId.toInt()),
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-04",
            ),
        )

    private suspend fun shelve(
        bookshelfName: String,
        vararg bookIds: Long,
    ) {
        val shelfId =
            database.bookshelfDao().findByName(bookshelfName)?.id
                ?: database.bookshelfDao().insert(Bookshelf(name = bookshelfName))
        bookIds.forEach { database.bookBookshelfDao().insert(BookBookshelf(bookId = it, bookshelfId = shelfId)) }
    }

    private suspend fun readerShelf(
        bookId: Long,
        state: ShelfState,
    ) = database.bookShelfStateDao().upsert(BookShelfState(bookId = bookId, state = state, updatedAt = 100))

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository =
                HomeRepository(
                    BookListRepository(database.bookListDao()),
                    ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 0L }),
                )
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a new reader sees recently added and the shelves that have books`() =
        runTest {
            val ids = (1..3).map { addBook("$it") }
            shelve(harvardShelf, ids[0], ids[1])

            val sections = repository.observeSections().first()

            assertEquals(listOf("recently-added", "harvard-classics"), sections.map { it.id })
        }

    @Test
    fun `the reader's shelves come first once they hold books`() =
        runTest {
            val reading = addBook("1")
            val wanted = addBook("2")
            readerShelf(reading, ShelfState.READING)
            readerShelf(wanted, ShelfState.WANT_TO_READ)

            val sections = repository.observeSections().first()

            assertEquals(listOf("continue-reading", "want-to-read", "recently-added"), sections.map { it.id })
        }

    @Test
    fun `each reader slider holds only the books on that shelf`() =
        runTest {
            val reading = addBook("1")
            val wanted = addBook("2")
            readerShelf(reading, ShelfState.READING)
            readerShelf(wanted, ShelfState.WANT_TO_READ)

            val sections = repository.observeSections().first()

            assertEquals(listOf("1"), sections.first { it.id == "continue-reading" }.books.map { it.externalId })
            assertEquals(listOf("2"), sections.first { it.id == "want-to-read" }.books.map { it.externalId })
        }

    @Test
    fun `a long shelf shows ten books and offers view more`() =
        runTest {
            shelve(harvardShelf, *(1..12).map { addBook("$it") }.toLongArray())

            val section = repository.observeSections().first().first { it.id == "harvard-classics" }

            assertEquals(10, section.books.size)
            assertTrue(section.hasMore)
        }
}
