package com.freelibrary.app.ui.reader

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.reader.PdfBook
import com.freelibrary.app.data.reader.PdfOpenResult
import com.freelibrary.app.data.reader.PdfProblem
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Why the reader could not open the PDF. */
enum class PdfFailure {
    NOT_DOWNLOADED,
    NOT_A_PDF,
    PASSWORD_PROTECTED,
    DAMAGED,
    EMPTY,
}

sealed interface PdfReaderUiState {
    data object Loading : PdfReaderUiState

    data class Failed(
        val reason: PdfFailure,
    ) : PdfReaderUiState

    /** The PDF is open. [title] is the catalog title. */
    data class Ready(
        val book: PdfBook,
        val title: String?,
    ) : PdfReaderUiState
}

private const val SAVE_DELAY_MS = 1_500L

/**
 * Opens a downloaded PDF for the reader screen and keeps the page the reader is on. Opening a book
 * follows the shelf rules (it becomes Currently Reading unless it is already finished). The page is
 * saved a moment after the reader stops turning pages, and at once when [saveNow] is called.
 * Reaching the last page asks once whether to mark the book finished.
 */
@HiltViewModel
class PdfReaderViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val readerRepository: ReaderRepository,
        private val readingStateRepository: ReadingStateRepository,
        private val detailsRepository: BookDetailsRepository,
    ) : ViewModel() {
        private val bookId: Long? = savedStateHandle.get<Long>(ARG_BOOK_ID)

        private val _uiState = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Loading)
        val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

        /** The page the reader is on, counted from 0. It survives a rotation. */
        var page: Int = 0
            private set

        private val endEvents = Channel<Unit>(Channel.CONFLATED)

        /** Emits once, when the reader reaches the last page and the book is not already finished. */
        val endReached: Flow<Unit> = endEvents.receiveAsFlow()

        private var endPrompted = false
        private var saveJob: Job? = null
        private var openedBook: PdfBook? = null

        init {
            open()
        }

        private fun open() {
            viewModelScope.launch {
                val id = bookId
                val file = id?.let { readerRepository.pdfFile(it) }
                if (id == null || file == null) {
                    _uiState.value = PdfReaderUiState.Failed(PdfFailure.NOT_DOWNLOADED)
                    return@launch
                }
                // Opening must finish even if the screen is closed meanwhile, or the file would stay open.
                val result = withContext(Dispatchers.IO + NonCancellable) { PdfBook.open(file) }
                when (result) {
                    is PdfOpenResult.Failed -> _uiState.value = PdfReaderUiState.Failed(result.problem.toFailure())
                    is PdfOpenResult.Opened -> {
                        if (isActive) begin(id, result.book) else result.book.close()
                    }
                }
            }
        }

        private suspend fun begin(
            id: Long,
            book: PdfBook,
        ) {
            openedBook = book
            page = (readerRepository.savedPdfPosition(id)?.page ?: 0).coerceIn(0, book.pageCount - 1)
            // Neither of these may stop the reader from reading.
            try {
                readingStateRepository.onBookOpened(id)
            } catch (e: SQLiteException) {
                // The shelf is not updated this time.
            }
            val title =
                try {
                    detailsRepository.load(id)?.title
                } catch (e: SQLiteException) {
                    null
                }
            _uiState.value = PdfReaderUiState.Ready(book, title)
        }

        /** The reader is on [newPage] (from 0). */
        fun onPageChanged(newPage: Int) {
            val ready = _uiState.value as? PdfReaderUiState.Ready ?: return
            val id = bookId ?: return
            if (newPage !in 0 until ready.book.pageCount) return
            page = newPage
            if (newPage == ready.book.pageCount - 1) promptToFinishOnce(id)
            saveJob?.cancel()
            saveJob =
                viewModelScope.launch {
                    delay(SAVE_DELAY_MS)
                    readerRepository.savePosition(id, ReaderPosition.Pdf(newPage))
                }
        }

        /** Saves the page now, for when the reader leaves the screen. */
        fun saveNow() {
            if (_uiState.value !is PdfReaderUiState.Ready) return
            val id = bookId ?: return
            saveJob?.cancel()
            val toSave = ReaderPosition.Pdf(page)
            viewModelScope.launch { withContext(NonCancellable) { readerRepository.savePosition(id, toSave) } }
        }

        /** The reader chose to mark the book as finished. */
        fun onMarkFinished() {
            val id = bookId ?: return
            viewModelScope.launch { readingStateRepository.markFinished(id) }
        }

        private fun promptToFinishOnce(id: Long) {
            if (endPrompted) return
            endPrompted = true
            viewModelScope.launch {
                if (readingStateRepository.observeStateOf(id).first() != ShelfState.FINISHED) endEvents.trySend(Unit)
            }
        }

        override fun onCleared() {
            openedBook?.close()
            openedBook = null
        }

        private fun PdfProblem.toFailure(): PdfFailure =
            when (this) {
                PdfProblem.NOT_A_PDF -> PdfFailure.NOT_A_PDF
                PdfProblem.PASSWORD_PROTECTED -> PdfFailure.PASSWORD_PROTECTED
                PdfProblem.DAMAGED -> PdfFailure.DAMAGED
                PdfProblem.EMPTY -> PdfFailure.EMPTY
            }
    }
