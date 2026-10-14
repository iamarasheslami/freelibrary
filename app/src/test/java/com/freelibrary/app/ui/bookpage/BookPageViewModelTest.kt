package com.freelibrary.app.ui.bookpage

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookPageViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var readingState: ReadingStateRepository
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            readingState =
                ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 0L })
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1342",
                        title = "Pride and Prejudice",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-04",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_BOOK_ID to bookId)) =
        BookPageViewModel(
            SavedStateHandle(arguments),
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
            readingState,
        )

    private suspend fun BookPageViewModel.content(): BookPageUiState.Content =
        uiState.first {
            it is BookPageUiState.Content
        } as BookPageUiState.Content

    @Test
    fun `the page starts in the loading state`() {
        assertEquals(BookPageUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `a book on no shelf offers Save`() =
        runTest {
            val content = viewModel().content()

            assertEquals("Pride and Prejudice", content.details.title)
            assertNull(content.shelfState)
            assertEquals(shelfControlsFor(null), content.controls)
        }

    @Test
    fun `an unknown book is reported as not found`() =
        runTest {
            val state = viewModel(mapOf(ARG_BOOK_ID to 999L)).uiState.first { it !is BookPageUiState.Loading }

            assertEquals(BookPageUiState.NotFound, state)
        }

    @Test
    fun `a missing book argument is reported as not found`() =
        runTest {
            val state = viewModel(emptyMap()).uiState.first { it !is BookPageUiState.Loading }

            assertEquals(BookPageUiState.NotFound, state)
        }

    @Test
    fun `Save puts the book on the Want to Read shelf and the buttons follow`() =
        runTest {
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onShelfAction(ShelfAction.SAVE)

            val content =
                viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == ShelfState.WANT_TO_READ }
            assertEquals(shelfControlsFor(ShelfState.WANT_TO_READ), (content as BookPageUiState.Content).controls)
        }

    @Test
    fun `Mark as finished moves a book being read to Finished`() =
        runTest {
            readingState.setState(bookId, ShelfState.READING)
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onShelfAction(ShelfAction.MARK_FINISHED)

            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == ShelfState.FINISHED }
        }

    @Test
    fun `Remove takes the book off its shelf`() =
        runTest {
            readingState.setState(bookId, ShelfState.WANT_TO_READ)
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onShelfAction(ShelfAction.REMOVE)

            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == null }
        }
}
