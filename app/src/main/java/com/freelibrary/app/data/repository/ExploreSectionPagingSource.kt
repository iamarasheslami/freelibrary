package com.freelibrary.app.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.freelibrary.app.domain.model.ExploreSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** How many shelf queries one page of Explore runs at the same time. */
internal const val MAX_PARALLEL_SHELF_QUERIES = 4

/**
 * Serves the Explore sections, which are already in memory, one page at a time. Each page is turned
 * into what the screen shows by [transform]. A failure while doing so is returned as a load error,
 * not thrown, so one bad query cannot take the screen down.
 */
internal class ExploreSectionPagingSource<T : Any>(
    private val loadSections: suspend () -> List<ExploreSection>,
    private val transform: suspend (List<ExploreSection>) -> List<T>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        try {
            val sections = loadSections()
            val start = (params.key ?: 0).coerceIn(0, sections.size)
            val end = minOf(start + params.loadSize, sections.size)
            LoadResult.Page(
                data = transform(sections.subList(start, end)),
                prevKey = if (start > 0) maxOf(0, start - params.loadSize) else null,
                nextKey = if (end < sections.size) end else null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor -> maxOf(0, anchor - state.config.initialLoadSize / 2) }
}

/** Runs [load] for every section, at most [limit] at a time, and returns the results in the order of [sections]. */
internal suspend fun <T> loadInParallel(
    sections: List<ExploreSection>,
    limit: Int = MAX_PARALLEL_SHELF_QUERIES,
    load: suspend (ExploreSection) -> T,
): List<T> =
    coroutineScope {
        val gate = Semaphore(limit)
        sections.map { section -> async { gate.withPermit { load(section) } } }.awaitAll()
    }
