package com.freelibrary.app.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
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

/** Runs a block the first time it is asked to and never again, however many threads ask. */
internal class OnceOnly {
    private val done = AtomicBoolean(false)

    fun runFirstTime(block: () -> Unit) {
        if (done.compareAndSet(false, true)) block()
    }
}

@Singleton
class CatalogSyncRunner
    @Inject
    constructor(
        private val syncRepository: CatalogSyncRepository,
        private val exploreRepository: ExploreRepository,
        private val searchRepository: SearchRepository,
    ) {
        // An unexpected error in the background is logged, never allowed to crash the app on start.
        private val scope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.IO +
                    CoroutineExceptionHandler { _, error -> Log.e(TAG, "The background catalog sync stopped unexpectedly", error) },
            )
        private val startGuard = OnceOnly()

        suspend fun run(): SyncResult? =
            runCatalogSync(
                sync = { syncRepository.sync() },
                onCatalogChanged = {
                    exploreRepository.invalidate()
                    searchRepository.invalidateVocabularies()
                },
            )

        /**
         * Starts a sync in the background the first time it is called in this process and does
         * nothing after that, so rotating the phone or recreating the screen never starts another.
         */
        fun startOnce() = startGuard.runFirstTime { scope.launch { run() } }
    }
