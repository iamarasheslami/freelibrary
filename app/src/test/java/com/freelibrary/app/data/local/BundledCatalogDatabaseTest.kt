package com.freelibrary.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that the real bundled catalog.sqlite (fetched via the
 * fetchCatalogDatabase Gradle task into app/src/main/assets/) is actually
 * accepted by Room at runtime - not just present as a file. This is the
 * genuine correctness proof: a database Room rejects due to a schema or
 * identity-hash mismatch would fail here, not silently pass CI.
 */
@RunWith(RobolectricTestRunner::class)
class BundledCatalogDatabaseTest {
    @Test
    fun `Room accepts the bundled catalog database and can query real data`() =
        runTest {
            val database =
                Room.databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                    "catalog.sqlite",
                ).createFromAsset("catalog.sqlite").build()

            val bookCount = database.bookDao().count()
            assertTrue("Expected a real, non-empty catalog, got $bookCount books", bookCount > 70000)

            val pridePrejudice =
                database.bookDao().getBySourceAndExternalId(
                    database.sourceDao().getAll().first().id,
                    "1342",
                )
            assertEquals("Pride and Prejudice", pridePrejudice?.title)

            database.close()
        }
}
