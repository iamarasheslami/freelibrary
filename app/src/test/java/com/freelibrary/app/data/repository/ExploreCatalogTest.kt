package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ExploreCollections
import com.freelibrary.app.domain.model.ExploreSectionKind
import com.freelibrary.app.domain.model.HomeShelves
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Checks the Explore sections against the real bundled catalog (fetched by fetchCatalogDatabase). */
@RunWith(RobolectricTestRunner::class)
class ExploreCatalogTest {
    @Test
    fun `the bundled catalog opens with shelves Home does not show, then genres, then other shelves`() =
        runTest {
            val database =
                Room.databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                    "catalog.sqlite",
                ).createFromAsset("catalog.sqlite").build()

            val sections = ExploreRepository(database.bookListDao()).sections()
            val kinds = sections.map { it.kind }

            val homeNames = HomeShelves.all.map { it.bookshelfName }
            val opening = sections.take(10).map { it.bookshelfName }
            assertTrue("The first page repeats Home: ${opening.filter { it in homeNames }}", opening.none { it in homeNames })
            assertEquals(homeNames, sections.map { it.bookshelfName }.filter { it in homeNames })
            val collections = sections.filter { it.kind == ExploreSectionKind.COLLECTION }.map { it.bookshelfName }
            assertEquals(ExploreCollections.all.map { it.bookshelfName }.toSet(), collections.toSet())
            assertEquals(11, kinds.count { it == ExploreSectionKind.COLLECTION })
            assertTrue(
                "only ${kinds.count { it == ExploreSectionKind.GENRE }} genres",
                kinds.count { it == ExploreSectionKind.GENRE } >= 60,
            )
            assertTrue("only ${sections.size} sections", sections.size >= 200)
            assertTrue(kinds.indexOfLast { it != ExploreSectionKind.SHELF } < kinds.indexOfFirst { it == ExploreSectionKind.SHELF })
            assertTrue(sections.all { it.bookCount >= CatalogDefaults.MIN_SHELF_BOOKS })

            database.close()
        }
}
