package com.freelibrary.app.ui.booklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.BookListRepository
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.ViewMoreTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Navigation argument names of the list page (they must match nav_graph.xml). */
const val ARG_KIND = "kind"
const val ARG_BOOKSHELF_NAME = "bookshelfName"

/** Set by Explore: the shelf list follows the reader's filter instead of showing every book of the shelf. */
const val ARG_FILTERED = "filtered"

/**
 * The full list behind one "View more" button, paged. The list is chosen from the page's
 * arguments; arguments that name nothing known give an empty list instead of a crash.
 */
@HiltViewModel
class BookListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: BookListRepository,
        filterHolder: BookFilterHolder,
    ) : ViewModel() {
        val pages: Flow<PagingData<BookCard>> =
            when (
                val target =
                    viewMoreTargetFrom(
                        savedStateHandle.get<String>(ARG_KIND),
                        savedStateHandle.get<String>(ARG_BOOKSHELF_NAME),
                    )
            ) {
                ViewMoreTarget.ContinueReading -> repository.readerShelfPages(ShelfState.READING)
                ViewMoreTarget.WantToRead -> repository.readerShelfPages(ShelfState.WANT_TO_READ)
                ViewMoreTarget.Finished -> repository.readerShelfPages(ShelfState.FINISHED)
                ViewMoreTarget.RecentlyAdded -> repository.recentlyAddedPages()
                is ViewMoreTarget.CatalogShelf ->
                    repository.shelfPages(
                        target.bookshelfName,
                        if (savedStateHandle.get<Boolean>(ARG_FILTERED) == true) filterHolder.filter.value else BookFilter(),
                    )
                null -> Pager(PagingConfig(pageSize = 1)) { NoBooksPagingSource }.flow
            }.cachedIn(viewModelScope)
    }

/** A source with no books, which finishes loading at once, like any other list that turns out empty. */
private object NoBooksPagingSource : PagingSource<Int, BookCard>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BookCard> =
        LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

    override fun getRefreshKey(state: PagingState<Int, BookCard>): Int? = null
}
