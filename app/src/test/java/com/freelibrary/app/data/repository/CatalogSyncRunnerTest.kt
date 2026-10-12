package com.freelibrary.app.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogSyncRunnerTest {
    private var changedCalls = 0

    private fun result(
        added: Int = 0,
        updated: Int = 0,
        failed: Int = 0,
    ) = SyncResult(added = added, updated = updated, failed = failed, failureSamples = emptyList())

    @Test
    fun `new books tell the caches the catalog changed`() =
        runTest {
            val outcome = runCatalogSync(sync = { result(added = 3) }, onCatalogChanged = { changedCalls++ })

            assertEquals(result(added = 3), outcome)
            assertEquals(1, changedCalls)
        }

    @Test
    fun `updated books tell the caches the catalog changed`() =
        runTest {
            runCatalogSync(sync = { result(updated = 1) }, onCatalogChanged = { changedCalls++ })

            assertEquals(1, changedCalls)
        }

    @Test
    fun `a sync that changed nothing leaves the caches alone`() =
        runTest {
            val outcome = runCatalogSync(sync = { result(failed = 2) }, onCatalogChanged = { changedCalls++ })

            assertEquals(result(failed = 2), outcome)
            assertEquals(0, changedCalls)
        }

    @Test
    fun `a sync that could not run gives nothing and leaves the caches alone`() =
        runTest {
            val outcome =
                runCatalogSync(
                    sync = { throw CatalogSyncException("Failed to fetch sync manifest") },
                    onCatalogChanged = { changedCalls++ },
                )

            assertNull(outcome)
            assertEquals(0, changedCalls)
        }
}
