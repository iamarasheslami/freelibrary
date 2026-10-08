package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ExploreCollections
import com.freelibrary.app.domain.model.ExploreSectionKind
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
    fun `the bundled catalog gives the curated collections first, then genres, then other shelves`() =
        runTest {
            val database =
                Room.databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                    "catalog.sqlite",
                ).createFromAsset("catalog.sqlite").build()

            val sections = ExploreRepository(database.bookListDao()).sections()
            val kinds = sections.map { it.kind }

            assertEquals(ExploreCollections.all.map { it.bookshelfName }, sections.take(11).map { it.bookshelfName })
            assertEquals(11, kinds.count { it == ExploreSectionKind.COLLECTION })
            assertTrue(
                "only ${kinds.count { it == ExploreSectionKind.GENRE }} genres",
                kinds.count { it == ExploreSectionKind.GENRE } >= 60,
            )
            assertTrue("only ${sections.size} sections", sections.size >= 200)
            assertEquals(kinds.sortedBy { it.ordinal }, kinds)
            assertTrue(sections.all { it.bookCount >= CatalogDefaults.MIN_SHELF_BOOKS })

            database.close()
        }
}
