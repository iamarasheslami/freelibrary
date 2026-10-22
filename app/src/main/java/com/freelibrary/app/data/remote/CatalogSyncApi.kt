package com.freelibrary.app.data.remote

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.Manifest
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Fetches the sync manifest and individual book data from the
 * freelibrary-catalog-data repository, served statically via
 * raw.githubusercontent.com. Read-only by design - this app never writes
 * back to the catalog source, only reads updates from it.
 */
interface CatalogSyncApi {
    /** The manifest at [path], relative to [BASE_URL] (for example "manifest.json" or "sources/oapen/manifest.json"). */
    @GET
    suspend fun getManifest(
        @Url path: String,
    ): Manifest

    /** The book file at [path], relative to [BASE_URL] (for example "books/1342.json"). */
    @GET
    suspend fun getBook(
        @Url path: String,
    ): BookExport

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/iamarasheslami/freelibrary-catalog-data/main/"
    }
}
