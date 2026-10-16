package com.freelibrary.app.data.repository

/** Runs one catalog sync when the reader asks for it. Null means the sync could not run at all. */
fun interface CatalogSyncer {
    suspend fun syncNow(): SyncResult?
}
