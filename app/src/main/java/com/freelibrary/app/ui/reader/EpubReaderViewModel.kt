package com.freelibrary.app.ui.reader

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.reader.EpubBook
import com.freelibrary.app.data.reader.EpubOpenResult
import com.freelibrary.app.data.reader.EpubProblem
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

/** Why the reader could not open the book. */
enum class ReaderFailure {
    NOT_DOWNLOADED,
    NOT_AN_EPUB,
    DAMAGED,
    EMPTY,
}

sealed interface EpubReaderUiState {
    data object Loading : EpubReaderUiState

    data class Failed(
        val reason: ReaderFailure,
    ) : EpubReaderUiState

    /** The book is open. [title] is the catalog title, or the book's own if the catalog has none. */
    data class Ready(
        val book: EpubBook,
        val title: String?,
    ) : EpubReaderUiState
}

private const val SAVE_DELAY_MS = 1_500L

/** How far down the last chapter counts as having reached the end of the book, in thousandths. */
private const val END_PERMILLE = 980

/**
 * Opens a downloaded EPUB for the reader screen and keeps where the reader is. Opening a book
 * follows the shelf rules (it becomes Currently Reading unless it is already finished). The position
 * is saved a moment after the reader stops moving, and at once when [saveNow] is called. Reaching
 * the end of the last chapter asks once whether to mark the book finished.
 */
@HiltViewModel
class EpubReaderViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val readerRepository: ReaderRepository,
        private val readingStateRepository: ReadingStateRepository,
        private val detailsRepository: BookDetailsRepository,
    ) : ViewModel() {
        private val bookId: Long? = savedStateHandle.get<Long>(ARG_BOOK_ID)

        private val _uiState = MutableStateFlow<EpubReaderUiState>(EpubReaderUiState.Loading)
        val uiState: StateFlow<EpubReaderUiState> = _uiState.asStateFlow()

        /** Where the reader is now: the chapter showing and how far down it. It survives a rotation. */
        var position: ReaderPosition.Epub = ReaderPosition.Epub(0, 0)
            private set

        private val endEvents = Channel<Unit>(Channel.CONFLATED)

        /** Emits once, when the reader reaches the end of the book and the book is not already finished. */
        val endReached: Flow<Unit> = endEvents.receiveAsFlow()

        private var endPrompted = false
        private var saveJob: Job? = null
        private var openedBook: EpubBook? = null

        init {
            open()
        }

        private fun open() {
            viewModelScope.launch {
                val id = bookId
                val file = id?.let { readerRepository.epubFile(it) }
                if (id == null || file == null) {
                    _uiState.value = EpubReaderUiState.Failed(ReaderFailure.NOT_DOWNLOADED)
                    return@launch
                }
                // Opening must finish even if the screen is closed meanwhile, or the file would stay open.
                val result = withContext(Dispatchers.IO + NonCancellable) { EpubBook.open(file) }
                when (result) {
                    is EpubOpenResult.Failed -> _uiState.value = EpubReaderUiState.Failed(result.problem.toFailure())
                    is EpubOpenResult.Opened -> {
                        if (isActive) begin(id, result.book) else result.book.close()
                    }
                }
            }
        }

        private suspend fun begin(
            id: Long,
            book: EpubBook,
        ) {
            openedBook = book
            val saved = readerRepository.savedEpubPosition(id)
            val chapter = (saved?.chapter ?: 0).coerceIn(0, book.chapters.lastIndex)
            val permille = if (saved != null && saved.chapter == chapter) saved.permille else 0
            position = ReaderPosition.Epub(chapter, permille)
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
            _uiState.value = EpubReaderUiState.Ready(book, title ?: book.title)
        }

        /** The reader is at [chapter], [permille] thousandths of the way down it. */
        fun onPositionChanged(
            chapter: Int,
            permille: Int,
        ) {
            val ready = _uiState.value as? EpubReaderUiState.Ready ?: return
            val id = bookId ?: return
            if (chapter !in ready.book.chapters.indices) return
            position = ReaderPosition.Epub(chapter, permille.coerceIn(0, ReaderPosition.MAX_PERMILLE))
            if (chapter == ready.book.chapters.lastIndex && permille >= END_PERMILLE) promptToFinishOnce(id)
            saveJob?.cancel()
            saveJob =
                viewModelScope.launch {
                    delay(SAVE_DELAY_MS)
                    readerRepository.savePosition(id, position)
                }
        }

        /** Saves the position now, for when the reader leaves the screen. */
        fun saveNow() {
            if (_uiState.value !is EpubReaderUiState.Ready) return
            val id = bookId ?: return
            saveJob?.cancel()
            val toSave = position
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

        private fun EpubProblem.toFailure(): ReaderFailure =
            when (this) {
                EpubProblem.NOT_AN_EPUB -> ReaderFailure.NOT_AN_EPUB
                EpubProblem.DAMAGED -> ReaderFailure.DAMAGED
                EpubProblem.EMPTY -> ReaderFailure.EMPTY
            }
    }
