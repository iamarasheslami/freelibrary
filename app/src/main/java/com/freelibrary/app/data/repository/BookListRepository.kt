package com.freelibrary.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.freelibrary.app.data.local.dao.BookCardRow
import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.domain.model.CatalogDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 30

/**
 * The books behind the home and Explore screens: the sliders (the first books of a list, live)
 * and the full "View more" lists (paged). Catalog lists show [CatalogDefaults.LANGUAGE] books
 * only; the reader's own shelves show every book the reader chose.
 */
@Singleton
class BookListRepository
    @Inject
    constructor(
        private val bookListDao: BookListDao,
    ) {
        /** The slider of any catalog shelf, by its bookshelf name: its first books and its total size. */
        fun observeShelfSlider(
            bookshelfName: String,
            filter: BookFilter = BookFilter(),
        ): Flow<BookSlider> =
            combine(
                bookListDao.observeBooksOnShelf(
                    bookshelfName,
                    filter.browseLanguage,
                    CatalogDefaults.BOOKS_PER_SLIDER,
                    filter.formats.toList(),
                    filter.formats.isEmpty(),
                    filter.genres.toList(),
                    filter.genres.isEmpty(),
                ),
                bookListDao.observeShelfBookCount(
                    bookshelfName,
                    filter.browseLanguage,
                    filter.formats.toList(),
                    filter.formats.isEmpty(),
                    filter.genres.toList(),
                    filter.genres.isEmpty(),
                ),
            ) { rows, total -> BookSlider(rows.map { it.toBookCard() }, total) }

        /** The Recently Added slider. */
        fun observeRecentlyAdded(): Flow<List<BookCard>> =
            bookListDao.observeRecentlyAdded(CatalogDefaults.LANGUAGE, CatalogDefaults.BOOKS_PER_SLIDER)
                .map { rows -> rows.map { it.toBookCard() } }

        /** Every book of a catalog shelf, for its "View more" page. */
        fun shelfPages(
            bookshelfName: String,
            filter: BookFilter = BookFilter(),
        ): Flow<PagingData<BookCard>> =
            pages {
                bookListDao.shelfBooksPagingSource(
                    bookshelfName,
                    filter.browseLanguage,
                    filter.formats.toList(),
                    filter.formats.isEmpty(),
                    filter.genres.toList(),
                    filter.genres.isEmpty(),
                )
            }

        /** Every book, newest first, for the Recently Added "View more" page. */
        fun recentlyAddedPages(): Flow<PagingData<BookCard>> = pages { bookListDao.recentlyAddedPagingSource(CatalogDefaults.LANGUAGE) }

        /** All the reader's books in [state], for a "View more" page. */
        fun readerShelfPages(state: ShelfState): Flow<PagingData<BookCard>> = pages { bookListDao.booksInStatePagingSource(state) }

        private fun pages(sourceFactory: () -> PagingSource<Int, BookCardRow>): Flow<PagingData<BookCard>> =
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = sourceFactory,
            ).flow.map { data -> data.map { it.toBookCard() } }
    }
