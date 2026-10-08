package com.freelibrary.app.domain.model

import androidx.annotation.StringRes
import com.freelibrary.app.R

/** How many books a catalog shelf holds in the catalog language. */
data class ShelfBookCount(
    val name: String,
    val bookCount: Int,
)

enum class ExploreSectionKind {
    /** A hand-picked collection, shown first. */
    COLLECTION,

    /** One of the catalog's genre shelves ("Category: ..."). */
    GENRE,

    /** Any other catalog shelf: a topic, a place, a series. */
    SHELF,
}

/**
 * One slider of the Explore page. [bookshelfName] is the catalog shelf behind it. A curated
 * collection has a [titleRes] for its heading; genres and other shelves are titled by [title],
 * which comes from the catalog. [id] is unique across the page.
 */
data class ExploreSection(
    val id: String,
    val bookshelfName: String,
    val kind: ExploreSectionKind,
    val title: String,
    @StringRes val titleRes: Int?,
    val bookCount: Int,
)

/** The hand-picked collections that open the Explore page, in display order. */
object ExploreCollections {
    val all: List<HomeShelf> =
        listOf(
            HomeShelf("best-books-ever", R.string.shelf_best_books_ever, "Best Books Ever Listings"),
            HomeShelf("harvard-classics", R.string.shelf_harvard_classics, "Harvard Classics"),
            HomeShelf("nobel-literature", R.string.shelf_nobel_literature, "Nobel Prizes in Literature"),
            HomeShelf("american-bestsellers", R.string.shelf_american_bestsellers, "Bestsellers, American, 1895-1923"),
            HomeShelf("banned-books", R.string.shelf_banned_books, "Banned Books from Anne Haight's list"),
            HomeShelf("childrens-book-series", R.string.shelf_childrens_book_series, "Children's Book Series"),
            HomeShelf("childrens-literature", R.string.shelf_childrens_literature, "Children's Literature"),
            HomeShelf("childrens-fiction", R.string.shelf_childrens_fiction, "Children's Fiction"),
            HomeShelf("world-war-i", R.string.shelf_world_war_i, "World War I"),
            HomeShelf("us-civil-war", R.string.shelf_us_civil_war, "US Civil War"),
            HomeShelf("christmas", R.string.shelf_christmas, "Christmas"),
        )
}

/** Builds the ordered list of Explore sections from the shelf sizes of the catalog. */
object ExploreSections {
    private const val GENRE_PREFIX = "Category: "

    /**
     * Collections first, in curated order and only those present in [counts]; then the genre
     * shelves; then every other shelf that is not one of the collections. Genres and other
     * shelves are ordered largest first, ties by name. A book may appear in several sections.
     */
    fun build(
        counts: List<ShelfBookCount>,
        collections: List<HomeShelf> = ExploreCollections.all,
    ): List<ExploreSection> {
        val countByName = counts.associate { it.name to it.bookCount }
        val collectionNames = collections.map { it.bookshelfName }.toSet()
        val largestFirst = compareByDescending<ShelfBookCount> { it.bookCount }.thenBy { it.name }

        val collectionSections =
            collections.mapNotNull { shelf ->
                countByName[shelf.bookshelfName]?.let { count ->
                    ExploreSection(
                        id = "collection:${shelf.id}",
                        bookshelfName = shelf.bookshelfName,
                        kind = ExploreSectionKind.COLLECTION,
                        title = shelf.bookshelfName,
                        titleRes = shelf.titleRes,
                        bookCount = count,
                    )
                }
            }
        val genreSections =
            counts.filter { it.name.startsWith(GENRE_PREFIX) }
                .sortedWith(largestFirst)
                .map {
                    ExploreSection(
                        id = "genre:${it.name}",
                        bookshelfName = it.name,
                        kind = ExploreSectionKind.GENRE,
                        title = it.name.removePrefix(GENRE_PREFIX),
                        titleRes = null,
                        bookCount = it.bookCount,
                    )
                }
        val shelfSections =
            counts.filter { !it.name.startsWith(GENRE_PREFIX) && it.name !in collectionNames }
                .sortedWith(largestFirst)
                .map {
                    ExploreSection(
                        id = "shelf:${it.name}",
                        bookshelfName = it.name,
                        kind = ExploreSectionKind.SHELF,
                        title = it.name,
                        titleRes = null,
                        bookCount = it.bookCount,
                    )
                }
        return collectionSections + genreSections + shelfSections
    }
}
