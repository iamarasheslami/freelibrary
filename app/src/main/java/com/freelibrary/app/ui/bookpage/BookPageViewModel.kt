package com.freelibrary.app.ui.bookpage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.domain.model.BookDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation argument name of the book page (it must match nav_graph.xml). */
const val ARG_BOOK_ID = "bookId"

private const val NO_BOOK = -1L

/** What a book's page shows: nothing yet, the book with its shelf buttons, or a notice that it is gone. */
sealed interface BookPageUiState {
    data object Loading : BookPageUiState

    data object NotFound : BookPageUiState

    data class Content(
        val details: BookDetails,
        val shelfState: ShelfState?,
        val controls: ShelfControls,
    ) : BookPageUiState
}

@HiltViewModel
class BookPageViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val detailsRepository: BookDetailsRepository,
        private val readingStateRepository: ReadingStateRepository,
    ) : ViewModel() {
        private val bookId: Long = savedStateHandle.get<Long>(ARG_BOOK_ID) ?: NO_BOOK

        /** The book is read once; its shelf state is watched, so the buttons follow every change. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<BookPageUiState> =
            flow { emit(detailsRepository.load(bookId)) }
                .flatMapLatest { details ->
                    if (details == null) {
                        flowOf(BookPageUiState.NotFound)
                    } else {
                        readingStateRepository.observeStateOf(bookId).map { state ->
                            BookPageUiState.Content(details, state, shelfControlsFor(state))
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookPageUiState.Loading)

        fun onShelfAction(action: ShelfAction) {
            viewModelScope.launch {
                when (action) {
                    ShelfAction.SAVE -> readingStateRepository.saveForLater(bookId)
                    ShelfAction.MARK_FINISHED -> readingStateRepository.markFinished(bookId)
                    ShelfAction.REMOVE -> readingStateRepository.removeFromShelf(bookId)
                }
            }
        }
    }
