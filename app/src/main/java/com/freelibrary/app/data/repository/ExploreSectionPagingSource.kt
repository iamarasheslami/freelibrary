package com.freelibrary.app.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.freelibrary.app.domain.model.ExploreSection

/** Serves the Explore sections, which are already in memory, one page at a time. */
internal class ExploreSectionPagingSource(
    private val loadSections: suspend () -> List<ExploreSection>,
) : PagingSource<Int, ExploreSection>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ExploreSection> {
        val sections = loadSections()
        val start = (params.key ?: 0).coerceIn(0, sections.size)
        val end = minOf(start + params.loadSize, sections.size)
        return LoadResult.Page(
            data = sections.subList(start, end),
            prevKey = if (start > 0) maxOf(0, start - params.loadSize) else null,
            nextKey = if (end < sections.size) end else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ExploreSection>): Int? =
        state.anchorPosition?.let { anchor -> maxOf(0, anchor - state.config.initialLoadSize / 2) }
}
