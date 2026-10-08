package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreSectionsTest {
    private val collections =
        listOf(
            HomeShelf("a", titleRes = 1, bookshelfName = "Alpha Collection"),
            HomeShelf("b", titleRes = 2, bookshelfName = "Beta Collection"),
            HomeShelf("c", titleRes = 3, bookshelfName = "Gamma Collection"),
        )

    private fun count(
        name: String,
        books: Int,
    ) = ShelfBookCount(name, books)

    @Test
    fun `collections come first in curated order and only those the catalog has`() {
        val sections =
            ExploreSections.build(
                counts = listOf(count("Gamma Collection", 20), count("Alpha Collection", 50), count("Category: Poetry", 900)),
                collections = collections,
            )

        assertEquals(
            listOf("collection:a", "collection:c", "genre:Category: Poetry"),
            sections.map { it.id },
        )
        assertTrue(sections.take(2).all { it.kind == ExploreSectionKind.COLLECTION })
    }

    @Test
    fun `a collection keeps its heading resource and the catalog count`() {
        val section =
            ExploreSections.build(listOf(count("Beta Collection", 42)), collections).single()

        assertEquals(2, section.titleRes)
        assertEquals(42, section.bookCount)
        assertEquals("Beta Collection", section.bookshelfName)
    }

    @Test
    fun `genres follow the collections, largest first, with the Category prefix removed`() {
        val sections =
            ExploreSections.build(
                counts = listOf(count("Category: Poetry", 300), count("Category: Novels", 900), count("Category: Drama", 500)),
                collections = collections,
            )

        assertEquals(listOf("Novels", "Drama", "Poetry"), sections.map { it.title })
        assertTrue(sections.all { it.kind == ExploreSectionKind.GENRE && it.titleRes == null })
    }

    @Test
    fun `sections of the same size are ordered by name`() {
        val sections =
            ExploreSections.build(
                counts = listOf(count("Category: Zoology", 100), count("Category: Art", 100), count("Category: Music", 100)),
                collections = collections,
            )

        assertEquals(listOf("Art", "Music", "Zoology"), sections.map { it.title })
    }

    @Test
    fun `other shelves come last, largest first, without the collections or the genres`() {
        val sections =
            ExploreSections.build(
                counts =
                    listOf(
                        count("Punch", 400),
                        count("Alpha Collection", 60),
                        count("Category: Poetry", 900),
                        count("Christmas", 130),
                    ),
                collections = collections,
            )

        assertEquals(
            listOf("collection:a", "genre:Category: Poetry", "shelf:Punch", "shelf:Christmas"),
            sections.map { it.id },
        )
        assertEquals(1, sections.count { it.bookshelfName == "Alpha Collection" })
    }

    @Test
    fun `every section has its own id`() {
        val sections =
            ExploreSections.build(
                counts = (1..30).map { count("Shelf $it", it * 10) } + (1..5).map { count("Category: Genre $it", it * 10) },
                collections = collections,
            )

        assertEquals(sections.size, sections.map { it.id }.toSet().size)
    }

    @Test
    fun `an empty catalog gives no sections`() {
        assertTrue(ExploreSections.build(emptyList(), collections).isEmpty())
    }

    @Test
    fun `the curated collections are the eleven agreed ones with unique names and ids`() {
        assertEquals(11, ExploreCollections.all.size)
        assertEquals(11, ExploreCollections.all.map { it.id }.toSet().size)
        assertEquals(11, ExploreCollections.all.map { it.bookshelfName }.toSet().size)
        assertTrue(ExploreCollections.all.all { it.titleRes != 0 })
    }
}
