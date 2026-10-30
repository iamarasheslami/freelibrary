package com.freelibrary.app.ui.reader

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.data.reader.TextBook
import com.freelibrary.app.data.reader.TextOpenResult
import com.freelibrary.app.data.reader.TextProblem
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.data.repository.SettingsRepository
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

/** Why the reader could not open the text. */
enum class TextFailure {
    NOT_DOWNLOADED,
    UNREADABLE,
    EMPTY,
    TOO_LARGE,
}

sealed interface TextReaderUiState {
    data object Loading : TextReaderUiState

    data class Failed(
        val reason: TextFailure,
    ) : TextReaderUiState

    /** The text is open. [title] is the catalog title. */
    data class Ready(
        val book: TextBook,
        val title: String?,
    ) : TextReaderUiState
}

/** Where the reader is: the part of the book showing and how far through it, in thousandths. */
data class TextPlace(
    val part: Int,
    val permille: Int,
)

private const val SAVE_DELAY_MS = 1_500L
private const val TEXT_STEP_PERCENT = 10

/** How far through the last part counts as having reached the end of the book, in thousandths. */
private const val END_PERMILLE = 980

/**
 * Opens a downloaded plain text book for the reader screen and keeps where the reader is. Opening a
 * book follows the shelf rules (it becomes Currently Reading unless it is already finished). The
 * place is saved a moment after the reader stops moving, and at once when [saveNow] is called.
 * Reaching the end of the last part asks once whether to mark the book finished. Page or scroll
 * view and the text size are settings the readers share.
 */
@HiltViewModel
class TextReaderViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val readerRepository: ReaderRepository,
        private val readingStateRepository: ReadingStateRepository,
        private val detailsRepository: BookDetailsRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val bookId: Long? = savedStateHandle.get<Long>(ARG_BOOK_ID)

        /** The place the reader was asked to open at (a bookmark), if any; it wins over the saved place. */
        private val startPosition: ReaderPosition? = savedStateHandle.startPosition()

        private val _uiState = MutableStateFlow<TextReaderUiState>(TextReaderUiState.Loading)
        val uiState: StateFlow<TextReaderUiState> = _uiState.asStateFlow()

        private val _paged = MutableStateFlow(true)

        /** True to show the book page by page, false as one long scroll per part. Settled before the book is ready. */
        val paged: StateFlow<Boolean> = _paged.asStateFlow()

        private val _textPercent = MutableStateFlow(ReaderStyle.DEFAULT_TEXT_PERCENT)

        /** The text size in percent of the normal size, settled before the book is ready. */
        val textPercent: StateFlow<Int> = _textPercent.asStateFlow()

        /** Where the reader is now. It survives a rotation. */
        var position: TextPlace = TextPlace(0, 0)
            private set

        private val endEvents = Channel<Unit>(Channel.CONFLATED)

        /** Emits once, when the reader reaches the end of the book and the book is not already finished. */
        val endReached: Flow<Unit> = endEvents.receiveAsFlow()

        private var endPrompted = false
        private var saveJob: Job? = null

        init {
            open()
        }

        private fun open() {
            viewModelScope.launch {
                val id = bookId
                val file = id?.let { readerRepository.textFile(it) }
                if (id == null || file == null) {
                    _uiState.value = TextReaderUiState.Failed(TextFailure.NOT_DOWNLOADED)
                    return@launch
                }
                val result = withContext(Dispatchers.IO + NonCancellable) { TextBook.open(file) }
                when (result) {
                    is TextOpenResult.Failed -> _uiState.value = TextReaderUiState.Failed(result.problem.toFailure())
                    is TextOpenResult.Opened -> if (isActive) begin(id, result.book)
                }
            }
        }

        private suspend fun begin(
            id: Long,
            book: TextBook,
        ) {
            _paged.value = settingsRepository.readerPaged(id).first()
            _textPercent.value = settingsRepository.readerTextPercent.first()
            val saved = (startPosition as? ReaderPosition.Text) ?: readerRepository.savedTextPosition(id)
            val (part, permille) = saved?.let { book.locate(it.offset) } ?: (0 to 0)
            position = TextPlace(part, permille)
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
            _uiState.value = TextReaderUiState.Ready(book, title)
        }

        /** The reader chose page view ([paged] true) or scroll view for this book; the choice is kept for next time. */
        fun setPaged(paged: Boolean) {
            _paged.value = paged
            val id = bookId ?: return
            viewModelScope.launch { settingsRepository.setReaderPaged(id, paged) }
        }

        /** The reader asked for larger ([larger] true) or smaller text; the size stays within its limits and is kept. */
        fun changeTextSize(larger: Boolean) = setTextSize(_textPercent.value + if (larger) TEXT_STEP_PERCENT else -TEXT_STEP_PERCENT)

        /** The reader picked [percent] on the text size bar; the size stays within its limits and is kept. */
        fun setTextSize(percent: Int) {
            val size = percent.coerceIn(ReaderStyle.MIN_TEXT_PERCENT, ReaderStyle.MAX_TEXT_PERCENT)
            if (size == _textPercent.value) return
            _textPercent.value = size
            viewModelScope.launch { settingsRepository.setReaderTextPercent(size) }
        }

        /** The reader is at [part], [permille] thousandths of the way through it. */
        fun onPositionChanged(
            part: Int,
            permille: Int,
        ) {
            val ready = _uiState.value as? TextReaderUiState.Ready ?: return
            val id = bookId ?: return
            if (part !in ready.book.parts.indices) return
            position = TextPlace(part, permille.coerceIn(0, ReaderPosition.MAX_PERMILLE))
            if (part == ready.book.parts.lastIndex && permille >= END_PERMILLE) promptToFinishOnce(id)
            saveJob?.cancel()
            val place = ReaderPosition.Text(ready.book.offsetOf(position.part, position.permille))
            saveJob =
                viewModelScope.launch {
                    delay(SAVE_DELAY_MS)
                    readerRepository.savePosition(id, place)
                }
        }

        /** Saves the place now, for when the reader leaves the screen. */
        fun saveNow() {
            val ready = _uiState.value as? TextReaderUiState.Ready ?: return
            val id = bookId ?: return
            saveJob?.cancel()
            val place = ReaderPosition.Text(ready.book.offsetOf(position.part, position.permille))
            viewModelScope.launch { withContext(NonCancellable) { readerRepository.savePosition(id, place) } }
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

        private fun TextProblem.toFailure(): TextFailure =
            when (this) {
                TextProblem.UNREADABLE -> TextFailure.UNREADABLE
                TextProblem.EMPTY -> TextFailure.EMPTY
                TextProblem.TOO_LARGE -> TextFailure.TOO_LARGE
            }
    }
