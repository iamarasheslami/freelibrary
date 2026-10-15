package com.freelibrary.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.freelibrary.app.data.repository.BookListRepository
import com.freelibrary.app.data.repository.ExploreRepository
import com.freelibrary.app.domain.model.ExploreRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The Explore page: its sections, ten at a time, each with the first books of its shelf. A row's
 * books are read once, when its page of sections loads; the page does not need to follow later
 * catalog changes while it is open.
 */
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        exploreRepository: ExploreRepository,
        bookListRepository: BookListRepository,
    ) : ViewModel() {
        val rows: Flow<PagingData<ExploreRow>> =
            exploreRepository
                .sectionPages()
                .map { data ->
                    data.map { section ->
                        ExploreRow(section, bookListRepository.observeShelfSlider(section.bookshelfName).first())
                    }
                }.cachedIn(viewModelScope)
    }
