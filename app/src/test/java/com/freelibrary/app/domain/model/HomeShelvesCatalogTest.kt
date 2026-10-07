package com.freelibrary.app.domain.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Checks the home shelves against the real bundled catalog (fetched by fetchCatalogDatabase):
 * a misspelled bookshelf name would otherwise silently produce an empty slider.
 */
@RunWith(RobolectricTestRunner::class)
class HomeShelvesCatalogTest {
    @Test
    fun `every home shelf exists in the bundled catalog and fills a slider with English books`() =
        runTest {
            val database =
                Room.databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                    "catalog.sqlite",
                ).createFromAsset("catalog.sqlite").build()

            HomeShelves.all.forEach { shelf ->
                val count = database.bookListDao().observeShelfBookCount(shelf.bookshelfName, "en").first()
                assertTrue(
                    "'${shelf.bookshelfName}' has only $count English books in the bundled catalog",
                    count >= CatalogDefaults.BOOKS_PER_SLIDER,
                )

                val books =
                    database.bookListDao()
                        .observeBooksOnShelf(shelf.bookshelfName, "en", CatalogDefaults.BOOKS_PER_SLIDER)
                        .first()
                assertEquals("'${shelf.bookshelfName}' slider is not full", CatalogDefaults.BOOKS_PER_SLIDER, books.size)
            }

            database.close()
        }
}
