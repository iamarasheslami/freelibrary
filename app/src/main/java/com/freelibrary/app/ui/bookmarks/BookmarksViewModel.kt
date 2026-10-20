package com.freelibrary.app.ui.bookmarks

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.data.repository.SavedBookmark
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A bookmark as the list shows it. [textPercent] is how far through a plain text book it is, when that is known. */
data class BookmarkItem(
    val bookmark: SavedBookmark,
    val textPercent: Int?,
)

/**
 * The bookmarks of one book (the book in the arguments), in the order of the book. A bookmark opens
 * only when the file of the format it was made in is still downloaded.
 */
@HiltViewModel
class BookmarksViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val bookmarkRepository: BookmarkRepository,
        private val readerRepository: ReaderRepository,
        private val detailsRepository: BookDetailsRepository,
    ) : ViewModel() {
        private val bookId: Long? = savedStateHandle.get<Long>(ARG_BOOK_ID)

        private val textLength = MutableStateFlow<Long?>(null)
        private val _bookTitle = MutableStateFlow<String?>(null)

        /** The catalog title of the book, once it is known. */
        val bookTitle: StateFlow<String?> = _bookTitle.asStateFlow()

        /** The bookmarks, or null until they are loaded. */
        val items: StateFlow<List<BookmarkItem>?> =
            (bookId?.let { bookmarkRepository.observe(it) } ?: emptyFlow())
                .catch { emit(emptyList()) }
                .combine(textLength) { saved, length -> saved.map { BookmarkItem(it, percentOf(it.position, length)) } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        init {
            viewModelScope.launch {
                val id = bookId ?: return@launch
                try {
                    textLength.value = readerRepository.textFile(id)?.length()?.takeIf { it > 0 }
                    _bookTitle.value = detailsRepository.load(id)?.title
                } catch (e: SQLiteException) {
                    // The list works without the title and the percentages.
                }
            }
        }

        /** True when the book can be opened at [position]: the file of that kind of book is still downloaded. */
        suspend fun canOpen(position: ReaderPosition): Boolean {
            val id = bookId ?: return false
            return try {
                val file =
                    when (position) {
                        is ReaderPosition.Epub -> readerRepository.epubFile(id)
                        is ReaderPosition.Pdf -> readerRepository.pdfFile(id)
                        is ReaderPosition.Text -> readerRepository.textFile(id)
                    }
                file != null
            } catch (e: SQLiteException) {
                false
            }
        }

        fun delete(bookmark: SavedBookmark) {
            viewModelScope.launch { bookmarkRepository.delete(bookmark.id) }
        }

        /** Puts a deleted bookmark back (the Undo of a delete). */
        fun restore(bookmark: SavedBookmark) {
            viewModelScope.launch { bookmarkRepository.restore(bookmark) }
        }

        private fun percentOf(
            position: ReaderPosition,
            length: Long?,
        ): Int? {
            if (position !is ReaderPosition.Text || length == null || length <= 0) return null
            return (position.offset * 100L / length).toInt().coerceIn(0, 100)
        }
    }
