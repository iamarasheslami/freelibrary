package com.freelibrary.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ExploreRow
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.domain.model.ExploreSections
import com.freelibrary.app.domain.model.ShelfBookCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val SECTIONS_PER_PAGE = 10
private const val LOAD_MORE_WHEN_SECTIONS_LEFT = 5

/**
 * The Explore page: every catalog shelf as a slider, curated collections first. The ordered
 * section list is built once from the shelf sizes of the catalog and kept in memory until
 * [invalidate] (call it after a catalog sync); the page then loads it ten sections at a time.
 */
@Singleton
class ExploreRepository
    @Inject
    constructor(
        private val bookListDao: BookListDao,
    ) {
        private val lock = Mutex()
        private var cached: Pair<BookFilter, List<ExploreSection>>? = null

        /** All Explore sections for [filter] in display order. Only the latest filter's list is kept. */
        suspend fun sections(filter: BookFilter = BookFilter()): List<ExploreSection> =
            lock.withLock {
                cached?.takeIf { it.first == filter }?.second ?: loadSections(filter).also { cached = filter to it }
            }

        /** The Explore sections alone, ten at first and ten more each time the reader nears the end. */
        fun sectionPages(filter: BookFilter = BookFilter()): Flow<PagingData<ExploreSection>> = pages(filter) { it }

        /**
         * The Explore page: the same pages, each section with its first books from [loadSlider]. The
         * shelves of one page are read at the same time, and the next page starts while a few sections
         * are still ahead of the reader, so the end of the list is rarely reached before it is ready.
         */
        fun rowPages(
            filter: BookFilter = BookFilter(),
            loadSlider: suspend (ExploreSection) -> BookSlider,
        ): Flow<PagingData<ExploreRow>> = pages(filter) { page -> loadInParallel(page) { ExploreRow(it, loadSlider(it)) } }

        private fun <T : Any> pages(
            filter: BookFilter,
            transform: suspend (List<ExploreSection>) -> List<T>,
        ): Flow<PagingData<T>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = SECTIONS_PER_PAGE,
                        initialLoadSize = SECTIONS_PER_PAGE,
                        prefetchDistance = LOAD_MORE_WHEN_SECTIONS_LEFT,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = { ExploreSectionPagingSource({ sections(filter) }, transform) },
            ).flow

        /** Forget the cached section list, so the shelf sizes are counted again on the next load. */
        suspend fun invalidate() {
            lock.withLock { cached = null }
        }

        /** Without a filter a shelf needs [CatalogDefaults.MIN_SHELF_BOOKS] books; with one, any shelf that still has a book is listed. */
        private suspend fun loadSections(filter: BookFilter): List<ExploreSection> {
            val minBooks = if (filter.isActive) 1 else CatalogDefaults.MIN_SHELF_BOOKS
            val counts =
                bookListDao
                    .shelfBookCounts(
                        filter.browseLanguage,
                        minBooks,
                        filter.formats.toList(),
                        filter.formats.isEmpty(),
                        filter.genres.toList(),
                        filter.genres.isEmpty(),
                    ).map { ShelfBookCount(it.name, it.bookCount) }
            return ExploreSections.build(counts)
        }
    }
