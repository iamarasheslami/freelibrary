package com.freelibrary.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.BookListRepository
import com.freelibrary.app.data.repository.ExploreRepository
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.ExploreRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * The Explore page: its sections, ten at a time, each with the first books of its shelf. A row's
 * books are read once, when its page of sections loads; the page does not need to follow later
 * catalog changes while it is open. When the reader changes the filter, the whole page is built
 * again for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        exploreRepository: ExploreRepository,
        bookListRepository: BookListRepository,
        filterHolder: BookFilterHolder,
    ) : ViewModel() {
        /** The shared filter, so the page can show whether one is on. */
        val filter: StateFlow<BookFilter> = filterHolder.filter

        val rows: Flow<PagingData<ExploreRow>> =
            filterHolder.filter
                .flatMapLatest { filter ->
                    exploreRepository.rowPages(filter) { section ->
                        bookListRepository.observeShelfSlider(section.bookshelfName, filter).first()
                    }
                }.cachedIn(viewModelScope)
    }
