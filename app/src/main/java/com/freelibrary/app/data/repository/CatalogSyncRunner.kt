package com.freelibrary.app.data.repository

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CatalogSyncRunner"

/**
 * Runs one catalog sync and tells the caches that depend on the catalog when it changed anything,
 * so the Explore sections and the spelling corrections see the new books.
 *
 * Returns null when the sync could not run at all, which is normal without a connection: the app
 * carries on with the catalog it has and the next start tries again. Books that failed one by one
 * are only logged, because the next sync fetches them again.
 */
internal suspend fun runCatalogSync(
    sync: suspend () -> SyncResult,
    onCatalogChanged: suspend () -> Unit,
): SyncResult? {
    val result =
        try {
            sync()
        } catch (e: CatalogSyncException) {
            Log.w(TAG, "The catalog sync did not run: ${e.message}", e.cause)
            return null
        }
    if (result.failed > 0) {
        Log.w(TAG, "${result.failed} books failed to sync, for example: ${result.failureSamples}")
    }
    if (result.added + result.updated > 0) onCatalogChanged()
    return result
}

@Singleton
class CatalogSyncRunner
    @Inject
    constructor(
        private val syncRepository: CatalogSyncRepository,
        private val exploreRepository: ExploreRepository,
        private val searchRepository: SearchRepository,
    ) {
        suspend fun run(): SyncResult? =
            runCatalogSync(
                sync = { syncRepository.sync() },
                onCatalogChanged = {
                    exploreRepository.invalidate()
                    searchRepository.invalidateVocabularies()
                },
            )
    }
