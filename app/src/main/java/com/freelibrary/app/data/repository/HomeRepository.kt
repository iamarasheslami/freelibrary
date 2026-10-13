package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.domain.model.HomeSections
import com.freelibrary.app.domain.model.HomeShelves
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The home screen's list of sliders, kept live: it is rebuilt whenever the reader's shelves, the
 * newest books or any curated shelf changes, and handed on only when the list really differs, so
 * the background sync writing books does not redraw the screen for nothing.
 */
class HomeRepository
    @Inject
    constructor(
        private val bookListRepository: BookListRepository,
        private val readingStateRepository: ReadingStateRepository,
    ) {
        fun observeSections(): Flow<List<HomeSection>> {
            val shelves =
                combine(
                    HomeShelves.all.map { shelf ->
                        bookListRepository.observeShelfSlider(shelf.bookshelfName).map { slider -> shelf to slider }
                    },
                ) { it.toList() }

            return combine(
                readerSlider(ShelfState.READING),
                readerSlider(ShelfState.WANT_TO_READ),
                bookListRepository.observeRecentlyAdded(),
                shelves,
            ) { continueReading, wantToRead, recentlyAdded, curated ->
                HomeSections.build(continueReading, wantToRead, recentlyAdded, curated)
            }.distinctUntilChanged()
        }

        private fun readerSlider(state: ShelfState): Flow<BookSlider> =
            combine(
                readingStateRepository.observeShelf(state, CatalogDefaults.BOOKS_PER_SLIDER),
                readingStateRepository.observeCount(state),
            ) { books, total -> BookSlider(books, total) }
    }
