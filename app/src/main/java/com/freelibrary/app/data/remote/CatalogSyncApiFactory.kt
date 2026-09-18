package com.freelibrary.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val json = Json { ignoreUnknownKeys = true }

/**
 * Creates a [CatalogSyncApi] instance. ignoreUnknownKeys is deliberately
 * enabled: this app must not crash if the catalog-data repository adds new
 * fields in the future that an older installed app version doesn't know
 * about yet - forward compatibility matters since app updates and data
 * updates are on independent schedules by design.
 *
 * [baseUrl] defaults to the real catalog-data repository but can be
 * overridden in tests to point at a local MockWebServer instance.
 */
fun createCatalogSyncApi(baseUrl: String = CatalogSyncApi.BASE_URL): CatalogSyncApi {
    val retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    return retrofit.create(CatalogSyncApi::class.java)
}
