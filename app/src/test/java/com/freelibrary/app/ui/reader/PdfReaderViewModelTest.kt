package com.freelibrary.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PdfReaderViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var readerRepository: ReaderRepository
    private lateinit var readingState: ReadingStateRepository
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            readingState =
                ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 0L })
            readerRepository = ReaderRepository(database.downloadedBookDao(), database.readingProgressDao(), TimeProvider { 0L })
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
        // Work still on its way must finish while the main dispatcher is still set, or it fails in a later test.
        runBlocking { withTimeoutOrNull(5_000) { created.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        database.close()
        Dispatchers.resetMain()
    }

    private val created = mutableListOf<PdfReaderViewModel>()

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_BOOK_ID to bookId)): PdfReaderViewModel =
        build(arguments).also { created.add(it) }

    private fun build(arguments: Map<String, Any?>) =
        PdfReaderViewModel(
            SavedStateHandle(arguments),
            readerRepository,
            readingState,
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
        )

    private suspend fun PdfReaderViewModel.settled(): PdfReaderUiState =
        withTimeout(10_000) {
            uiState.first { it !is PdfReaderUiState.Loading }
        }

    @Test
    fun `a book that was not downloaded as a PDF cannot be opened`() =
        runBlocking<Unit> {
            val state = viewModel().settled()

            assertEquals(PdfReaderUiState.Failed(PdfFailure.NOT_DOWNLOADED), state)
        }

    @Test
    fun `a downloaded file that is not a PDF is reported`() =
        runBlocking<Unit> {
            val file = temporaryFolder.newFile("$bookId.pdf")
            file.writeText("This is plain text.")
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = file.path, format = "pdf", downloadedAt = 1L),
            )

            val state = viewModel().settled()

            assertEquals(PdfReaderUiState.Failed(PdfFailure.NOT_A_PDF), state)
        }

    @Test
    fun `without a book in the arguments there is nothing to open`() =
        runBlocking<Unit> {
            val state = viewModel(emptyMap()).settled()

            assertEquals(PdfReaderUiState.Failed(PdfFailure.NOT_DOWNLOADED), state)
        }
}
