package com.freelibrary.app.data.remote

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.Manifest
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Fetches the sync manifest and individual book data from the
 * freelibrary-catalog-data repository, served statically via
 * raw.githubusercontent.com. Read-only by design - this app never writes
 * back to the catalog source, only reads updates from it.
 */
interface CatalogSyncApi {
    @GET("manifest.json")
    suspend fun getManifest(): Manifest

    @GET("books/{externalId}.json")
    suspend fun getBook(
        @Path("externalId") externalId: String,
    ): BookExport

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/iamarasheslami/freelibrary-catalog-data/main/"
    }
}
