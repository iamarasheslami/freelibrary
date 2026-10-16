package com.freelibrary.app.ui.explore

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.BookListRepository
import com.freelibrary.app.data.repository.ExploreRepository
import com.freelibrary.app.domain.model.BookFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExploreViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private var sourceId: Long = 0
    private var nextExternalId = 1
    private val filterHolder = BookFilterHolder()

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

    private suspend fun addShelf(
        name: String,
        books: Int,
        language: String = "en",
    ) {
        val shelfId = database.bookshelfDao().insert(Bookshelf(name = name))
        repeat(books) {
            val externalId = "${nextExternalId++}"
            val bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = externalId,
                        title = "Book $externalId",
                        issuedDate = null,
                        primaryLanguage = language,
                        locc = null,
                        lastModified = "2026-10-04",
                    ),
                )
            database.bookBookshelfDao().insert(BookBookshelf(bookId = bookId, bookshelfId = shelfId))
        }
    }

    private fun viewModel() =
        ExploreViewModel(
            ExploreRepository(database.bookListDao()),
            BookListRepository(database.bookListDao()),
            filterHolder,
        )

    @Test
    fun `collections come first, then genres, then other shelves`() =
        runTest {
            addShelf("Poetry", 11)
            addShelf("Category: Adventure", 10)
            addShelf("Harvard Classics", 12)

            val rows = viewModel().rows.asSnapshot()

            assertEquals(
                listOf("collection:harvard-classics", "genre:Category: Adventure", "shelf:Poetry"),
                rows.map { it.section.id },
            )
        }

    @Test
    fun `each row carries the first ten books of its shelf and whether there are more`() =
        runTest {
            addShelf("Harvard Classics", 12)
            addShelf("Category: Adventure", 10)

            val rows = viewModel().rows.asSnapshot()

            val collection = rows[0].slider
            assertEquals(10, collection.books.size)
            assertEquals(12, collection.totalBooks)
            assertTrue(collection.hasMore)
            val genre = rows[1].slider
            assertEquals(10, genre.books.size)
            assertFalse(genre.hasMore)
        }

    @Test
    fun `choosing a language shows the shelves of that language`() =
        runTest {
            addShelf("Poetry", 10, language = "fr")
            addShelf("Drama", 10)
            filterHolder.update(BookFilter(language = "fr"))

            val rows = viewModel().rows.asSnapshot()

            assertEquals(listOf("shelf:Poetry"), rows.map { it.section.id })
        }

    @Test
    fun `changing the filter builds the page again`() =
        runTest {
            addShelf("Poetry", 10, language = "fr")
            addShelf("Drama", 10)
            val viewModel = viewModel()

            val before = viewModel.rows.asSnapshot().map { it.section.id }
            filterHolder.update(BookFilter(language = "fr"))
            val after = viewModel.rows.asSnapshot().map { it.section.id }

            assertEquals(listOf("shelf:Drama"), before)
            assertEquals(listOf("shelf:Poetry"), after)
        }

    @Test
    fun `a shelf with too few books is not listed`() =
        runTest {
            addShelf("Poetry", 9)

            assertTrue(viewModel().rows.asSnapshot().isEmpty())
        }
}
