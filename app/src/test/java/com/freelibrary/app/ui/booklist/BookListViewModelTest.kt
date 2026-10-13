package com.freelibrary.app.ui.booklist

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookListViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private var sourceId: Long = 0

    @Before
    fun setUp() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun addBook(
        externalId: String,
        issuedDate: String? = null,
    ): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = "Book $externalId",
                issuedDate = issuedDate,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-04",
            ),
        )

    private fun viewModel(
        kind: String?,
        bookshelfName: String? = null,
    ) = BookListViewModel(
        SavedStateHandle(
            buildMap {
                kind?.let { put(ARG_KIND, it) }
                bookshelfName?.let { put(ARG_BOOKSHELF_NAME, it) }
            },
        ),
        BookListRepository(database.bookListDao()),
    )

    @Test
    fun `the recently added list shows the newest books first`() =
        runTest {
            (1..3).forEach { addBook("$it", issuedDate = "2026-09-0$it") }

            val books = viewModel(KIND_RECENTLY_ADDED).pages.asSnapshot()

            assertEquals(listOf("Book 3", "Book 2", "Book 1"), books.map { it.title })
        }

    @Test
    fun `a shelf list shows the books of the named shelf`() =
        runTest {
            val one = addBook("1")
            addBook("2")
            val shelfId = database.bookshelfDao().insert(Bookshelf(name = "Poetry"))
            database.bookBookshelfDao().insert(BookBookshelf(bookId = one, bookshelfId = shelfId))

            val books = viewModel(KIND_SHELF, "Poetry").pages.asSnapshot()

            assertEquals(listOf("Book 1"), books.map { it.title })
        }

    @Test
    fun `the continue reading list shows only books being read`() =
        runTest {
            val reading = addBook("1")
            val wanted = addBook("2")
            database.bookShelfStateDao().upsert(BookShelfState(bookId = reading, state = ShelfState.READING, updatedAt = 100))
            database.bookShelfStateDao().upsert(BookShelfState(bookId = wanted, state = ShelfState.WANT_TO_READ, updatedAt = 200))

            val books = viewModel(KIND_CONTINUE_READING).pages.asSnapshot()

            assertEquals(listOf("Book 1"), books.map { it.title })
        }

    @Test
    fun `arguments that name nothing known give an empty list`() =
        runTest {
            addBook("1")

            assertTrue(viewModel("nonsense").pages.asSnapshot().isEmpty())
            assertTrue(viewModel(null).pages.asSnapshot().isEmpty())
            assertTrue(viewModel(KIND_SHELF).pages.asSnapshot().isEmpty())
        }
}
