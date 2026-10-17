package com.freelibrary.app.domain.model

import androidx.annotation.StringRes
import com.freelibrary.app.R

/** Where a slider's "View more" leads. */
sealed interface ViewMoreTarget {
    data object ContinueReading : ViewMoreTarget

    data object WantToRead : ViewMoreTarget

    data object RecentlyAdded : ViewMoreTarget

    data object Finished : ViewMoreTarget

    data class CatalogShelf(
        val bookshelfName: String,
    ) : ViewMoreTarget
}

/**
 * One slider of the home screen. [id] never changes for a slider, so the list can tell which one
 * is which when its books change. [hasMore] says whether to offer "View more".
 */
data class HomeSection(
    val id: String,
    @StringRes val titleRes: Int,
    val books: List<BookCard>,
    val hasMore: Boolean,
    val viewMore: ViewMoreTarget,
)

/** Builds the home screen's list of sliders in their agreed order. */
object HomeSections {
    /**
     * Continue Reading, Want to Read and Recently Added come first, then the curated [shelves] in
     * their own order. A slider with no books is left out, so a reader with nothing saved yet sees
     * no empty rows. Recently Added has no total, so it offers "View more" whenever it is full.
     */
    fun build(
        continueReading: BookSlider,
        wantToRead: BookSlider,
        recentlyAdded: List<BookCard>,
        shelves: List<Pair<HomeShelf, BookSlider>>,
    ): List<HomeSection> =
        listOfNotNull(
            section(
                "continue-reading",
                R.string.home_continue_reading,
                continueReading.books,
                continueReading.hasMore,
                ViewMoreTarget.ContinueReading,
            ),
            section(
                "want-to-read",
                R.string.home_want_to_read,
                wantToRead.books,
                wantToRead.hasMore,
                ViewMoreTarget.WantToRead,
            ),
            section(
                "recently-added",
                R.string.home_recently_added,
                recentlyAdded,
                recentlyAdded.size >= CatalogDefaults.BOOKS_PER_SLIDER,
                ViewMoreTarget.RecentlyAdded,
            ),
        ) +
            shelves.mapNotNull { (shelf, slider) ->
                section(
                    shelf.id,
                    shelf.titleRes,
                    slider.books,
                    slider.hasMore,
                    ViewMoreTarget.CatalogShelf(shelf.bookshelfName),
                )
            }

    private fun section(
        id: String,
        titleRes: Int,
        books: List<BookCard>,
        hasMore: Boolean,
        viewMore: ViewMoreTarget,
    ): HomeSection? = if (books.isEmpty()) null else HomeSection(id, titleRes, books, hasMore, viewMore)
}
