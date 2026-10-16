package com.freelibrary.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookFts
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.SearchRepository
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.shared.SearchScope
import com.freelibrary.shared.SearchText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: SearchRepository
    private var sourceId: Long = 0
    private val filterHolder = BookFilterHolder()
    private var language = "en"

    private suspend fun addBook(
        externalId: String,
        title: String,
        vararg authorNames: String,
    ) {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = title,
                    issuedDate = null,
                    primaryLanguage = language,
                    locc = null,
                    lastModified = "2026-10-02",
                ),
            )
        authorNames.forEach { name ->
            val authorId = database.authorDao().insert(Author(name = name, birthYear = null, deathYear = null))
            database.bookAuthorDao().insert(BookAuthor(bookId = bookId, authorId = authorId, role = "author"))
        }
        database.bookFtsDao().upsert(
            BookFts(
                bookId = bookId,
                title = SearchText.fold(title),
                authorNames = SearchText.fold(authorNames.joinToString(" ")),
            ),
        )
    }

    @Before
    fun setUp() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = SearchRepository(database.bookFtsDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            addBook("1", "Hamlet", "Shakespeare, William")
        }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** A view model whose state is being watched, as the screen does, for the length of the test. */
    private fun TestScope.watched(handle: SavedStateHandle = SavedStateHandle()): SearchViewModel {
        val viewModel = SearchViewModel(repository, filterHolder, handle)
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private fun SearchState.titles() = (this as SearchState.Results).results.map { it.title }

    @Test
    fun `typing a title shows the matching books once the reader pauses`() =
        runTest {
            val viewModel = watched()

            viewModel.setQuery("hamlet")

            assertEquals(listOf("Hamlet"), viewModel.state.first { it is SearchState.Results }.titles())
        }

    @Test
    fun `a query nothing matches shows no results`() =
        runTest {
            val viewModel = watched()

            viewModel.setQuery("zzzzqqqq")

            assertEquals(SearchState.NoResults, viewModel.state.first { it == SearchState.NoResults })
        }

    @Test
    fun `switching to author searches again by author`() =
        runTest {
            val viewModel = watched()
            viewModel.setQuery("shakespeare")
            viewModel.state.first { it == SearchState.NoResults }

            viewModel.setScope(SearchScope.AUTHOR)

            assertEquals(listOf("Hamlet"), viewModel.state.first { it is SearchState.Results }.titles())
        }

    @Test
    fun `choosing a language filter searches again within that language`() =
        runTest {
            language = "fr"
            addBook("2", "Hamlet Francais", "Writer, A.")
            val viewModel = watched()
            viewModel.setQuery("hamlet")
            viewModel.state.first { it is SearchState.Results && it.results.size == 2 }

            filterHolder.update(BookFilter(language = "fr"))

            val state = viewModel.state.first { it is SearchState.Results && it.results.size == 1 }
            assertEquals(listOf("Hamlet Francais"), state.titles())
        }

    @Test
    fun `it exposes the shared filter`() =
        runTest {
            filterHolder.update(BookFilter(formats = setOf("epub")))

            assertEquals(BookFilter(formats = setOf("epub")), watched().filter.value)
        }

    @Test
    fun `clearing the query goes back to idle`() =
        runTest {
            val viewModel = watched()
            viewModel.setQuery("hamlet")
            viewModel.state.first { it is SearchState.Results }

            viewModel.setQuery("   ")

            assertEquals(SearchState.Idle, viewModel.state.first { it == SearchState.Idle })
        }

    @Test
    fun `the query and the scope come back after the screen is recreated`() =
        runTest {
            val handle = SavedStateHandle()
            val first = watched(handle)
            first.setQuery("hamlet")
            first.setScope(SearchScope.AUTHOR)

            val restored = SearchViewModel(repository, filterHolder, handle)

            assertEquals("hamlet", restored.query.value)
            assertEquals(SearchScope.AUTHOR, restored.scope.value)
        }
}
