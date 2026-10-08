package com.freelibrary.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.domain.model.ExploreSections
import com.freelibrary.app.domain.model.ShelfBookCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val SECTIONS_PER_PAGE = 10
private const val LOAD_MORE_WHEN_SECTIONS_LEFT = 2

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
        private var cachedSections: List<ExploreSection>? = null

        /** All Explore sections in display order. */
        suspend fun sections(): List<ExploreSection> = lock.withLock { cachedSections ?: loadSections().also { cachedSections = it } }

        /** The Explore page: ten sections at first, ten more each time the reader nears the end. */
        fun sectionPages(): Flow<PagingData<ExploreSection>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = SECTIONS_PER_PAGE,
                        initialLoadSize = SECTIONS_PER_PAGE,
                        prefetchDistance = LOAD_MORE_WHEN_SECTIONS_LEFT,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = { ExploreSectionPagingSource(::sections) },
            ).flow

        /** Forget the cached section list, so the shelf sizes are counted again on the next load. */
        suspend fun invalidate() {
            lock.withLock { cachedSections = null }
        }

        private suspend fun loadSections(): List<ExploreSection> {
            val counts =
                bookListDao.shelfBookCounts(CatalogDefaults.LANGUAGE, CatalogDefaults.MIN_SHELF_BOOKS)
                    .map { ShelfBookCount(it.name, it.bookCount) }
            return ExploreSections.build(counts)
        }
    }
