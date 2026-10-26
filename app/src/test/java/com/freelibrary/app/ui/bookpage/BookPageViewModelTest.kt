package com.freelibrary.app.ui.bookpage

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.download.BookDownloadManager
import com.freelibrary.app.data.download.BookDownloader
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

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

    private fun downloads() =
        BookDownloadManager(
            BookDownloader(
                OkHttpClient(),
                BookStorage(File("unused-in-these-tests")),
                database.downloadedBookDao(),
                TimeProvider { 0L },
                1_000_000L,
            ),
            database.bookFormatDao(),
            database.downloadedBookDao(),
        )

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_BOOK_ID to bookId)) =
        BookPageViewModel(
            SavedStateHandle(arguments),
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
            readingState,
            downloads(),
        )

    private suspend fun BookPageViewModel.content(): BookPageUiState.Content =
        uiState.first {
            it is BookPageUiState.Content
        } as BookPageUiState.Content

    @Test
    fun `the files section lists the formats the catalog offers, EPUB first`() =
        runTest {
            database.bookFormatDao().insertAll(
                listOf(
                    BookFormat(bookId = bookId, formatType = "txt", downloadUrl = "https://example.org/1.txt"),
                    BookFormat(bookId = bookId, formatType = "epub", downloadUrl = "https://example.org/1.epub"),
                ),
            )

            val files = viewModel().files.first { it != null }

            assertEquals(listOf(BookFileFormat.EPUB, BookFileFormat.TEXT), files?.available)
            assertEquals(emptyList<BookFileFormat>(), files?.downloaded)
            assertNull(files?.transfer)
        }

    @Test
    fun `a book with no known format offers nothing to download`() =
        runTest {
            val files = viewModel().files.first { it != null }

            assertEquals(emptyList<BookFileFormat>(), files?.available)
        }

    @Test
    fun `the page starts in the loading state`() {
        assertEquals(BookPageUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `a book on no shelf has no shelf state`() =
        runTest {
            val content = viewModel().content()

            assertEquals("Pride and Prejudice", content.details.title)
            assertNull(content.shelfState)
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
    fun `Save puts the book on the Want to Read shelf and a second tap takes it off`() =
        runTest {
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onToggleSaved()
            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == ShelfState.WANT_TO_READ }

            viewModel.onToggleSaved()
            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == null }
        }

    @Test
    fun `Finished moves a book being read to Finished and a second tap takes it off`() =
        runTest {
            readingState.setState(bookId, ShelfState.READING)
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onToggleFinished()
            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == ShelfState.FINISHED }

            viewModel.onToggleFinished()
            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == null }
        }

    @Test
    fun `Start over takes a book being read off Currently Reading`() =
        runTest {
            readingState.setState(bookId, ShelfState.READING)
            val viewModel = viewModel()
            viewModel.content()

            viewModel.onRestart()

            viewModel.uiState.first { it is BookPageUiState.Content && it.shelfState == null }
        }
}
