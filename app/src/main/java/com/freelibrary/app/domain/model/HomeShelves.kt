package com.freelibrary.app.domain.model

import androidx.annotation.StringRes
import com.freelibrary.app.R

/**
 * One curated slider of the home screen. [bookshelfName] is the exact name of the bookshelf in
 * the catalog database; [titleRes] is the heading shown to the reader.
 */
data class HomeShelf(
    val id: String,
    @StringRes val titleRes: Int,
    val bookshelfName: String,
)

/**
 * The home screen's shelf sliders, in display order. Changing the sliders, now or when other
 * catalog sources are added, means editing this list only. A test checks that every
 * bookshelfName exists in the bundled catalog.
 */
object HomeShelves {
    val all: List<HomeShelf> =
        listOf(
            HomeShelf("best-books-ever", R.string.shelf_best_books_ever, "Best Books Ever Listings"),
            HomeShelf("harvard-classics", R.string.shelf_harvard_classics, "Harvard Classics"),
            HomeShelf("nobel-literature", R.string.shelf_nobel_literature, "Nobel Prizes in Literature"),
            HomeShelf("american-bestsellers", R.string.shelf_american_bestsellers, "Bestsellers, American, 1895-1923"),
            HomeShelf("banned-books", R.string.shelf_banned_books, "Banned Books from Anne Haight's list"),
            HomeShelf(
                "science-fiction-fantasy",
                R.string.shelf_science_fiction_fantasy,
                "Category: Science-Fiction & Fantasy",
            ),
            HomeShelf("adventure", R.string.shelf_adventure, "Category: Adventure"),
            HomeShelf("mythology-folklore", R.string.shelf_mythology_folklore, "Category: Mythology, Legends & Folklore"),
            HomeShelf("poetry", R.string.shelf_poetry, "Category: Poetry"),
            HomeShelf("philosophy-ethics", R.string.shelf_philosophy_ethics, "Category: Philosophy & Ethics"),
        )
}
